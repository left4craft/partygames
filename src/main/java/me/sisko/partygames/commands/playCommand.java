package me.sisko.partygames.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.sisko.partygames.util.MinigameManager;

public class playCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission("partygames.play")) {
            if(args.length < 1) {
                sender.sendMessage("Usage: /play <name>");
            } else {
                if(MinigameManager.isValidType(args[0])) {
                    MinigameManager.playGame(args[0]);
                } else {
                    sender.sendMessage(args[0] + " is not a valid game!");
                }
            }
        }
        return true;
    }
    
}
