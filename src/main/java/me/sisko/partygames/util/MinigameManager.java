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
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.minigames.DiggingMinigame;
import me.sisko.partygames.minigames.DropperMinigame;
import me.sisko.partygames.minigames.HoeHoeHoeMinigame;
import me.sisko.partygames.minigames.KOTHMinigame;
import me.sisko.partygames.minigames.LastPlayerStandingMinigame;
import me.sisko.partygames.minigames.Minigame;
import me.sisko.partygames.minigames.OneInTheChamberMinigame;
import me.sisko.partygames.minigames.ParkourMinigame;
import me.sisko.partygames.minigames.QuakeMinigame;
import me.sisko.partygames.minigames.SpleefMinigame;
import me.sisko.partygames.minigames.SumoMinigame;
import me.sisko.partygames.minigames.TNTRunMinigame;
import me.sisko.partygames.minigames.RedLightMinigame;
import me.sisko.partygames.util.ChatSender.ChatSound;

public class MinigameManager {
    // maps a string, representing the minigame type, to a list
    // of minigames. Each minigame object represents only a single
    // json file.
    
    private static Map<String, List<Minigame>> minigames;
    private static Minigame currentMinigame = null;
    private static BukkitRunnable timeout = null;

    private static long endTime = 0;

    public static enum GameState {
        INITIALIZING,
        PREGAME,
        INGAME,
        POSTGAME,
        NOGAME
    };


    private static GameState gameState = GameState.NOGAME;
    private static List<Player> inGame;

    public static void load() {
        File dataFolder = new File(Main.getPlugin().getDataFolder().getAbsolutePath() + "/games");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        Main.getPlugin().getLogger().info("Checking minigames directory");


        minigames = new HashMap<String, List<Minigame>>();
        inGame = new ArrayList<Player>();

        for (File game : dataFolder.listFiles()) {
            Main.getPlugin().getLogger().info("Loading " + game.getName());
            final JSONObject minigameJson = getJson(game);
            final String type = minigameJson.getString("type").toLowerCase();

            // construct the correct type of minigame, based on the type parameter
            Minigame m = null;
            try {
                if(type.equals("digging")) {
                    m = new DiggingMinigame();
                }
                else if (type.equals("tntrun")) {
                    m = new TNTRunMinigame();
                }
                else if (type.equals("dropper")) {
                    m = new DropperMinigame();
                }
                else if (type.equals("spleef")) {
                    m = new SpleefMinigame();
                }
                else if(type.equals("parkour")) {
                    m = new ParkourMinigame();
                } else if(type.equals("sumo")) {
                    m = new SumoMinigame();
                } else if(type.equals("lastplayerstanding")) {
                    m = new LastPlayerStandingMinigame();
                } else if (type.equals("oneinthechamber")) {
                    m = new OneInTheChamberMinigame();
                }
                else if (type.equals("quake")) {
                    m = new QuakeMinigame();
                } 
                else if (type.equals("hoehoehoe")) {
                    m = new HoeHoeHoeMinigame();
                } else if (type.equals("redlightgreenlight")) {
                    m = new RedLightMinigame();
                } else if (type.equals("koth")) {
                    m = new KOTHMinigame();
                }
            } catch (Exception e) {
                Main.getPlugin().getLogger().warning("Error enabling minigame " + type);
                e.printStackTrace();
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
            } else {
                Main.getPlugin().getLogger().warning("Undefined minigame type " + type);
            }
        }
    }

    public static final boolean isValidType(String type) {
        return minigames.containsKey(type);
    }

    public static final List<String> getTypes() {
        return minigames.keySet().stream().collect(Collectors.toList());
    }

    public static void playGame(String type) {
        if(gameState != GameState.NOGAME) {
            Main.getPlugin().getLogger().warning("playGame() called when there is already a game being played!");
            return;
        }
        gameState = GameState.INITIALIZING;

        // select a random map, given minigame type
        currentMinigame = minigames.get(type).get(new Random().nextInt(minigames.get(type).size()));
        currentMinigame.initialize();
    }

    // called by the minigame when done initializing
    public static void initializationComplete() {
        if(gameState != GameState.INITIALIZING) {
            Main.getPlugin().getLogger().warning("initializationComplete() called when initialization is already complete!");
            return;
        }
        gameState = GameState.PREGAME;

        inGame.addAll(Bukkit.getOnlinePlayers());

        ChatSender.broadcastMinigame(currentMinigame);
        correctPlayerStates();
        currentMinigame.prestart(Main.getPlugin().getServer().getOnlinePlayers().stream().collect(Collectors.toList()));
        Bukkit.getPluginManager().registerEvents(currentMinigame, Main.getPlugin());
    }

