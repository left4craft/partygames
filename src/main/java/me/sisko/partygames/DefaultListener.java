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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

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
        e.getPlayer().sendMessage(ChatColor.GREEN + "Welcome to party games!");
        e.getPlayer().setGameMode(GameMode.SURVIVAL);
        e.getPlayer().getInventory().clear();
        FileConfiguration config = Main.getPlugin().getConfig();
        e.getPlayer().teleport(new Location(Main.getPlugin().getServer().getWorld("world"), config.getDouble("spawn.x"), 
            config.getDouble("spawn.y"), config.getDouble("spawn.z"), (float) config.getDouble("spawn.yaw"),
            (float) config.getDouble("spawn.pitch")));
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
	}

    // by default, disallow inventory modifications
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHunger(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
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
