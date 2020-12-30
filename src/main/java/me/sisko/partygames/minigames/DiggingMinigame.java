package me.sisko.partygames.minigames;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

import me.sisko.partygames.Main;

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
    private double winningY;
    
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
        int lowest_y = json.getInt("stack_lowest_block_y");
        height = json.getInt("stack_height");
        winningY = ((double) lowest_y) - 0.5;

        for(final Object stack : json.getJSONArray("stacks")) {
            JSONObject stackJson = (JSONObject) stack;
            stackStarts.add(new Location(Main.getWorld(), stackJson.getInt("x"), lowest_y, stackJson.getInt("z")));
            spawns.add(new Location(Main.getWorld(), stackJson.getDouble("x")+0.5, 0.5+lowest_y+height, stackJson.getDouble("z")+0.5));
        }
    }

    @Override
    public void initialize() {
        Random rng = new Random();

        // build all the stacks
        for(int y = 0; y < height; y++) {
            Material m = block_types[rng.nextInt(block_types.length)];
            for(Location stackStart : stackStarts) {
                stackStart.add(0, y, 0).getBlock().setType(m);
            }
        }
    }

    @Override
    public void start(List<Player> players) {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(e.getTo().getY() < winningY) {
            e.getPlayer().teleport(winnerLocation);
        }
    }
}