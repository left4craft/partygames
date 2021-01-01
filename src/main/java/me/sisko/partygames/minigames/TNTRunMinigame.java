package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.MinigameManager;

public class TNTRunMinigame extends Minigame {

    private Location spawn;
    private List<BlockVector3[]> layers;
    private int lowestY;

    private List<Player> inGame;
    private List<Player> winners;
    private boolean gameStarted;

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = {"name", "description", "map", "spawn",
            "layers"};
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

        Main.getPlugin().getLogger().info("Setting up a tnt run map " + map);

        // add the spawn points
        final JSONObject spectatorJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spectatorJson.getDouble("x"),
            spectatorJson.getDouble("y"), spectatorJson.getDouble("z"),
            spectatorJson.getFloat("yaw"), spectatorJson.getFloat("pitch"));

        
        lowestY = 255;
        layers = new ArrayList<BlockVector3[]>();
        for(final Object layer : json.getJSONArray("layers")) {
            JSONObject layerJson = (JSONObject) layer;

            final int y = layerJson.getInt("y");
            if(y < lowestY) lowestY = y;

            BlockVector3[] layerLocation = {
                BlockVector3.at(layerJson.getInt("x_1"), y, layerJson.getInt("z_1")),
                BlockVector3.at(layerJson.getInt("x_2"), y, layerJson.getInt("z_2"))
            };

            layers.add(layerLocation);
        }
    }

    @Override
    public void initialize() {
        winners = new ArrayList<Player>();
        inGame = new ArrayList<Player>();
        gameStarted = false;

        // build the tnt arena
        for(BlockVector3[] layer : layers) {
            CuboidRegion selection = new CuboidRegion(layer[0], layer[1]);

            try {
                Pattern pattern = new PatternFactory(WorldEdit.getInstance()).parseFromInput("minecraft:red_sandstone", new ParserContext());
                WorldEdit.getInstance().newEditSession(new BukkitWorld(Main.getWorld())).setBlocks(selection, pattern);
            } catch (InputParseException | MaxChangedBlocksException e) {
                Main.getPlugin().getLogger().warning("Could not set the tnt run floor!");
                e.printStackTrace();
            }

        }

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {        
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            inGame.add(p);
            p.teleport(spawn);
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
        }
        inGame.clear();
        winners.clear();
    }

    @Override
    public final List<Player> timeout() {
        for (Player p : inGame) {
            winners.add(p);
        }
        Collections.reverse(winners);
        return winners;
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spawn);
        p.setFlying(true);
        p.setAllowFlight(true);
    }

    @Override
    public void removePlayer(Player p) {
        if(inGame.contains(p)) inGame.remove(p);
        p.setFlying(false);
        p.setAllowFlight(false);

        if(inGame.size() == 0 && gameStarted) {
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

                if(inGame.size() == 0) {
                    Collections.reverse(winners);
                    MinigameManager.gameComplete(winners);
                }
            }
        } else if (inGame.contains(e.getPlayer()) && gameStarted) {
            Block below = e.getPlayer().getLocation().add(0, -1, 0).getBlock();

            // @TODO make block types configurable
            // also need to change the initialize() method
            if(below.getType().equals(Material.RED_SANDSTONE)) {
                below.setType(Material.ORANGE_TERRACOTTA);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        below.setType(Material.AIR);
                    }
                }.runTaskLater(Main.getPlugin(), 20);
            }
        }
    }
}