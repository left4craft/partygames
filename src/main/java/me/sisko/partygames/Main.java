package me.sisko.partygames;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.sisko.partygames.commands.PlayCommand;
import me.sisko.partygames.util.ConfigManager;
import me.sisko.partygames.util.Database;
import me.sisko.partygames.util.MinigameManager;
import me.sisko.partygames.util.ScorePlaceholder;
import me.sisko.partygames.util.ScoreboardProvider;
import me.sisko.partygames.util.SidebarManager;

public class Main extends JavaPlugin {
    private static Main plugin;
    private SidebarManager sidebar;

    @Override
    public void onEnable() {
        plugin = this;

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register("play", "Force-start a specific minigame", new PlayCommand()));

        // Small check to make sure that PlaceholderAPI is installed
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new ScorePlaceholder().register();
        }

        sidebar = new SidebarManager(new ScoreboardProvider());
        // auto increment rainbow and redraw the sidebars
        new BukkitRunnable(){
            @Override
            public void run() {
                ScoreboardProvider.incrementRainbow();
                sidebar.update();
            }

        }.runTaskTimer(this, 0, 1);

        Bukkit.getPluginManager().registerEvents(sidebar, this);
        Bukkit.getPluginManager().registerEvents(new DefaultListener(), this);
        ConfigManager.load();
        MinigameManager.load();
        Database.connect();

    }

    @Override
    public void onDisable() {
        sidebar.onDisable();
    }

    public static Main getPlugin() {
        return plugin;
    }

    public static World getWorld() {
        return Main.getPlugin().getServer().getWorld("world");
    }
}
