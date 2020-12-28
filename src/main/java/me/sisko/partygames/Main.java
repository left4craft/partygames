package me.sisko.partygames;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main plugin;
    
    @Override
    public void onEnable() {
        plugin = this;
        getLogger().info("Hello World!");
        Bukkit.getPluginManager().registerEvents(new DefaultListener(), this);
    }

    public static Main getPlugin() {
        return plugin;
    }
}