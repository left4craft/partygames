package me.sisko.partygames;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import me.missionary.board.BoardManager;
import me.missionary.board.settings.BoardSettings;
import me.missionary.board.settings.ScoreDirection;
import me.sisko.partygames.commands.playCommand;
import me.sisko.partygames.util.ConfigManager;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.ScoreboardProvider;

public class Main extends JavaPlugin {
    private static Main plugin;
    private BoardManager manager;

    @Override
    public void onEnable() {
        plugin = this;
        getLogger().info("Hello World!");
        getCommand("play").setExecutor(new playCommand());
        
        manager = new BoardManager(this, new BoardSettings(new ScoreboardProvider(), ScoreDirection.UP));
        Bukkit.getPluginManager().registerEvents(manager, this);
        Bukkit.getPluginManager().registerEvents(new DefaultListener(), this);
        ConfigManager.load();
        MinigameManager.load();

    }

    @Override
    public void onDisable() {
        manager.onDisable();
    }

    public static Main getPlugin() {
        return plugin;
    }

    public static World getWorld() {
        return Main.getPlugin().getServer().getWorld("world");
    }
}