package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;

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
import me.sisko.partygames.util.MinigameManager;

public class SpleefMinigame extends Minigame {

    private Location spawn;
    private BlockVector3[] floor;
    private int lowestY;

    private List<Player> inGame;
    private List<Player> winners;
    private boolean gameStarted;


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

        final BlockVector3[] floor = { BlockVector3.at(layerJson.getInt("x_1"), lowestY, layerJson.getInt("z_1")),
                    BlockVector3.at(layerJson.getInt("x_2"), lowestY, layerJson.getInt("z_2")) };
        this.floor = floor;

    }

    @Override
    public void initialize() {
        winners = new ArrayList<Player>();
        inGame = new ArrayList<Player>();
        gameStarted = false;

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {        
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            inGame.add(p);
            p.teleportAsync(spawn);

            p.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
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
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setInvisible(false);
            p.getInventory().clear();
        }
        inGame.clear();
        winners.clear();

        // build the tnt arena
        CuboidRegion selection = new CuboidRegion(floor[0], floor[1]);
        try {
            EditSession edit = new EditSessionBuilder(BukkitAdapter.adapt(Main.getWorld())).build();
            edit.setBlocks((Region) selection, BlockTypes.SNOW_BLOCK);
            edit.close();
        } catch (MaxChangedBlocksException e) {
            Main.getPlugin().getLogger().warning("Could not set the spleef floor!");
            e.printStackTrace();
        }
    }

    @Override
    public final List<Player> timeout() {
        winners.addAll(inGame);
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
        if(inGame.contains(p)) inGame.remove(p);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.getInventory().clear();

        if(inGame.size() <= 1 && gameStarted) {
            winners.addAll(inGame);
            Collections.reverse(winners);
            MinigameManager.gameComplete(winners);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(e.getTo().getY() < lowestY-1) {
            e.setCancelled(true);
            if(inGame.contains(e.getPlayer()) && gameStarted) {
                inGame.remove(e.getPlayer());
                winners.add(e.getPlayer());
                addPlayer(e.getPlayer());  

                if(inGame.size() <= 1) {
                    winners.addAll(inGame);
                    Collections.reverse(winners);
                    MinigameManager.gameComplete(winners);
                }
            } else {
                e.setCancelled(false);
                addPlayer(e.getPlayer());
            } 
        } 
    }

    @Override
    public final List<String> getScoreboardLinesLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(inGame.contains(p)) {
            retVal.add("&bYou are &aalive");
        } else {
            retVal.add("&bYou are &cdead");
        }
        retVal.add("&bPlayers left: &f" + inGame.size());
        for(Player ingamePlayer : inGame) {
            retVal.add("&a" + ingamePlayer.getDisplayName());
        }

        return retVal;
    } 

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if(gameStarted && inGame.contains(e.getPlayer()) && e.getBlock().getType().equals(Material.SNOW_BLOCK)) {
            e.setCancelled(false);
        } else {
            e.setCancelled(true);
        }
    }
}