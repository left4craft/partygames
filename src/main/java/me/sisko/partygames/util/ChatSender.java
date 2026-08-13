package me.sisko.partygames.util;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import me.sisko.partygames.minigames.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ChatSender {
    public static final Component PREFIX = Component.text("PartyGames ", NamedTextColor.GREEN)
        .append(Component.text(">> ", NamedTextColor.DARK_GREEN));

    public static enum ChatSound {
        COUNTDOWN,
        START,
        REDLIGHT,
        GREENLIGHT
    };

    public static void broadcast(final String message) {
        broadcast(Component.text(message, NamedTextColor.GRAY));
    }

    public static void broadcast(final Component message) {
        Bukkit.broadcast(PREFIX.append(message));
    }

    public static void broadcast(final String message, final ChatSound sound) {
        broadcast(Component.text(message, NamedTextColor.GRAY), sound);
    }

    public static void broadcast(final Component message, final ChatSound sound) {
        broadcast(message);
        if(sound == ChatSound.COUNTDOWN) {
            Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 1f, 0.5f));
        } else if (sound == ChatSound.START) {
            Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 1f, 1f));
        } else if (sound == ChatSound.REDLIGHT) {
            Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, SoundCategory.BLOCKS, 1f, 1f));
        } else if (sound == ChatSound.GREENLIGHT) {
            Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1f, 1f));
        }
    }

    public static void broadcastMinigame(final Minigame game) {
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text(game.getName(), NamedTextColor.GREEN, TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("Map: ", NamedTextColor.AQUA)
            .append(Component.text(game.getMap(), NamedTextColor.WHITE)));
        Bukkit.broadcast(Component.text("Description: ", NamedTextColor.AQUA)
            .append(Component.text(game.getDescription(), NamedTextColor.WHITE)));
        Bukkit.broadcast(Component.empty());
    }

    public static void broadcastWinners(final Minigame game, final List<Player> winners) {
        broadcast(game.getName() + " complete!");
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Winners", NamedTextColor.GREEN)
            .decorate(TextDecoration.UNDERLINED, TextDecoration.BOLD));
        for(int i = 0; i < 3; i++) {
            Component line = Component.text((i+1) + ") ", NamedTextColor.AQUA);
            if(i < winners.size()) {
                line = line.append(winners.get(i).displayName().colorIfAbsent(NamedTextColor.WHITE));
            } else {
                line = line.append(Component.text("Nobody", NamedTextColor.WHITE));
            }
            Bukkit.broadcast(line);
        }
        Bukkit.broadcast(Component.empty());
    }

    public static void broadcastOverallWinners(final Leaderboard leaderboard) {
        broadcast("Minigame rotation complete");
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Overall Winners", NamedTextColor.GREEN)
            .decorate(TextDecoration.UNDERLINED, TextDecoration.BOLD));
        for(int i = 0; i < leaderboard.getLeaderboard().size(); i++) {
            Bukkit.broadcast(Component.text((i+1) + ") ", NamedTextColor.AQUA)
                .append(leaderboard.getLeaderboard().get(i).getPlayer().displayName()
                    .colorIfAbsent(NamedTextColor.WHITE)));
        }
        Bukkit.broadcast(Component.empty());
    }

    public static void tell(final Player player, final String message) {
        tell(player, Component.text(message, NamedTextColor.GRAY));
    }

    public static void tell(final Player player, final Component message) {
        player.sendMessage(PREFIX.append(message));
    }

    // legacy &-coded display name, for the scoreboard lines that are still
    // built as legacy strings
    public static String legacyName(final Player player) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(player.displayName());
    }
}
