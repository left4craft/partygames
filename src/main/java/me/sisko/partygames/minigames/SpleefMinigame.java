package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.MinigameManager.GameState;

public class SpleefMinigame extends Minigame {

    private Location spawn;
    private Location[] floor;
    private int lowestY;

    private List<Player> winners;


    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawn", "floor" };
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

        Main.getPlugin().getLogger().info("Setting up a spleef map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"), spawnJson.getDouble("y"),
                spawnJson.getDouble("z"), spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));

        lowestY = 255;

        JSONObject layerJson = json.getJSONObject("floor");

        lowestY = layerJson.getInt("y");

        final Location[] floor = { new Location(Main.getWorld(), Math.min(layerJson.getInt("x_1"), layerJson.getInt("x_2")), lowestY, 
                Math.min(layerJson.getInt("z_1"), layerJson.getInt("z_2"))),
                new Location(Main.getWorld(), Math.max(layerJson.getInt("x_1"), layerJson.getInt("x_2")), lowestY, 
                Math.max(layerJson.getInt("z_1"), layerJson.getInt("z_2"))) };
        this.floor = floor;

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
            
            p.teleport(spawn);

            p.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
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
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setInvisible(false);
            p.getInventory().clear();
        }
        winners.clear();

        for(double x = floor[0].getX(); x < floor[1].getX(); x++) {
            for(double z = floor[0].getZ(); z < floor[1].getZ(); z++) {
                Main.getWorld().getBlockAt(new Location(Main.getWorld(), x, lowestY, z)).setType(Material.SNOW_BLOCK);
            }
        }
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
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.getInventory().clear();

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            winners.addAll(MinigameManager.getIngamePlayers());
            Collections.reverse(winners);
            MinigameManager.gameComplete(winners);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(e.getTo().getY() < lowestY-1) {
            e.setCancelled(true);
            if(MinigameManager.isInGame(e.getPlayer()) && MinigameManager.getGameState().equals(GameState.INGAME)) {
                MinigameManager.removeFromGame(e.getPlayer());
                winners.add(e.getPlayer());
                addPlayer(e.getPlayer());  
                ChatSender.tell(e.getPlayer(), "You died");

                if(MinigameManager.getNumberInGame() <= 1) {
                    winners.addAll(MinigameManager.getIngamePlayers());
                    Collections.reverse(winners);
                    MinigameManager.gameComplete(winners);
                }
            // only call addPlayer if the state is in game
            } else {
                e.getPlayer().teleport(spawn);
            }
        } 
    }

    @Override
    public final List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(MinigameManager.isInGame(p)) {
            retVal.add("&bYou are &aalive");
        } else {
            retVal.add("&bYou are &cdead");
        }
        retVal.add("&bPlayers left: &f" + MinigameManager.getNumberInGame());
        for(Player ingamePlayer : MinigameManager.getIngamePlayers()) {
            retVal.add("&a" + ingamePlayer.getDisplayName());
        }

        return retVal;
    } 

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if(MinigameManager.getGameState().equals(GameState.INGAME) && MinigameManager.isInGame(e.getPlayer()) && e.getBlock().getType().equals(Material.SNOW_BLOCK)) {
            e.setCancelled(false);
        } else {
            e.setCancelled(true);
        }
    }
}