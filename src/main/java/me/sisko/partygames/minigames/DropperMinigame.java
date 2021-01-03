package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.MinigameManager;

public class DropperMinigame extends Minigame {

    private Location spawn;

    private List<Player> inGame;
    private List<Player> winners;
    private boolean gameStarted;

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = {"name", "description", "map", "spawn"};
        for(final String key : keys) {
            if(!json.has(key)) return false;
        }
        return true;
    }

    @Override
    public void setup(final JSONObject json) {

        name = json.getString("name");
        description = json.getString("description");
        map = json.getString("map");

        Main.getPlugin().getLogger().info("Setting up a digging game arena map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"),
            spawnJson.getDouble("y"), spawnJson.getDouble("z"),
            spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));
    }

    @Override
    public void initialize() {
        inGame = new ArrayList<Player>();
        winners = new ArrayList<Player>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {        
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            inGame.add(p);
            p.teleportAsync(spawn);
            p.setGameMode(GameMode.SURVIVAL);
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        gameStarted = true;
    }

    @Override
    public void postgame() {
        gameStarted = false;
    }

    @Override
    public void cleanup() {
        inGame.clear();
        winners.clear();

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setInvisible(false);
        }
    }

    @Override
    public final List<Player> timeout() {
        return winners;
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spawn);
        p.setInvisible(true);
        p.setAllowFlight(true);
    }

    @Override
    public void removePlayer(Player p) {
        if(inGame.contains(p)) inGame.remove(p);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);

        if(inGame.size() == 0 && gameStarted) {
            MinigameManager.gameComplete(winners);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if(e.getCause().equals(DamageCause.FALL) && e.getEntityType().equals(EntityType.PLAYER)) {
            e.getEntity().teleport(spawn);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(e.getTo().getBlock().getType().equals(Material.WATER) && gameStarted && inGame.contains(e.getPlayer())) {
            winners.add(e.getPlayer());
            inGame.remove(e.getPlayer());
            addPlayer(e.getPlayer());

            if(winners.size() == 3 || inGame.size() < 1) {
                MinigameManager.gameComplete(winners);
            }
        } else if (!gameStarted && e.getTo().getY() < spawn.getY()-1.) {
            e.getPlayer().teleport(spawn);
        }
    }

    @Override
    public final List<String> getScoreboardLinesLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(winners.contains(p)) {
            retVal.add("&bYou have &acompleted&b the dropper");
        } else {
            retVal.add("&bYou have &cnot&b completed the dropper");
        }
        retVal.add("");
        retVal.add("&b&nFinishers");

        for(final Player winner : winners) {
            retVal.add("&a" + winner.getDisplayName());
        }

        return retVal;
    } 
}