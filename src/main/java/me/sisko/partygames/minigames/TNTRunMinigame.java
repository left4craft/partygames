package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.MinigameManager.GameState;

public class TNTRunMinigame extends Minigame {

    private Location spawn;
    private List<BlockVector3[]> layers;
    private int lowestY;

    private List<Player> winners;

    private BukkitRunnable floorDecay;

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawn", "layers" };
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

        Main.getPlugin().getLogger().info("Setting up a tnt run map " + map);

        // add the spawn points
        final JSONObject spectatorJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spectatorJson.getDouble("x"), spectatorJson.getDouble("y"),
                spectatorJson.getDouble("z"), spectatorJson.getFloat("yaw"), spectatorJson.getFloat("pitch"));

        lowestY = 255;
        layers = new ArrayList<BlockVector3[]>();
        for (final Object layer : json.getJSONArray("layers")) {
            JSONObject layerJson = (JSONObject) layer;

            final int y = layerJson.getInt("y");
            if (y < lowestY)
                lowestY = y;

            BlockVector3[] layerLocation = { BlockVector3.at(layerJson.getInt("x_1"), y, layerJson.getInt("z_1")),
                    BlockVector3.at(layerJson.getInt("x_2"), y, layerJson.getInt("z_2")) };

            layers.add(layerLocation);
        }
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
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        floorDecay = new BukkitRunnable(){
            @Override
            public void run() {
                Random rng = new Random();
                for (BlockVector3[] layer : layers) {
                    final int minz = Math.min(layer[0].getBlockZ(), layer[1].getBlockZ());
                    final int maxz = Math.max(layer[0].getBlockZ(), layer[1].getBlockZ());

                    final int minx = Math.min(layer[0].getBlockX(), layer[1].getBlockX());
                    final int maxx = Math.max(layer[0].getBlockX(), layer[1].getBlockX());

                    for(int x = minx; x <= maxx; x++) {
                        for(int z = minz; z < maxz; z++) {
                            if(rng.nextInt(100) == 1) {
                                Block block = new Location(Main.getWorld(), x, layer[0].getBlockY(), z).getBlock();
                                if(!(block.getType() == Material.AIR)) {
                                    block.setType(Material.ORANGE_TERRACOTTA);                         
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            block.setType(Material.AIR);
                                        }
                                    }.runTaskLater(Main.getPlugin(), 10);
                                }
                            }
                        }
                    }
                }
            }
        };

        floorDecay.runTaskTimer(Main.getPlugin(), 0, 20);

        for(Player p : MinigameManager.getIngamePlayers()) {
            breakBlocksAtFeet(p);
        }
    }

    @Override
    public void postgame() {
        floorDecay.cancel();
    }

    @Override
    public void cleanup() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setInvisible(false);
        }
        winners.clear();

        // build the tnt arena
        for (BlockVector3[] layer : layers) {
            CuboidRegion selection = new CuboidRegion(layer[0], layer[1]);
            try {
                EditSession edit = new EditSessionBuilder(BukkitAdapter.adapt(Main.getWorld())).build();
                edit.setBlocks((Region) selection, BlockTypes.RED_SANDSTONE);
                edit.close();
            } catch (MaxChangedBlocksException e) {
                Main.getPlugin().getLogger().warning("Could not set the tnt run floor!");
                e.printStackTrace();
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
            } else if (!MinigameManager.isInGame(e.getPlayer())) {
                e.setCancelled(false);
                addPlayer(e.getPlayer());
            
            // player is in game after winning and has just died
            } else {
                addPlayer(e.getPlayer());
            }
        } else if (MinigameManager.isInGame(e.getPlayer()) && MinigameManager.getGameState().equals(GameState.INGAME)) {

            breakBlocksAtFeet(e.getPlayer());
            // after game ends, winner falls down
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

    private void breakBlocksAtFeet(Player p ) {
        Block breakBlocks[] = new Block[9];
        // get blocks all around where the player is standing
        // such that it is impossible for player to stay in one place
        for(int i = 0; i < 9; i++) {
            breakBlocks[i] = p.getLocation().add(((i%3)-1)*0.75, -0.5, (Math.floor(i/3)-1)*0.75).getBlock();
        }

        // @TODO make block types configurable
        // also need to change the initialize() method
        for(Block below : breakBlocks) {
            if(below.getType().equals(Material.RED_SANDSTONE)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        below.setType(Material.ORANGE_TERRACOTTA);
                    }
                }.runTaskLater(Main.getPlugin(), 10);
            
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