package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;

public class HoeHoeHoeMinigame extends Minigame {
    final Material[] colors = {Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
        Material.LIME_WOOL, Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL, Material.WHITE_WOOL,
        Material.PINK_WOOL};

    private Location spawn;
    private Location center;
    private double radius;
    private int placed;
    private int numBlocks;

    private Leaderboard leaderboard;
    private Map<Player, Material> playerColors;
    private Map<Player, Boolean> playerStarted;

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawn", "center", "radius", "num_blocks" };
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

        Main.getPlugin().getLogger().info("Setting up a hoehoehoe map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"), spawnJson.getDouble("y"),
                spawnJson.getDouble("z"), spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));

        JSONObject centerJson = json.getJSONObject("center");

        center = new Location(Main.getWorld(), centerJson.getInt("x"), centerJson.getInt("y"), centerJson.getInt("z"));
        radius = json.getDouble("radius");

        numBlocks = json.getInt("num_blocks");
    }

    @Override
    public void initialize() {
        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {   
        leaderboard = new Leaderboard(players);
        playerColors = new HashMap<Player, Material>();
        playerStarted = new HashMap<Player, Boolean>();
        placed = 0;

        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            playerColors.put(p, colors[i]);
            playerStarted.put(p, false);

            p.teleport(spawn);
            p.getInventory().addItem(new ItemStack(Material.DIAMOND_HOE));
            p.getInventory().addItem(new ItemStack(playerColors.get(p)));

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

        // build the arena
        for(double x = center.getX()-radius; x < center.getX()+radius; x++) {
            for(double z = center.getZ()-radius; z < center.getZ()+radius; z++) {
                Location iter = new Location(Main.getWorld(), x, center.getY(), z);
                if(iter.distance(center) <= radius) {
                    Main.getWorld().getBlockAt(iter).setType(Material.GRASS_BLOCK);
                }
            }
        }
    }

    @Override
    public final List<Player> timeout() {
        return leaderboard.getWinners();
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
            MinigameManager.gameComplete(leaderboard.getWinners());
        }
    }

    @Override
    public final List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(leaderboard.contains(p)) {
            retVal.add("&bYou are in &f" + leaderboard.getPlace(p) + "&b place");
        } else {
            retVal.add("&bYou are spectating");
        }
        retVal.add("&b&nLeaderboard");

        for(final PlayerScore score : leaderboard.getLeaderboard()) {
            retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + score.getScore());
        }

        return retVal;
    } 

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        e.setCancelled(true);

        // check the player is indeed in game and clicked a grass block using a diamond hoe
        if(MinigameManager.getGameState().equals(GameState.INGAME) && MinigameManager.isInGame(e.getPlayer()) 
            && e.getAction().equals(Action.RIGHT_CLICK_BLOCK) 
            && e.getItem() != null && e.getItem().getType().equals(Material.DIAMOND_HOE)
            && e.getClickedBlock().getType().equals(Material.GRASS_BLOCK)) {

            final Material color = playerColors.get(e.getPlayer());
            final Block block = e.getClickedBlock();
            // if not started, change block no matter what
            if(!playerStarted.get(e.getPlayer())) {
                block.setType(color);
                playerStarted.put(e.getPlayer(), true);
                leaderboard.changeScore(e.getPlayer(), 1);
                placed++;
            
            // player has started and an adjascent block is the correct material
            } else if (block.getRelative(BlockFace.NORTH).getType().equals(color) || block.getRelative(BlockFace.EAST).getType().equals(color)
                || block.getRelative(BlockFace.SOUTH).getType().equals(color) || block.getRelative(BlockFace.WEST).getType().equals(color)) {
                    block.setType(color);
                    leaderboard.changeScore(e.getPlayer(), 1);
                    placed++;
            }

            // check if map is full of blocks
            if(placed >= numBlocks) {
                MinigameManager.gameComplete(leaderboard.getWinners());
            }
        }
    }
}