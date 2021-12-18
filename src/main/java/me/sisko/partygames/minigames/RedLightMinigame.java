package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.ChatSender.ChatSound;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;
import net.md_5.bungee.api.ChatColor;

public class RedLightMinigame extends Minigame {

    final private Material GREEN_OFF = Material.GREEN_WOOL;
    final private Material GREEN_ON = Material.LIME_WOOL;
    final private Material YELLOW_OFF = Material.ORANGE_STAINED_GLASS;
    final private Material YELLOW_ON = Material.YELLOW_WOOL;
    final private Material RED_OFF = Material.RED_NETHER_BRICKS;
    final private Material RED_ON = Material.RED_WOOL;

    private Location spawn;
    private int startX;
    private int endX;
    private long minRed;
    private long maxRed;
    private long minGreen;
    private long maxGreen;
    private boolean redLight;
    private boolean displayRedLight;

    private int lightBottomX;
    private int lightBottomY;
    private int lightBottomZ;
    // private Location lightTop;

    private Leaderboard distanceTravelled;
    private List<Player> winners;
    private List<BukkitRunnable> lightSchedule;


    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawn", "start_x", "end_x", "min_red", "max_red", "min_green", "max_green", "light_bottom" };
        for (final String key : keys) {
            if (!json.has(key))
                return false;
        }
        return true;
    }

    @Override
    public void setup(final JSONObject json) {

        name = json.getString("name");
        description = json.getString("description");
        map = json.getString("map");

        Main.getPlugin().getLogger().info("Setting up a red light green light map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"), spawnJson.getDouble("y"),
                spawnJson.getDouble("z"), spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));

        final JSONObject lightBottomJson = json.getJSONObject("light_bottom");
        lightBottomX = lightBottomJson.getInt("x");
        lightBottomY = lightBottomJson.getInt("y");
        lightBottomZ = lightBottomJson.getInt("z");

        startX = json.getInt("start_x");
        endX = json.getInt("end_x");
        minRed = json.getInt("min_red")*20;
        maxRed = json.getInt("max_red")*20;
        minGreen = json.getInt("min_green")*20;
        maxGreen = json.getInt("max_green")*20;
    }

    @Override
    public void initialize() {
        winners = new ArrayList<Player>();
        lightSchedule = new ArrayList<BukkitRunnable>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        // leaderboard counting distance from start
        distanceTravelled = new Leaderboard(players, false, 0); 
        redLight = true;
        displayRedLight = true;

        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            p.teleportAsync(spawn);
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        Main.getPlugin().getLogger().info("Scheduling red light green light...");
        long endTime = 0;
        Random rng = new Random();
        while (endTime < this.getTimeoutTime()) {
            // schedule green light
            long startTime = endTime;
            endTime += minGreen + rng.nextInt((int) (maxGreen - minGreen));
            BukkitRunnable b = new BukkitRunnable() {
                @Override
                public void run() {
                    redLight = false;
                    displayRedLight = false;
                    ChatSender.broadcast("" + ChatColor.GREEN + ChatColor.BOLD + "Green Light", ChatSound.GREENLIGHT);
                    setLight("red", false);
                    setLight("green", true);
                }
            };
            lightSchedule.add(b);
            lightSchedule.get(lightSchedule.size()-1).runTaskLater(Main.getPlugin(), startTime);

            // schedule red light
            startTime = endTime;
            endTime += minRed + rng.nextInt((int) (maxRed - minRed));
            b = new BukkitRunnable() {

                // actually turn red light on later, but change the display instantly
                @Override
                public void run() {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            redLight = true;
                            setLight("yellow", false);
                            setLight("red", true);
                        }
                    }.runTaskLater(Main.getPlugin(), 20);
                    ChatSender.broadcast("" + ChatColor.RED + ChatColor.BOLD + "Red Light", ChatSound.REDLIGHT);
                    displayRedLight = true; 
                    setLight("green", false);
                    setLight("yellow", true);
                }
            };
            lightSchedule.add(b);
            lightSchedule.get(lightSchedule.size()-1).runTaskLater(Main.getPlugin(), startTime);
        }
        Main.getPlugin().getLogger().info("Scheduled " + lightSchedule.size() + " red lights.");
    }

    @Override
    public void postgame() {
    }

    @Override
    public void cleanup() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setInvisible(false);
            p.getInventory().clear();
            p.setHealth(20);
        }
        for (BukkitRunnable b : lightSchedule) {
            b.cancel();
        }
        setLight("red", false);
        setLight("yellow", false);
        setLight("green", false);

        lightSchedule.clear();
        winners.clear();
    }

    @Override
    public final List<Player> timeout() {
        winners.addAll(MinigameManager.getIngamePlayers());
        // Collections.reverse(winners);
        return winners;
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spawn);
        p.setInvisible(true);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.getInventory().clear();
        p.setHealth(20);
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.getInventory().clear();
        p.setHealth(20);

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            winners.addAll(MinigameManager.getIngamePlayers());
            // Collections.reverse(winners);
            MinigameManager.gameComplete(distanceTravelled.getWinners());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(MinigameManager.isInGame(e.getPlayer())) {

            // stop players from running before game starts
            // technically redundant if game starts in red light state, but good just in case
            if (MinigameManager.getGameState().equals(GameState.PREGAME) && e.getTo().getX() >= startX) {
                e.getPlayer().teleport(spawn);

            // handle player movement during green light
            } else if(MinigameManager.getGameState().equals(GameState.INGAME) && !redLight) {
                if(e.getTo().getX() > startX && e.getTo().getX() < endX) {
                    distanceTravelled.setScore(e.getPlayer(), (int) (e.getTo().getX() - startX));
                } else if (e.getTo().getX() >= endX && !winners.contains(e.getPlayer())) {
                    winners.add(e.getPlayer());
                    ChatSender.tell(e.getPlayer(), "Congratulations, you finished!");
                    MinigameManager.removeFromGame(e.getPlayer());
                    addPlayer(e.getPlayer());
                    if (winners.size() >= 3 || MinigameManager.getIngamePlayers().size() < 1) {
                        // Collections.reverse(winners);
                        MinigameManager.gameComplete(winners);
                    }            
                }
            }

            // handle player movement during red light
            else if(MinigameManager.getGameState().equals(GameState.INGAME) && redLight) {
                if(e.getTo().getX() > startX && e.getTo().getX() < endX) {
                    ChatSender.tell(e.getPlayer(), "Don't move during red light!");
                    e.getPlayer().teleport(spawn);
                }
            }
        }
    }    

    @Override
    public final List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(!displayRedLight) {
            retVal.add("&a&lGreen Light");
        } else {
            retVal.add("&c&lRed Light");
        }
        retVal.add("&b&nDistance Travelled:");
        for(PlayerScore score : distanceTravelled.getLeaderboard()) {
            retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + score.getScore());
        }

        return retVal;
    }

    private void setLight(String light, boolean on) {
        if(light.equals("red")) {
            for(int z = lightBottomZ; z < lightBottomZ + 4; z++) {
                for(int y = lightBottomY; y < lightBottomY + 4; y++) {
                    if(on) {
                        Main.getWorld().getBlockAt(lightBottomX, y, z).setType(RED_ON);
                    } else {
                        Main.getWorld().getBlockAt(lightBottomX, y, z).setType(RED_OFF);
                    }
                }
            }
        } else if (light.equals("yellow")) {
            for(int z = lightBottomZ; z < lightBottomZ + 4; z++) {
                for(int y = lightBottomY + 5; y < lightBottomY + 9; y++) {
                    if(on) {
                        Main.getWorld().getBlockAt(lightBottomX, y, z).setType(YELLOW_ON);
                    } else {
                        Main.getWorld().getBlockAt(lightBottomX, y, z).setType(YELLOW_OFF);
                    }
                }
            }
        } else if (light.equals("green")) {
            for(int z = lightBottomZ; z < lightBottomZ + 4; z++) {
                for(int y = lightBottomY + 10; y < lightBottomY + 14; y++) {
                    if(on) {
                        Main.getWorld().getBlockAt(lightBottomX, y, z).setType(GREEN_ON);
                    } else {
                        Main.getWorld().getBlockAt(lightBottomX, y, z).setType(GREEN_OFF);
                    }
                }
            }
        }
    }
}