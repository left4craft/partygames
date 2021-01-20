package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;

public class OneInTheChamberMinigame extends Minigame {

    private List<Location> spawns;
    private int killsToWin;

    private Leaderboard kills;
    //private List<Player> winners;


    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawns", "kills_to_win" };
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

        Main.getPlugin().getLogger().info("Setting up a last player standing map " + map);

        spawns = new ArrayList<Location>();
        for(final Object spawn : json.getJSONArray("spawns")) {
            JSONObject spawnJson = (JSONObject) spawn;

            spawns.add(new Location(Main.getWorld(), spawnJson.getDouble("x"),
                spawnJson.getDouble("y"), spawnJson.getDouble("z"),
                spawnJson.getFloat("yaw"), spawnJson.getFloat("pitch")));
        }

        killsToWin = json.getInt("kills_to_win");
    }

    @Override
    public void initialize() {
        //winners = new ArrayList<Player>();

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        // leaderboard counting how many kills each player has
        kills = new Leaderboard(players); 


        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            p.teleportAsync(getRandomSpawn());
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        for(Player p : MinigameManager.getIngamePlayers()) {
            p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
            p.getInventory().addItem(new ItemStack(Material.BOW));
            p.getInventory().addItem(new ItemStack(Material.ARROW));
        }
    }

    @Override
    public void postgame() {
    }

    @Override
    public void cleanup() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setFlying(false);
            p.setCollidable(true);
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
        return kills.getWinners();
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(spawns.get(0));
        p.setInvisible(true);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
        p.getInventory().clear();
        p.setHealth(20);
        p.setFireTicks(0);
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setCollidable(true);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.getInventory().clear();
        p.setHealth(20);

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            MinigameManager.gameComplete(kills.getWinners());
        }
    }

    @EventHandler
    public void onDamagedByPlayer(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            final Player damager = (Player) e.getDamager();
            final Player damagee = (Player) e.getEntity();

            if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
                // don't do anything for nonlethal
                if (damagee.getHealth() - e.getFinalDamage() > 0) {
                    e.setCancelled(false);
                    return;
                }

                e.setCancelled(true);

                killPlayer(damager, damagee);
            }
        } else if (e.getDamager() instanceof Arrow && e.getEntity() instanceof Player) {
            final Arrow arrow = (Arrow) e.getDamager();
            if(arrow.getShooter() instanceof Player) {
                final Player damagee = (Player) e.getEntity();
                final Player damager = (Player) arrow.getShooter();
                if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
                    e.setCancelled(true);
                    killPlayer(damager, damagee);
                }
    
            }

        }
    }

    @Override
    public final List<String> getScoreboardLines(Player p) {
        List<String> retVal = new ArrayList<String>();
        if(MinigameManager.isInGame(p)) {
            retVal.add("&bYou are &aalive");
        } else {
            retVal.add("&bYou are &cspectating");
        }
        retVal.add("&b&nKill Leaderboard");
        for(PlayerScore score : kills.getLeaderboard()) {
            retVal.add("&a" + score.getPlayer().getDisplayName() + "&r&b: &f" + score.getScore());
        }

        return retVal;
    }

    private Location getRandomSpawn() {
        return spawns.get((new Random()).nextInt(spawns.size()));
    }

    private void killPlayer(final Player damager, final Player damagee) {

        // prevent platyer from scoring points by damaging self
        if(damager.getUniqueId().equals(damagee.getUniqueId())) return;

        kills.changeScore(damager, 1);
        damager.getInventory().addItem(new ItemStack(Material.ARROW));
        damager.playSound(damager.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 0.5f);

        // respawn
        damagee.teleport(getRandomSpawn());
        damagee.setHealth(20);
        damagee.setFireTicks(0);
        resetArrows(damagee);
        ChatSender.tell(damager, "You killed " + damagee.getDisplayName());
        ChatSender.tell(damagee, "You were killed by " + damager.getDisplayName());

        if(kills.getScore(damager) >= killsToWin) {
            MinigameManager.gameComplete(kills.getWinners());
        }
    }

    private void resetArrows(Player p) {
        for(ItemStack item : p.getInventory().getContents()) {
            if(item != null && item.getType().equals(Material.ARROW)) {
                p.getInventory().remove(item);
            }
        }
        p.getInventory().addItem(new ItemStack(Material.ARROW));
    }
}