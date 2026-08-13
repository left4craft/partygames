package me.sisko.partygames.commands;

import java.util.Collection;
import java.util.List;

import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.MinigameRotator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();
        if(args.length < 1) {
            sender.sendMessage(Component.text("Usage: /play <game>", NamedTextColor.RED));
        } else if(MinigameManager.isValidType(args[0])) {
            MinigameRotator.forceStartRotation(List.of(args[0]));
        } else {
            sender.sendMessage(Component.text(args[0] + " is not a valid game!", NamedTextColor.RED));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if(args.length > 1) return List.of();
        final String prefix = args.length == 0 ? "" : args[0].toLowerCase();
        return MinigameManager.getTypes().stream().filter(type -> type.startsWith(prefix)).toList();
    }

    @Override
    public String permission() {
        return "partygames.play";
    }
}
