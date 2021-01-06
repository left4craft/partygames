package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;

public class SumoMinigame extends Minigame {

    private Location spawn;
    private int lowestY;
    private int numberOfLives;
    private Material weapon;
    private int weaponKnockback;

    private Leaderboard livesRemaining;
    //private List<Player> winners;


    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawn", "death_y", "number_of_lives", "weapon", "weapon_knockback_level" };
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

        Main.getPlugin().getLogger().info("Setting up a sumo map " + map);

        // add the spawn points
        final JSONObject spawnJson = json.getJSONObject("spawn");
        spawn = new Location(Main.getWorld(), spawnJson.getDouble("x"), spawnJson.getDouble("y"),
                spawnJson.getDouble("z"), spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch"));

        lowestY = json.getInt("death_y");
        numberOfLives = json.getInt("number_of_lives");
        weapon = Material.getMaterial(json.getString("weapon"));
        weaponKnockback = json.getInt("weapon_knockback_level");
    }

    @Override
    public void initialize() {
        //winners = new ArrayList<Player>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        // leaderboard counting the top number of lives remaining
        // once a player reaches 0 score they are eliminated
        livesRemaining = new Leaderboard(players, false, numberOfLives); 


        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            p.teleportAsync(spawn);
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        ItemStack weaponItem = new ItemStack(weapon);
        weaponItem.addUnsafeEnchantment(Enchantment.KNOCKBACK, weaponKnockback);

        for(Player p : MinigameManager.getIngamePlayers()) {
            p.getInventory().addItem(weaponItem);
        }
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
            p.setHealth(20);
        }
        //winners.clear();
    }

    @Override
    public final List<Player> timeout() {
        //winners.addAll(MinigameManager.getIngamePlayers());
        //Collections.reverse(winners);
        return livesRemaining.getWinners();
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spawn);
        p.setInvisible(true);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.getInventory().clear();
        p.setHealth(20);
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.getInventory().clear();
        p.setHealth(20);

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            //winners.addAll(MinigameManager.getIngamePlayers());
            //Collections.reverse(winners);
            MinigameManager.gameComplete(livesRemaining.getWinners());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(e.getTo().getY() < lowestY-1) {
            if(MinigameManager.isInGame(e.getPlayer()) && MinigameManager.getGameState().equals(GameState.INGAME)) {

                livesRemaining.changeScore(e.getPlayer(), -1);

                // die if no lives remaining
                if(livesRemaining.getScore(e.getPlayer()) == 0) {
                    MinigameManager.removeFromGame(e.getPlayer());
                    //winners.add(e.getPlayer());
                    addPlayer(e.getPlayer());
                    ChatSender.tell(e.getPlayer(), "You died");
                
                // else respawn
                } else {
                    e.getPlayer().teleport(spawn);
                    ChatSender.tell(e.getPlayer(), "You were knocked out of the arena and lost a life");
                }


                if(MinigameManager.getNumberInGame() <= 1) {
                    //winners.addAll(MinigameManager.getIngamePlayers());
                    //Collections.reverse(winners);
                    MinigameManager.gameComplete(livesRemaining.getWinners());
                }

            // spectator falling
            } else if (!MinigameManager.isInGame(e.getPlayer())) {
                addPlayer(e.getPlayer());
            // player is in game after winning and has just died
            } else if (MinigameManager.getGameState().equals(GameState.POSTGAME)) {
                addPlayer(e.getPlayer());
            }
            // player fall off before game, etc
            else {
                e.getPlayer().teleport(spawn);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            final Player damager = (Player) e.getDamager();
            final Player damagee = (Player) e.getEntity();

            if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
                // make the hit go through, but do no damage
                e.setCancelled(false);
                e.setDamage(0);
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
        retVal.add("&b&nLives Remaining");
        for(PlayerScore score : livesRemaining.getLeaderboard()) {
            retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + score.getScore());
        }

        return retVal;
    }
}