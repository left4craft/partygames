package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.MinigameManager;

public class DiggingMinigame extends Minigame {
    private String name;
    private String description;
    private String map;

    private final Material[] block_types = {Material.DIRT, Material.SNOW_BLOCK,
    Material.STONE, Material.NETHERRACK, Material.DARK_OAK_LOG, Material.OAK_PLANKS};

    private Location winnerLocation;
    private Location spectatorLocation;
    private List<Location> stackStarts;
    private int height;
    private List<Location> spawns;
    private double lowestY;

    private List<Player> inGame;
    private List<Player> winners;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getMap() {
        return map;
    }

    @Override
    public boolean jsonValid(JSONObject json) {
        final String[] keys = {"name", "description", "map", "stack_lowest_block_y",
            "stack_height", "stacks", "spectator_spawn", "winner_spawn"};
        for(final String key : keys) {
            if(!json.has(key)) return false;
        }
        return true;
    }

    @Override
    public void setup(JSONObject json) {

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
    public void start(final List<Player> players) {
        inGame = new ArrayList<Player>();
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            p.teleport(spawns.get(i));
            inGame.add(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            p.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            p.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));
        }
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

    @Override @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().teleport(spectatorLocation);
    }

    @Override @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if(inGame.contains(e.getPlayer())) inGame.remove(e.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if(inGame.contains(e.getPlayer())) {
            boolean allowed = false;
            for (Material m : block_types) {
                if(e.getBlock().getType() == m) allowed = true;
            }
            if(allowed) {
                e.setCancelled(false);
                if(e.getBlock().getLocation().getY()-0.5 <= lowestY) {
                    e.getPlayer().teleport(winnerLocation);
                    inGame.remove(e.getPlayer());
                    winners.add(e.getPlayer());
                    
                }
            }
        } else {
            e.setCancelled(true);
        }
    }
}