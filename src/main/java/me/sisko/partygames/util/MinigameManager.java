package me.sisko.partygames.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.minigames.DiggingMinigame;
import me.sisko.partygames.minigames.Minigame;

public class MinigameManager {
    private static List<Minigame> minigames;

    public static void load() {
        File dataFolder = new File(Main.getPlugin().getDataFolder().getAbsolutePath() + "/games");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        for (File game : dataFolder.listFiles()) {
            Main.getPlugin().getLogger().info("Loading " + game.getName());
            final JSONObject minigameJson = getJson(game);
            if(minigameJson.getString("name").toLowerCase().equals("digging")) {
                Minigame m = new DiggingMinigame();
                if(m.jsonValid(minigameJson)) {
                    m.setup(minigameJson);
                    minigames.add(m);
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
