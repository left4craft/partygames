package me.sisko.partygames.util;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;

import me.sisko.partygames.Main;

public class ConfigManager {
    public static void load() {
        FileConfiguration config = Main.getPlugin().getConfig();
        File dataFolder = Main.getPlugin().getDataFolder();

        config.addDefault("sql.url", "jdbc:mysql://ip:port/database");
        config.addDefault("sql.user", "user");
        config.addDefault("sql.pass", "password");

        config.addDefault("spawn.x", 0.5);
        config.addDefault("spawn.y", 151.);
        config.addDefault("spawn.z", 0.5);
        config.addDefault("spawn.yaw", 0.);
        config.addDefault("spawn.pitch", 0.);


        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!new File(dataFolder, "config.yml").exists()) {
            Main.getPlugin().getLogger().info("Config.yml not found, creating!");
            config.options().copyDefaults(true);
            Main.getPlugin().saveConfig();
        } else {
            Main.getPlugin().getLogger().info("Config.yml found, loading!");
            config.options().copyDefaults(false);
        }

        reload();
    }

    public static void reload() {
        Main.getPlugin().reloadConfig();
    }
}
