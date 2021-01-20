package me.sisko.partygames.util;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.sisko.partygames.Main;

public class XpBarFill extends BukkitRunnable {
    private int currLevel;
    private int finalLevel;
    private Player player;

    public XpBarFill(int finalLevel, Player player) {
        this.currLevel = 0;
        this.finalLevel = finalLevel;
        this.player = player;
    }
    
    private XpBarFill(int currLevel, int finalLevel, Player player) {
        this.currLevel = currLevel;
        this.finalLevel = finalLevel;
        this.player = player;
    }

    @Override
    public void run() {
        player.setExp(((float) currLevel) / finalLevel);
        if(currLevel < finalLevel) {
            new XpBarFill(currLevel+1, finalLevel, player).runTaskLater(Main.getPlugin(), 1);
        }
    }
}