    // called by the minigame when done initializing
    public static void prestartComplete() {
        endTime = System.currentTimeMillis() + 10*1000;

        ChatSender.broadcast("Game starting in 10 seconds...");

        new BukkitRunnable() {
            @Override
            public void run() {
                ChatSender.broadcast("Game starting in 3 seconds...", ChatSound.COUNTDOWN);
            }
        }.runTaskLater(Main.getPlugin(), 7*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                ChatSender.broadcast("Game starting in 2 seconds...", ChatSound.COUNTDOWN);
            }
        }.runTaskLater(Main.getPlugin(), 8*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                ChatSender.broadcast("Game starting in 1 seconds...", ChatSound.COUNTDOWN);
            }
        }.runTaskLater(Main.getPlugin(), 9*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                ChatSender.broadcast("Game started!", ChatSound.START);

                timeout = new BukkitRunnable(){
                    @Override
                    public void run() {
                        gameComplete(currentMinigame.timeout());
                    }
                    
                };
                timeout.runTaskLater(Main.getPlugin(), currentMinigame.getTimeoutTime());
                currentMinigame.start();

                gameState = GameState.INGAME;
                endTime = System.currentTimeMillis() + currentMinigame.getTimeoutTime()*1000/20;
            }
        }.runTaskLater(Main.getPlugin(), 10*20);
        
    }

    // called by the minigame when it is done running
    public static void gameComplete(final List<Player> winners) {
        if(gameState != GameState.INGAME) {
            Main.getPlugin().getLogger().warning("gameComplete() called when not currently in a game!");
            return;
        }
        gameState = GameState.POSTGAME;

        inGame.clear();
        if(timeout != null) timeout.cancel();
        timeout = null;
        currentMinigame.postgame();     
        endTime = System.currentTimeMillis() + 10*1000;

        ChatSender.broadcastWinners(currentMinigame, winners);

        MinigameRotator.updatePoints(winners);
        // for(int i = 0; i < winners.size(); i++) {
        //     Bukkit.broadcastMessage(winners.get(i).getDisplayName());
        // }

        ChatSender.broadcast("Game ending in 10 seconds...");

        new BukkitRunnable() {
            @Override
            public void run() {
                ChatSender.broadcast("Game ending in 3 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 7*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                ChatSender.broadcast("Game ending in 2 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 8*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                ChatSender.broadcast("Game ending in 1 seconds...");
            }
        }.runTaskLater(Main.getPlugin(), 9*20);
        new BukkitRunnable(){
            @Override
            public void run() {
                // because winners is deleted by minigame.cleanup()

                ChatSender.broadcast("Game ended!");
                HandlerList.unregisterAll(currentMinigame);
                currentMinigame.cleanup();
                currentMinigame = null;
                gameState = GameState.NOGAME;

                MinigameRotator.gameComplete();


            }
        }.runTaskLater(Main.getPlugin(), 10*20);
    }

    public static void forceEndGame() {
        if(gameState.equals(GameState.NOGAME)) return;

        if(timeout != null) timeout.cancel();
        timeout = null;
        gameComplete(currentMinigame.timeout());
    }

    public static final boolean addPlayer(Player p) {
        if(!gameState.equals(GameState.NOGAME)) {
            ChatSender.tell(p, "You joined a game in progress, so you are being added as a spectator. You will be able to play in the next game.");
            currentMinigame.addPlayer(p);
            return true;
        }
        return false;
    }

    public static final boolean removePlayer(Player p) {
        if(!gameState.equals(GameState.NOGAME)) {
            removeFromGame(p);
            inGame.remove(p);
            currentMinigame.removePlayer(p);
            return true;
        }
        return false;
    }

    public static final GameState getGameState() {
        return gameState;
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

    
    public static final List<String> getScoreboardLines(Player p) {
        List<String> lines = new ArrayList<>();

        final long diff = 1000 + endTime - System.currentTimeMillis();
        final int seconds = (int) Math.floor((diff%60000) / 1000);
        final String seconds_str = seconds < 10 ? "0" + seconds : "" + seconds;

        if(gameState == GameState.PREGAME) {
            lines.add("&bGame starts: &f" + (int) Math.floor(diff/60000.) + ":" + seconds_str);
            lines.add("");
            //lines.add("&bGame: &f" + currentMinigame.getName());
            //lines.add("");
            //lines.add("&bMap: &f" + currentMinigame.getMap());
        } else if (gameState == GameState.INGAME) {
            lines.add("&bGame finishes: &f" + (int) Math.floor(diff/60000.) + ":" + seconds_str);
            lines.add("");
            lines.addAll(currentMinigame.getScoreboardLines(p));
        } else if (gameState == GameState.POSTGAME) {
            lines.add("&bGame ends: &f" + (int) Math.floor(diff/60000.) + ":" + seconds_str);
            lines.add("");
            //lines.add("&bGame: &f" + currentMinigame.getName());
            //lines.add("");
            //lines.add("&bMap: &f" + currentMinigame.getMap());
        }
        return lines;
    }

    public static boolean isInGame(Player p) {
        return inGame.contains(p);
    }

    public static int getNumberInGame() {
        return inGame.size();
    }

    public static void removeFromGame(Player p) {
        if(inGame.contains(p)) inGame.remove(p);
    }

    public static List<Player> getIngamePlayers() {
        return inGame;
    }

    // sanity checks to make sure players are not in some strange state at the start or end of games
    private static void correctPlayerStates() {
        for(Player p : Bukkit.getOnlinePlayers()) {

            // sanity checks to make sure player not in any wierd state
            if(p.getAllowFlight()) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            p.setGameMode(GameMode.SURVIVAL);
            p.setInvisible(false);
            p.setGlowing(false);
            p.setCollidable(true);
            p.getInventory().clear();
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            p.setExp(0f);
            p.setLevel(0);
        }
    }
}
