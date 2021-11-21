package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;

public class RedLightMinigame extends Minigame {

    private Location spawn;
    private int startX;
    private int endX;
    private boolean redLight;

    private Leaderboard distanceTravelled;
    private List<Player> winners;


    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawn", "start_x", "end_x" };
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

        Main.getPlugin().getLogger().info("Setting up a sumo map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"), spawnJson.getDouble("y"),
                spawnJson.getDouble("z"), spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));

        startX = json.getInt("start_x");
        endX = json.getInt("end_x");

    }

    @Override
    public void initialize() {
        winners = new ArrayList<Player>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        // leaderboard counting distance from start
        distanceTravelled = new Leaderboard(players, false, 0); 
        redLight = false;

        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            p.teleportAsync(spawn);
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
    }

    @Override
    public void postgame() {
    }

    @Override
    public void cleanup() {
        // for(Player p : Bukkit.getOnlinePlayers()) {
        //     p.setFlying(false);
        //     p.setAllowFlight(false);
        //     p.setInvisible(false);
        //     p.getInventory().clear();
        //     p.setHealth(20);
        // }
        winners.clear();
    }

    @Override
    public final List<Player> timeout() {
        winners.addAll(MinigameManager.getIngamePlayers());
        Collections.reverse(winners);
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
            //winners.addAll(MinigameManager.getIngamePlayers());
            //Collections.reverse(winners);
            MinigameManager.gameComplete(distanceTravelled.getWinners());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(MinigameManager.getGameState().equals(GameState.INGAME) && !redLight) {
            if(e.getTo().getX() > startX && e.getTo().getX() < endX && !winners.contains(e.getPlayer())) {
                distanceTravelled.setScore(e.getPlayer(), (int) (e.getTo().getX() - startX));
            } else if (e.getTo().getX() >= endX && !winners.contains(e.getPlayer())) {
                winners.add(e.getPlayer());
                ChatSender.tell(e.getPlayer(), "Congradulations, you finished!");
                if (winners.size() >= 3) {
                    Collections.reverse(winners);
                    MinigameManager.gameComplete(winners);
                }            
            }
        }
    }    

    @Override
    public final List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(!redLight) {
            retVal.add("&bThe light is &agreen");
        } else {
            retVal.add("&bThe light is &cred");
        }
        retVal.add("&b&nDistance Travelled:");
        for(PlayerScore score : distanceTravelled.getLeaderboard()) {
            retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + score.getScore());
        }

        return retVal;
    }
}