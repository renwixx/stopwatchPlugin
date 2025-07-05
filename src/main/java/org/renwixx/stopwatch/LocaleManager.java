package org.renwixx.stopwatch;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LocaleManager {

    private final StopwatchPlugin plugin;
    private FileConfiguration messagesConfig = null;

    public LocaleManager(StopwatchPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        String locale = plugin.getConfig().getString("locale", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file 'lang/" + locale + ".yml' not found. Creating from defaults...");
            plugin.saveResource("lang/" + locale + ".yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource("lang/" + locale + ".yml");
        if (defaultStream != null)
            messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream)));
    }

    public String getString(String key) {
        String message = messagesConfig.getString(key);
        if (message == null)
            return ChatColor.RED + "Missing translation for key: " + key;
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getString(String key, String... placeholders) {
        String message = getString(key);
        if (placeholders.length % 2 != 0)
            return message;
        for (int i = 0; i < placeholders.length; i += 2)
            message = message.replace(placeholders[i], placeholders[i + 1]);
        return message;
    }
}