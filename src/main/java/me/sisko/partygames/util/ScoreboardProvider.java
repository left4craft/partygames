package me.sisko.partygames.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.missionary.board.provider.BoardProvider;
import me.sisko.partygames.util.MinigameManager.GameState;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class ScoreboardProvider implements BoardProvider {
    
    private static float hue = 0f;
    private static final float dashHueDiff = 0.01f;

    @Override
    public String getTitle(Player player) {
        return "&a&lParty Games";
    }

    public static void incrementRainbow() {
        // increment the hue and wrap around to keep magnitude near 1
        // so it doesn't slowly overflow / loose percision
        hue += dashHueDiff;
        hue -= Math.floor(hue);
    }

    @Override
    public List<String> getLines(Player player) {
        List<String> lines = new ArrayList<>();
        //lines.add("&amc.left4craft.org");
        lines.add(getRainbowDashes(25));

        if(!MinigameManager.getGameState().equals(GameState.NOGAME)) {
            lines.addAll(MinigameManager.getScoreboardLines(player));
        } 
        
        lines.addAll(MinigameRotator.getScoreboardLines(player));

        lines.add(getRainbowDashes(25));
        return lines;
    }

    private String getRainbowDashes(int length) {
        TextComponent rainbow = new TextComponent();

        for(int i = 0; i < length; i++) {
            TextComponent dash = new TextComponent();
            dash.setColor(ChatColor.of(Color.getHSBColor(hue - ((float)i)*dashHueDiff, 0.95f, 0.95f)));
            dash.setStrikethrough(true);
            dash.setText("-");
            rainbow.addExtra(dash);
        }

        return rainbow.toLegacyText();
    }

}
