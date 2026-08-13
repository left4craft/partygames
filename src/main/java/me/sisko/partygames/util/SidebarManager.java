package me.sisko.partygames.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;

/*
Per-player sidebar scoreboards. Replaces the old me.missionary.board library,
which was compiled against the 1.16 API and split lines into 16-character
team prefix/suffix pairs; modern team prefixes are Components with no length
limit, and the score numbers on the right are hidden with a blank NumberFormat.

Each line is an invisible unique entry (a lone legacy colour code) whose
team prefix carries the visible text.
*/
public class SidebarManager implements Listener {

    private static final int MAX_LINES = 15;

    // one invisible, unique scoreboard entry per line
    private static final String[] ENTRIES = new String[MAX_LINES];
    static {
        for (int i = 0; i < MAX_LINES; i++) {
            ENTRIES[i] = "§" + Character.forDigit(i, 16) + "§r";
        }
    }

    private final ScoreboardProvider provider;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public SidebarManager(ScoreboardProvider provider) {
        this.provider = provider;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("partygames", Criteria.DUMMY,
            provider.getTitle(p));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        for (int i = 0; i < MAX_LINES; i++) {
            board.registerNewTeam("line" + i).addEntry(ENTRIES[i]);
        }

        boards.put(p.getUniqueId(), board);
        p.setScoreboard(board);
        update(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
    }

    public void update() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }

    private void update(Player p) {
        final Scoreboard board = boards.get(p.getUniqueId());
        if (board == null) return;

        final Objective objective = board.getObjective("partygames");
        objective.displayName(provider.getTitle(p));

        final List<Component> lines = provider.getLines(p);
        for (int i = 0; i < MAX_LINES; i++) {
            if (i < lines.size()) {
                Team team = board.getTeam("line" + i);
                team.prefix(lines.get(i));
                objective.getScore(ENTRIES[i]).setScore(lines.size() - i);
            } else {
                board.resetScores(ENTRIES[i]);
            }
        }
    }

    public void onDisable() {
        final Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(main);
        }
        boards.clear();
    }
}
