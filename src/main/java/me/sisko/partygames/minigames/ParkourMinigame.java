package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.MinigameManager.GameState;

public class ParkourMinigame extends Minigame {

    private Location spawn;
    private int minY;

    private List<Player> inGame;
    private List<Player> winners;
    private Map<Player, Location> checkpoints;
    private boolean gameStarted;

    private Material finishMaterial;

    @Override
    public final long getTimeoutTime() {
        return 20*60*8;
    }

    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = {"name", "description", "map", "spawn", "finish_block", "death_y"};
        for(final String key : keys) {
            if(!json.has(key)) return false;
        }

        if(Material.getMaterial(json.getString("finish_block")) == null) {
            Main.getPlugin().getLogger().warning("Invalid block in spleef configuration: " + json.getString("finish_block"));
            return false;
        }
        return true;
    }

    @Override
    public void setup(final JSONObject json) {

        name = json.getString("name");
        description = json.getString("description");
        map = json.getString("map");

        Main.getPlugin().getLogger().info("Setting up a parkour game arena map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"),
            spawnJson.getDouble("y"), spawnJson.getDouble("z"),
            spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));

        finishMaterial = Material.getMaterial(json.getString("finish_block"));
        minY = json.getInt("death_y");
    }

    @Override
    public void initialize() {
        inGame = new ArrayList<Player>();
        winners = new ArrayList<Player>();
        checkpoints = new HashMap<Player, Location>();
        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {        
        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            checkpoints.put(p, spawn);
            inGame.add(p);
            p.teleportAsync(spawn);
            p.setGameMode(GameMode.SURVIVAL);

            p.setInvisible(true);
            p.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

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

            p.getInventory().setBoots(new ItemStack(Material.AIR));
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

        p.getInventory().setBoots(new ItemStack(Material.AIR));

        if(inGame.size() == 0 && gameStarted) {
            MinigameManager.gameComplete(winners);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Material below = new Location(e.getTo().getWorld(), e.getTo().getX(), e.getTo().getY()-0.5, e.getTo().getZ()).getBlock().getType();
        Material above = new Location(e.getTo().getWorld(), e.getTo().getX(), e.getTo().getY()+0.5, e.getTo().getZ()).getBlock().getType();

        if(e.getTo().getBlock().getType().equals(finishMaterial) || below.equals(finishMaterial)) {

            // if in game set as winner
            if(gameStarted && inGame.contains(e.getPlayer())) {
                winners.add(e.getPlayer());
                inGame.remove(e.getPlayer());
            }

            // move player back to spawn regardless
            addPlayer(e.getPlayer());

            if(winners.size() == 3 || inGame.size() < 1) {
                MinigameManager.gameComplete(winners);
            }
        
        // prevent players from falling below map
        } else if (e.getTo().getY() < minY) {
            e.getPlayer().sendMessage("You fell below the map");
            if (inGame.contains(e.getPlayer())) {
                e.getPlayer().teleport(checkpoints.get(e.getPlayer()));
            } else {
                e.getPlayer().teleport(spawn);
            }
        }
        // prevent players from moving before game start
        else if(MinigameManager.getGameState() == GameState.PREGAME && e.getTo().distance(spawn) > 10) {
            e.getPlayer().teleport(spawn);
        }
        // handle checkpoints
        else if(inGame.contains(e.getPlayer()) && (e.getTo().getBlock().getType().toString().toLowerCase().contains("pressure_plate") || below.toString().toLowerCase().contains("pressure_plate") || above.toString().toLowerCase().contains("pressure_plate"))) {
            // make sure checkpoint is actually far away (more than 5 blocks)
            if(checkpoints.get(e.getPlayer()).distance(e.getTo()) > 5) {
                checkpoints.put(e.getPlayer(), e.getTo());
                e.getPlayer().sendMessage("Checkpoint reached!");

                if(e.getTo().getBlock().getType().equals(Material.CRIMSON_PRESSURE_PLATE) || below.equals(Material.CRIMSON_PRESSURE_PLATE) || above.equals(Material.CRIMSON_PRESSURE_PLATE)) {
                    ItemStack enchanted = new ItemStack(Material.NETHERITE_BOOTS);
                    enchanted.addEnchantment(Enchantment.SOUL_SPEED, 3);
                    e.getPlayer().getInventory().setBoots(enchanted);
                }
            }
        }
    }

    // bad way to handle checkpoints
    // multiple players cannot reach at same time
    // also does not save yaw/pitch

    // @EventHandler
    // public void onInteract(PlayerInteractEvent e) {
    //     if(e.getAction().equals(Action.PHYSICAL)) {
    //         Main.getPlugin().getLogger().info(e.getClickedBlock().getType().toString());
    //         if(e.getClickedBlock().getType().toString().toLowerCase().contains("pressure_plate")) {
    //             e.setCancelled(false);
    //             checkpoints.put(e.getPlayer(), e.getClickedBlock().getLocation().add(0, 1, 0));

    //             e.getPlayer().sendMessage("Checkpoint Reached!");
    //             if(e.getClickedBlock().getType().equals(Material.CRIMSON_PRESSURE_PLATE)) {
    //                 ItemStack enchanted = new ItemStack(Material.NETHERITE_BOOTS);
    //                 enchanted.addEnchantment(Enchantment.SOUL_SPEED, 3);
    //                 e.getPlayer().getInventory().setBoots(enchanted);
    //             }
    //         }
    //     }
    // }

    @Override
    public final List<String> getScoreboardLinesLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(winners.contains(p)) {
            retVal.add("&bYou have &acompleted&b the parkour");
        } else {
            retVal.add("&bYou have &cnot&b completed the parkour");
        }
        retVal.add("");
        retVal.add("&b&nFinishers");

        for(final Player winner : winners) {
            retVal.add("&a" + winner.getDisplayName());
        }

        return retVal;
    } 
}