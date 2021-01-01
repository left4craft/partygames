package me.sisko.partygames.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.minigames.DiggingMinigame;
import me.sisko.partygames.minigames.Minigame;
import me.sisko.partygames.minigames.TNTRunMinigame;

public class MinigameManager {
    // maps a string, representing the minigame type, to a list
    // of minigames. Each minigame object represents only a single
    // json file.
    
    private static Map<String, List<Minigame>> minigames;
    private static Minigame currentMinigame = null;
    private static BukkitRunnable timeout = null;


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
            } else if (type.equals("tntrun")) {
                m = new TNTRunMinigame();
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
                } else {
                    Main.getPlugin().getLogger().warning("Invalid JSON for this minigame type! Minigame not being loaded.");
                }
            }
        }
    }

    public static final boolean isValidType(String type) {
        return minigames.containsKey(type);
    }

    public static final String[] getTypes() {
        return minigames.keySet().stream().toArray(String[]::new);
    }

    public static void playGame(String type) {
        // select a random map, given minigame type
        currentMinigame = minigames.get(type).get(new Random().nextInt(minigames.get(type).size()));
        currentMinigame.initialize();
    }

    // called by the minigame when done initializing
    public static void initializationComplete() {
        currentMinigame.prestart(Main.getPlugin().getServer().getOnlinePlayers().stream().collect(Collectors.toList()));
        Bukkit.getPluginManager().registerEvents(currentMinigame, Main.getPlugin());
    }

    // called by the minigame when done initializing
    public static void prestartComplete() {
        Bukkit.broadcastMessage("Game starting in 10 seconds...");

        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game starting in 3 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 7*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game starting in 2 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 8*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game starting in 1 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 9*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game started!");

                timeout = new BukkitRunnable(){
                    @Override
                    public void run() {
                        gameComplete(currentMinigame.timeout());
                    }
                    
                };
                timeout.runTaskLater(Main.getPlugin(), currentMinigame.getTimeoutTime());
                currentMinigame.start();
            }
        }.runTaskLater(Main.getPlugin(), 10*20);
        
    }

    // called by the minigame when it is done running
    public static void gameComplete(final List<Player> winners) {
        timeout.cancel();
        timeout = null;
        currentMinigame.postgame();     
   
        Bukkit.broadcastMessage("Minigame " + currentMinigame.getName() + " complete!");
        Bukkit.broadcastMessage("Map: " + currentMinigame.getMap());
        Bukkit.broadcastMessage("Winners: ");
        for(int i = 0; i < winners.size(); i++) {
            Bukkit.broadcastMessage(winners.get(i).getDisplayName());
        }

        Bukkit.broadcastMessage("Game ending in 10 seconds...");
        
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game ending in 3 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 7*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game ending in 2 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 8*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game ending in 1 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 9*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.broadcastMessage("Game ended!");
                HandlerList.unregisterAll(currentMinigame);
                currentMinigame.cleanup();
                currentMinigame = null;

                for(Player p : Bukkit.getOnlinePlayers()) {
                    FileConfiguration config = Main.getPlugin().getConfig();
                    p.teleport(new Location(Main.getWorld(), config.getDouble("spawn.x"), 
                        config.getDouble("spawn.y"), config.getDouble("spawn.z"), (float) config.getDouble("spawn.yaw"),
                        (float) config.getDouble("spawn.pitch")));
                }

            }
        }.runTaskLater(Main.getPlugin(), 10*20);
    }

    public static final boolean addPlayer(Player p) {
        if(inGame()) {
            currentMinigame.addPlayer(p);
            return true;
        }
        return false;
    }

    public static final boolean removePlayer(Player p) {
        if(inGame()) {
            currentMinigame.removePlayer(p);
            return true;
        }
        return false;
    }

    public static final boolean inGame() {
        return currentMinigame != null;
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
