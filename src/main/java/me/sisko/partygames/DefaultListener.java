package me.sisko.partygames;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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
        //e.getPlayer().setGameMode(GameMode.SURVIVAL);
    }

    // by default, disallow block modifications
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockEvent(BlockEvent e) {
        if (e instanceof Cancellable) {
            ((Cancellable) e).setCancelled(true);
        }
    }

    // by default, disallow inventory modifications
    @EventHandler(priority = EventPriority.LOW)
    public void InventoryEvent(InventoryEvent e) {
        if (e instanceof Cancellable) {
            ((Cancellable) e).setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHunger(FoodLevelChangeEvent e) {
        if(e.getEntity() instanceof Player) {
            ((Player) e.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDamage(PlayerItemDamageEvent e) {
        e.setCancelled(true);
    }
}
