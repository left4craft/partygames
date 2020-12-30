package me.sisko.partygames.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.minigames.DiggingMinigame;
import me.sisko.partygames.minigames.Minigame;

public class MinigameManager {
    // maps a string, representing the minigame type, to a list
    // of minigames. Each minigame object represents only a single
    // json file.
    private static Map<String, List<Minigame>> minigames;

    public static void load() {
        File dataFolder = new File(Main.getPlugin().getDataFolder().getAbsolutePath() + "/games");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        Main.getPlugin().getLogger().info("Checking minigames directory");


        minigames = new HashMap<String, List<Minigame>>();
        for (File game : dataFolder.listFiles()) {
            Main.getPlugin().getLogger().info("Loading " + game.getName());
            final JSONObject minigameJson = getJson(game);
            final String type = minigameJson.getString("type").toLowerCase();

            // construct the correct type of minigame, based on the type parameter
            Minigame m = null;
            if(type.equals("digging")) {
                m = new DiggingMinigame();
            }

            // if type is valid, attempt to construct minigame and place it in the map accordingly
            if(m != null) {
                if(m.jsonValid(minigameJson)) {
                    m.setup(minigameJson);

                    if(minigames.containsKey(type)) {
                        minigames.get(type).add(m);
                    } else {
                        minigames.put(type, new ArrayList<Minigame>());
                        minigames.get(type).add(m);
                    }
                }
            }
        }
    }

    private static final JSONObject getJson(File f) {
        try {
            final String jsonStr = String.join("", Files.readAllLines(Paths.get(f.getAbsolutePath())));
            return new JSONObject(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
