package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;

public class DiggingMinigame extends Minigame {

    private final Material[] block_types = {Material.DIRT, Material.SNOW_BLOCK,
    Material.STONE, Material.NETHERRACK, Material.DARK_OAK_LOG, Material.OAK_PLANKS, Material.COBWEB};

    private Location winnerLocation;
    private Location spectatorLocation;
    private List<Location> stackStarts;
    private int height;
    private List<Location> spawns;
    private double lowestY;

    private List<Player> winners;

    private Leaderboard leaderboard;

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = {"name", "description", "map", "stack_lowest_block_y",
            "stack_height", "stacks", "spectator_spawn", "winner_spawn"};
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
        final JSONObject spectatorJson = json.getJSONObject("spectator_spawn");
        spectatorLocation = new Location(Main.getWorld(), spectatorJson.getDouble("x"),
            spectatorJson.getDouble("y"), spectatorJson.getDouble("z"),
            spectatorJson.getFloat("yaw"), spectatorJson.getFloat("pitch"));

        final JSONObject winnerJson = json.getJSONObject("winner_spawn");
        winnerLocation= new Location(Main.getWorld(), winnerJson.getDouble("x"),
            winnerJson.getDouble("y"), winnerJson.getDouble("z"),
            winnerJson.getFloat("yaw"), winnerJson.getFloat("pitch"));

        // add the stack starting locations
        lowestY = json.getInt("stack_lowest_block_y");
        height = json.getInt("stack_height");

        stackStarts = new ArrayList<Location>();
        spawns = new ArrayList<Location>();
        for(final Object stack : json.getJSONArray("stacks")) {
            JSONObject stackJson = (JSONObject) stack;

            stackStarts.add(new Location(Main.getWorld(), stackJson.getInt("x"), lowestY, stackJson.getInt("z")));
            spawns.add(new Location(Main.getWorld(), stackJson.getDouble("x")+0.5, 0.5+lowestY+height, stackJson.getDouble("z")+0.5));
        }
    }

    @Override
    public void initialize() {
        Random rng = new Random();
        winners = new ArrayList<Player>();

        // build all the stacks
        for(int y = 0; y < height; y++) {
            Material m = block_types[rng.nextInt(block_types.length)];
            for(Location stackStart : stackStarts) {
                (new Location(stackStart.getWorld(), stackStart.getX(), stackStart.getY()+y, stackStart.getZ())).getBlock().setType(m);
            }
        }

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        List<ItemStack> tools = new ArrayList<ItemStack>();
        tools.add(new ItemStack(Material.STONE_PICKAXE));
        tools.add(new ItemStack(Material.STONE_AXE));
        tools.add(new ItemStack(Material.STONE_SHOVEL));
        tools.add(new ItemStack(Material.STONE_SWORD));
        Collections.shuffle(tools, new Random());
        
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);

            p.teleportAsync(spawns.get(i));
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            for(final ItemStack item : tools) {
                p.getInventory().addItem(item);
            }
        }

        leaderboard = new Leaderboard(players);

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
            p.getInventory().clear();
        }
        winners.clear();
    }

    @Override
    public final List<Player> timeout() {
        return winners;
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spectatorLocation);
    }

    @Override
    public void removePlayer(Player p) {
        if(MinigameManager.getNumberInGame() == 0 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            MinigameManager.gameComplete(winners);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if(MinigameManager.isInGame(e.getPlayer())) {
            boolean allowed = false;
            for (Material m : block_types) {
                if(e.getBlock().getType() == m) allowed = true;
            }
            allowed = allowed && MinigameManager.getGameState().equals(GameState.INGAME);

            if(allowed) {
                e.setCancelled(false);

                leaderboard.changeScore(e.getPlayer(), 1);

                if(e.getBlock().getLocation().getY()-0.5 <= lowestY) {
                    e.getPlayer().teleport(winnerLocation);
                    e.getPlayer().getInventory().clear();
                    MinigameManager.removeFromGame(e.getPlayer());
                    winners.add(e.getPlayer());
                    if(MinigameManager.getGameState().equals(GameState.INGAME) && (MinigameManager.getNumberInGame() == 0 || winners.size() >= 3)) {
                        MinigameManager.gameComplete(winners);
                    }
                }

                // not sure why this is needed, but it destroys the last block
                e.getBlock().setType(Material.AIR);
            }
        } else {
            e.setCancelled(true);
        }
    }

    @Override
    public final List<String> getScoreboardLinesLines(Player p) {
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
}