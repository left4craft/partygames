package me.sisko.partygames.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.missionary.board.provider.BoardProvider;
import net.md_5.bungee.api.ChatColor;

public class ScoreboardProvider implements BoardProvider {
    
    @Override
    public String getTitle(Player player) {
        return ChatColor.LIGHT_PURPLE + "Party Games";
    }

    @Override
    public List<String> getLines(Player player) {
        if(MinigameManager.inGame()) {
            return MinigameManager.getScoreboardLines(player);
        }
        List<String> lines = new ArrayList<>();
        lines.add("&6mc.left4craft.org");
        lines.add("&7&m-----------------");
        lines.add(ChatColor.LIGHT_PURPLE + "Name" + ChatColor.GRAY + ": " + ChatColor.YELLOW + player.getName());
        lines.add("");
        lines.add(ChatColor.LIGHT_PURPLE + "Wins" + ChatColor.GRAY + ": " + ChatColor.YELLOW + "5");
        lines.add("");
        lines.add(ChatColor.LIGHT_PURPLE + "Points" + ChatColor.GRAY + ": " + ChatColor.YELLOW + "432");
        lines.add("");
        lines.add("&7&m-----------------");
        return lines;
    }

}
