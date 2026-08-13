package me.sisko.partygames.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.sisko.partygames.util.MinigameManager.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ScoreboardProvider {

    private static float hue = 0f;
    private static final float dashHueDiff = 0.01f;

    public Component getTitle(Player player) {
        return Component.text("Party Games", NamedTextColor.GREEN, TextDecoration.BOLD);
    }

    public static void incrementRainbow() {
        // increment the hue and wrap around to keep magnitude near 1
        // so it doesn't slowly overflow / loose percision
        hue += dashHueDiff;
        hue -= (float) Math.floor(hue);
    }

    public List<Component> getLines(Player player) {
        List<String> lines = new ArrayList<>();

        if(!MinigameManager.getGameState().equals(GameState.NOGAME)) {
            lines.addAll(MinigameManager.getScoreboardLines(player));
        }

        lines.addAll(MinigameRotator.getScoreboardLines(player));

        // the game code builds its lines as legacy &-coded strings;
        // deserialize them here, at the display boundary
        List<Component> components = new ArrayList<>();
        components.add(getRainbowDashes(25));
        for (String line : lines) {
            components.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
        components.add(getRainbowDashes(25));
        return components;
    }

    private Component getRainbowDashes(int length) {
        var rainbow = Component.text();

        for(int i = 0; i < length; i++) {
            rainbow.append(Component.text("-",
                TextColor.color(Color.HSBtoRGB(hue - ((float)i)*dashHueDiff, 0.95f, 0.95f) & 0xFFFFFF),
                TextDecoration.STRIKETHROUGH));
        }

        return rainbow.build();
    }
}
