package me.uniodex.HydroHomie.data;

import me.uniodex.HydroHomie.HydroHomie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    public static ConfigManager instance;
    private final HydroHomie plugin;

    private final Map<Config, FileConfiguration> configurations = new HashMap<>();


    public ConfigManager(HydroHomie plugin) {
        instance = this;
        this.plugin = plugin;

        plugin.saveDefaultConfig();

        for (Config config : Config.values()) {
            registerConfig(config);
        }

        for (Config config : configurations.keySet()) {
            reloadConfig(config);
            configurations.get(config).options().copyDefaults(true);
            saveConfig(config);
        }
    }

    public FileConfiguration getConfig(Config config) {
        return configurations.get(config);
    }

    private void registerConfig(Config config) {
        configurations.put(config, YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), getFileName(config))));
    }

    public void reloadConfig(Config config) {
        String fileName = getFileName(config);

        InputStream inputStream = plugin.getResource(fileName);
        if (inputStream != null) {
            InputStreamReader reader = new InputStreamReader(inputStream);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            configurations.get(config).setDefaults(defConfig);
            try {
                reader.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveConfig(Config config) {
        String fileName = getFileName(config);

        try {
            configurations.get(config).save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Couldn't save " + fileName + "!");
        }
    }

    private String getFileName(Config config) {
        return config.toString().toLowerCase() + ".yml";
    }

    public enum Config {
        DATA
    }
}