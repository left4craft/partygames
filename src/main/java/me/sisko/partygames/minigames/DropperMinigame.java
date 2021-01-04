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
import me.sisko.partygames.util.MinigameManager.GameState;

public class DropperMinigame extends Minigame {

    private Location spawn;

    private List<Player> winners;

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

        Main.getPlugin().getLogger().info("Setting up a dropper game arena map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"),
            spawnJson.getDouble("y"), spawnJson.getDouble("z"),
            spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));
    }

    @Override
    public void initialize() {
        winners = new ArrayList<Player>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {        
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            p.teleportAsync(spawn);
            p.setGameMode(GameMode.SURVIVAL);
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
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);

        if(MinigameManager.getNumberInGame() == 0 && MinigameManager.getGameState().equals(GameState.INGAME)) {
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
        if(e.getTo().getBlock().getType().equals(Material.WATER) && MinigameManager.getGameState().equals(GameState.INGAME) && MinigameManager.isInGame(e.getPlayer())) {
            winners.add(e.getPlayer());
            MinigameManager.removeFromGame(e.getPlayer());
            addPlayer(e.getPlayer());

            if(winners.size() == 3 || MinigameManager.getNumberInGame() < 1) {
                MinigameManager.gameComplete(winners);
            }
        } else if (!MinigameManager.getGameState().equals(GameState.INGAME) && e.getTo().getY() < spawn.getY()-1.5) {
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