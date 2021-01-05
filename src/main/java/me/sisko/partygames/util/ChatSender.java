package me.sisko.partygames.util;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.missionary.board.board.BoardEntry;
import me.sisko.partygames.minigames.Minigame;
import me.sisko.partygames.util.Leaderboard.PlayerScore;

public class ChatSender {
    public static final String PREFIX = ChatColor.GREEN + "PartyGames " + ChatColor.DARK_GREEN + ">> " + ChatColor.GRAY;
    
    public static enum ChatSound {
        COUNTDOWN,
        START
    };

    public static void broadcast(final String message) {
        Bukkit.broadcastMessage(PREFIX + message);
    }

    public static void broadcast(final String message, final ChatSound sound) {
        Bukkit.broadcastMessage(PREFIX + message);
        if(sound == ChatSound.COUNTDOWN) {
            Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 1f, 0.5f));
        } else if (sound == ChatSound.START) {
            Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 1f, 1f));
        }
    }

    public static void broadcastMinigame(final Minigame game) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.BOLD + game.getName());
        Bukkit.broadcastMessage(ChatColor.AQUA + "Map: " + ChatColor.WHITE + game.getMap());
        Bukkit.broadcastMessage(ChatColor.AQUA + "Description: " + ChatColor.WHITE + game.getDescription());
        Bukkit.broadcastMessage("");
    }

    public static void broadcastWinners(final Minigame game, final List<Player> winners) {
        broadcast(game.getName() + " complete!");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.UNDERLINE + ChatColor.BOLD + "Winners");
        for(int i = 0; i < 3; i++) {
            String line = "" + ChatColor.AQUA + (i+1) + ") " + ChatColor.WHITE;
            if(i < winners.size()) {
                line += winners.get(i).getDisplayName();
            } else {
                line += "Nobody";
            }
            Bukkit.broadcastMessage(line);
        }
        Bukkit.broadcastMessage("");
    }

    public static void broadcastOVerallWinners(final Leaderboard leaderboard) {
        broadcast("Minigame rotation complete complete");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.UNDERLINE + ChatColor.BOLD + "Overall Winners");
        for(int i = 0; i < leaderboard.getLeaderboard().size(); i++) {
            Bukkit.broadcastMessage("" + ChatColor.AQUA + (i+1) + ") " + ChatColor.WHITE + leaderboard.getLeaderboard().get(i).getPlayer().getDisplayName());
        }
        Bukkit.broadcastMessage("");
    }

    public static void tell(final Player player, final String message) {
        player.sendMessage(PREFIX + message);
    }
}
