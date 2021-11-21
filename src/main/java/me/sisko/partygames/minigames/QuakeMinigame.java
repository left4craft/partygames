package me.sisko.partygames.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.json.JSONObject;

import me.sisko.partygames.Main;
import me.sisko.partygames.util.ChatSender;
import me.sisko.partygames.util.Leaderboard;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.XpBarFill;
import me.sisko.partygames.util.Leaderboard.PlayerScore;
import me.sisko.partygames.util.MinigameManager.GameState;
import net.md_5.bungee.api.ChatColor;

public class QuakeMinigame extends Minigame {

    private List<Location> spawns;
    private int killsToWin;
    private Map<Player, Long> lastShot;

    private Leaderboard kills;


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

        Main.getPlugin().getLogger().info("Setting up a quake map " + map);

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
        lastShot = new HashMap<Player, Long>();

        for(int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            lastShot.put(p, 0l);
            p.teleport(getRandomSpawn());
        }
        MinigameManager.prestartComplete();
    }

    @Override
    public void start() {
        ItemStack gun = new ItemStack(Material.DIAMOND_HOE);
        gun.setAmount(1);
        gun.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        gun.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        ItemMeta meta = gun.getItemMeta();
        meta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "Rail Gun");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Right click to shoot");
        meta.setLore(lore);
        gun.setItemMeta(meta);

        for(Player p : MinigameManager.getIngamePlayers()) {
            p.getInventory().addItem(gun);
            p.setExp(1f);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 2, false, false, true));
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
            p.removePotionEffect(PotionEffectType.SPEED);
            p.removePotionEffect(PotionEffectType.JUMP);
            p.setExp(0f);
        }
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
        p.removePotionEffect(PotionEffectType.SPEED);
        p.removePotionEffect(PotionEffectType.JUMP);
    }

    @Override
    public void removePlayer(Player p) {
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvisible(false);
        p.setCollidable(true);
        p.getInventory().clear();
        p.setHealth(20);
        p.setExp(0f);

        if(MinigameManager.getNumberInGame() <= 1 && MinigameManager.getGameState().equals(GameState.INGAME)) {
            MinigameManager.gameComplete(kills.getWinners());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        // return if not shooting a gun
        if(!( MinigameManager.isInGame(e.getPlayer()) && e.getItem() != null && e.getItem().getType().equals(Material.DIAMOND_HOE) && System.currentTimeMillis() - lastShot.get(e.getPlayer()) > 1000l )) {
            return;
        }
        e.getPlayer().getLocation().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1f, 0.5f);
        lastShot.put(e.getPlayer(), System.currentTimeMillis());

        Location bullet = e.getPlayer().getEyeLocation();
        Vector direction = bullet.getDirection().normalize().multiply(0.5);
        new XpBarFill(20, e.getPlayer()).runTask(Main.getPlugin());
        for(int i = 0; i < 400; i++) {
            bullet.add(direction);

            // check for collision with block
            Block b = bullet.getBlock();
            if(b != null && b.isSolid() && !b.getType().toString().contains("DOOR") && !b.getType().toString().contains("SIGN") && !b.getType().toString().contains("SLAB")) {
                break;
            }

            // check for collision with player
            for(Entity damagee : bullet.getNearbyEntitiesByType(Player.class, 0.5, 0.5, 0.5, entity -> !entity.getUniqueId().equals(e.getPlayer().getUniqueId()))) {
                if(damagee instanceof Player) killPlayer(e.getPlayer(), (Player) damagee);
            }

            // spawn particle
            bullet.getWorld().spawnParticle(Particle.DRIP_LAVA, bullet, 1, 0, 0, 0, 0, null, true);
        }
    }
    // @EventHandler
    // public void onDamagedByPlayer(EntityDamageByEntityEvent e) {
    //     if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
    //         final Player damager = (Player) e.getDamager();
    //         final Player damagee = (Player) e.getEntity();

    //         if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
    //             // don't do anything for nonlethal
    //             if (damagee.getHealth() - e.getFinalDamage() > 0) {
    //                 e.setCancelled(false);
    //                 return;
    //             }

    //             e.setCancelled(true);

    //             killPlayer(damager, damagee);
    //         }
    //     } else if (e.getDamager() instanceof Arrow && e.getEntity() instanceof Player) {
    //         final Arrow arrow = (Arrow) e.getDamager();
    //         if(arrow.getShooter() instanceof Player) {
    //             final Player damagee = (Player) e.getEntity();
    //             final Player damager = (Player) arrow.getShooter();
    //             if(MinigameManager.getGameState().equals(MinigameManager.GameState.INGAME) && MinigameManager.isInGame(damager) && MinigameManager.isInGame(damagee)) {
    //                 e.setCancelled(true);
    //                 killPlayer(damager, damagee);
    //             }
    
    //         }

    //     }
    // }

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
        damager.playSound(damager.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 0.5f);

        // respawn
        damagee.teleport(getRandomSpawn());
        ChatSender.tell(damager, "You fragged " + damagee.getDisplayName());
        ChatSender.tell(damagee, "You were fragged by " + damager.getDisplayName());

        if(kills.getScore(damager) >= killsToWin) {
            MinigameManager.gameComplete(kills.getWinners());
        }
    }
}