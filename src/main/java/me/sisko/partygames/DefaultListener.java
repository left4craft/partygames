package me.sisko.partygames;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.sisko.partygames.util.Database;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.MinigameRotator;
import net.md_5.bungee.api.ChatColor;

/*
This class listens for events that happen in the lobby
and provide reasonable defaults for minigames. The priority
is low so that their behavior can be overriden by a regular
priority listener in the minigame itself.
*/
public class DefaultListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        // Scoreboard sc = e.getPlayer().getScoreboard();
        // if(sc.getTeam("noCollide") == null) {
        //     sc.registerNewTeam("noCollide");
        //     Team setup = sc.getTeam("noCollide");
        //     setup.setAllowFriendlyFire(true);
        //     setup.setCanSeeFriendlyInvisibles(true);
        //     setup.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        // }
        // Team team = sc.getTeam("noCollide");
        // team.addEntry(e.getPlayer().getName());
        
        // ensure player is cached in db
        Database.getOverallGames(e.getPlayer());

        MinigameRotator.onJoin(e.getPlayer());

        if(!MinigameManager.addPlayer(e.getPlayer())) {
            e.getPlayer().sendMessage(ChatColor.GREEN + "Welcome to party games!");
            e.getPlayer().setGameMode(GameMode.SURVIVAL);
            e.getPlayer().getInventory().clear();
            e.getPlayer().setGlowing(false);
            e.getPlayer().setAllowFlight(false);
            e.getPlayer().setCollidable(true);
            e.getPlayer().getActivePotionEffects().forEach(effect -> e.getPlayer().removePotionEffect(effect.getType()));

            FileConfiguration config = Main.getPlugin().getConfig();
            e.getPlayer().teleport(new Location(Main.getWorld(), config.getDouble("spawn.x"), 
                config.getDouble("spawn.y"), config.getDouble("spawn.z"), (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeave(PlayerQuitEvent e) {
        MinigameRotator.onLeave(e.getPlayer());

        MinigameManager.removePlayer(e.getPlayer());

        Database.saveToDb(e.getPlayer());
    }


    // by default, disallow block modifications
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent e) {
        if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent e) {
        if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
    }

    
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL && (e.getClickedBlock().getType().equals(Material.DIRT) || e.getClickedBlock().getType().equals(Material.FARMLAND))) {
			e.setCancelled(true);
        }
        
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
        }
	}

    // by default, disallow inventory modifications
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        if(!e.getWhoClicked().getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHunger(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
    }

    // prevent armor stand stuff
    @EventHandler(priority =  EventPriority.LOW)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
    }

    // protect paintings and item frames
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent  e) {
        if(e.getDamager() instanceof Player) {
            if(!((Player) e.getDamager()).getGameMode().equals(GameMode.CREATIVE)) e.setCancelled(true);
        }
    }

    // cancel out fall damage, etc
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    // disable arrow landing on blocks
    @EventHandler(priority = EventPriority.LOW)
    public void onProjectileHit(ProjectileHitEvent e) {
        e.getEntity().remove();
    }

    // disable arrow picking up
    @EventHandler(priority = EventPriority.LOW)
    public void onProjectilePickup(PlayerPickupArrowEvent e) {
        e.getArrow().remove();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDamage(PlayerItemDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVoidVall(PlayerMoveEvent e) {
        if(e.getTo().getY() < 0) {
            FileConfiguration config = Main.getPlugin().getConfig();
            e.getPlayer().teleport(new Location(Main.getPlugin().getServer().getWorld("world"), config.getDouble("spawn.x"), 
                config.getDouble("spawn.y"), config.getDouble("spawn.z"), (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")));
    
        }
    }
}
