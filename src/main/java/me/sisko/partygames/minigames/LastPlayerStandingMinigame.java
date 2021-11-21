package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;
import net.md_5.bungee.api.ChatColor;

public class LastPlayerStandingMinigame extends Minigame {

    private List<Location> spawns;
    private int numberOfLives;

    private Leaderboard livesRemaining;
    private Map<Player, Player> lastHit;


    @Override
    public final boolean jsonValid(final JSONObject json) {
        final String[] keys = { "name", "description", "map", "spawns", "number_of_lives" };
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

        numberOfLives = json.getInt("number_of_lives");
    }

    @Override
    public void initialize() {

        MinigameManager.initializationComplete();
    }

    @Override
    public void prestart(final List<Player> players) {
        lastHit = new HashMap<Player, Player>();

        // leaderboard counting the top number of lives remaining
        // once a player reaches 0 score they are eliminated
        livesRemaining = new Leaderboard(players, false, numberOfLives); 


        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            
            lastHit.put(p, null);
            p.teleportAsync(getRandomSpawn());
            p.setGlowing(true);
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        for(Player p : MinigameManager.getIngamePlayers()) {
            p.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            p.getInventory().addItem(new ItemStack(Material.BOW));
            p.getInventory().addItem(new ItemStack(Material.ARROW, 32));

            p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

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
            p.setGlowing(false);
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
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
        p.teleport(spawns.get(0));
        p.setInvisible(true);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
        p.getInventory().clear();
        p.setHealth(20);
        p.setFireTicks(0);
        p.setGlowing(false);
        p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setCollidable(true);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.setGlowing(false);
        p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        p.getInventory().clear();
        p.setHealth(20);

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            //winners.addAll(MinigameManager.getIngamePlayers());
            //Collections.reverse(winners);
            MinigameManager.gameComplete(livesRemaining.getWinners());
        }
    }

    @EventHandler
    public void onDamagedByPlayer(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            final Player damager = (Player) e.getDamager();
            final Player damagee = (Player) e.getEntity();

            if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
                lastHit.put(damagee, damager);

                // don't do anything for nonlethal
                if (damagee.getHealth() - e.getFinalDamage() > 0) {
                    e.setCancelled(false);
                    return;
                }

                e.setCancelled(true);

                killPlayer(damager, damagee);
            }

        } else if (e.getDamager() instanceof Arrow && e.getEntity() instanceof Player) {
            final Player damagee = (Player) e.getEntity();
            final Arrow arrow = (Arrow) e.getDamager();

            if(arrow.getShooter() instanceof Player) {
                final Player damager = (Player) arrow.getShooter();
                if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damagee)) {
                    lastHit.put(damagee, damager);

                    // don't do anything for nonlethal
                    if (damagee.getHealth() - e.getFinalDamage() > 0) {
                        e.setCancelled(false);
                        return;
                    }
    
                    e.setCancelled(true);
    
                    killPlayer(damager, damagee);
                }
            }


        }
    }

    @EventHandler
    public void onDamagedByEnvironment(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player)) return;

        Player damagee = (Player) e.getEntity();
        if(e.getCause().equals(DamageCause.LAVA)) {
            damagee.setFireTicks(0);

            if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damagee)) {
                // don't do anything for nonlethal
                if (damagee.getHealth() - e.getFinalDamage() > 0) {
                    e.setCancelled(false);
                    return;
                }

                e.setCancelled(true);
                killPlayer(null, damagee);
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

    private Location getRandomSpawn() {
        return spawns.get((new Random()).nextInt(spawns.size()));
    }

    private void killPlayer(Player damager, final Player damagee) {
        if(damager == null) {
            damager = lastHit.get(damagee);
        }

        livesRemaining.changeScore(damagee, -1);
        // die if no lives remaining
        if(livesRemaining.getScore(damagee) == 0) {
            MinigameManager.removeFromGame(damagee);
            addPlayer(damagee);
            ChatSender.tell(damagee, "You ran out of lives");
        
        // else respawn
        } else {
            damagee.teleport(getRandomSpawn());
            damagee.setHealth(20);
            damagee.setFireTicks(0);
            ChatSender.tell(damagee, "You were killed and lost a life");
        }

        // tell damager they got a kill and increment strength
        if(damager != null) {
            ChatSender.tell(damager, "You killed " + damagee.getDisplayName() + ChatColor.GRAY + " and gained increased strength");
            damager.playSound(damager.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 0.5f);

            if(damager.getPotionEffect(PotionEffectType.INCREASE_DAMAGE) == null) {
                damager.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false, true));
            } else {
                int amplevel = damager.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier()+1;
                damager.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                damager.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, amplevel, false, false, true));
            }
            damager.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20*10, 1, false, false, true));

        }

        if(MinigameManager.getNumberInGame() <= 1) {
            //winners.addAll(MinigameManager.getIngamePlayers());
            //Collections.reverse(winners);
            MinigameManager.gameComplete(livesRemaining.getWinners());
        }
    }
}