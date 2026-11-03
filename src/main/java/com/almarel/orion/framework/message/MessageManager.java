package com.almarel.orion.framework.message;

import com.google.inject.Inject;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageManager {

    private static final String PREFIX = "§8[§b1b1v§8] §r";

    private final JavaPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, List<String>> listMessages = new HashMap<>();
    private FileConfiguration langConfig;

    private static final String[] RESOURCE_FILES = {
            "lang/chat.yml",
            "lang/guicommon.yml",
            "lang/guibrushlist.yml",
            "lang/guiconstruct.yml",
            "lang/guiclipboard.yml",
            "lang/guibrush.yml",
            "lang/guicommand.yml"
    };

    @Inject
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        messages.clear();
        listMessages.clear();

        langConfig = new YamlConfiguration();

        for (String path : RESOURCE_FILES) {
            YamlConfiguration internalConf = loadDefaultConfig(path);
            if (internalConf == null) {
                plugin.getLogger().warning("Could not find internal resource: " + path);
                continue;
            }

            parseConfig(internalConf);

            for (String key : internalConf.getKeys(true)) {
                Object value = internalConf.get(key);
                if (!(value instanceof ConfigurationSection)) {
                    langConfig.set(key, value);
                }
            }
        }
    }

    public FileConfiguration getConfig() {
        return langConfig;
    }

    private YamlConfiguration loadDefaultConfig(String path) {
        InputStream stream = plugin.getResource(path);
        if (stream == null) return null;

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load default resource " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void parseConfig(FileConfiguration conf) {
        for (String key : conf.getKeys(true)) {
            if (conf.isString(key)) {
                messages.put(key, color(conf.getString(key)));
            } else if (conf.isList(key)) {
                List<String> rawList = conf.getStringList(key);
                List<String> coloredList = new ArrayList<>();
                for (String s : rawList) coloredList.add(color(s));
                listMessages.put(key, coloredList);
            }
        }
    }

    // --- Sending Logic ---

    public void send(CommandSender sender, OrionMessage message, Object... args) {
        if (sender == null) return;
        String rawText = get(message.getKey());

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String val = args[i] == null ? "null" : String.valueOf(args[i]);
                rawText = rawText.replace("{" + i + "}", val);
            }
        }
        sender.sendMessage(colorizeByType(rawText, message.getType()));
    }

    private String colorizeByType(String text, OrionMessage.Type type) {
        String baseColor;
        boolean usePrefix = true;

        switch (type) {
            case ERROR -> baseColor = "§c";
            case SUCCESS, INFO -> baseColor = "§7";
            default -> {
                baseColor = "";
                usePrefix = false;
            }
        }

        String result = baseColor + text;
        if (usePrefix) result = PREFIX + result;
        return result;
    }

    // --- Getters ---

    public String get(String key) {
        return messages.getOrDefault(key, "§cMessage not found: " + key);
    }

    @Deprecated
    public String get(String key, Object... args) {
        String msg = get(key);
        if (args == null || args.length == 0) return msg;
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return msg;
    }

    public List<String> getList(String key) {
        return listMessages.getOrDefault(key, new ArrayList<>());
    }

    public String get(OrionMessage message) { return get(message.getKey()); }
    public List<String> getList(OrionMessage message) { return getList(message.getKey()); }

    @Deprecated
    public void sendMessage(CommandSender sender, String key) { if (sender != null) sender.sendMessage(get(key)); }
    @Deprecated
    public void sendMessage(CommandSender sender, String key, Object... args) { if (sender != null) sender.sendMessage(get(key, args)); }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String get(OrionMessage message, Object... args) {
        return get(message.getKey(), args);
    }
}