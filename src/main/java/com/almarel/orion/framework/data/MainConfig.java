package com.almarel.orion.framework.data;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import lombok.AccessLevel;

import java.util.Locale;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Класс-контейнер для всех настроек плагина.
 * Поддерживает горячую перезагрузку через метод load().
 */
@Getter
public class MainConfig {

    @Getter(AccessLevel.NONE)
    private final JavaPlugin plugin;

    private int limitsMaxGroupLoadSize;

    private int profileTtlDays;

    private boolean showPasteMessage;
    private final Map<Material, String> defaultBrushes = new HashMap<>();

    public MainConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<Material, String> getDefaultBrushes() {
        return Collections.unmodifiableMap(defaultBrushes);
    }

    /**
     * Перезагружает конфигурацию с диска и обновляет поля.
     */
    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        try {
            // 1. Читаем простые настройки
            loadCoreSettings(config);

            // 3. Читаем маппинг дефолтных кистей
            loadDefaultBrushes(config);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load config!", e);
        }
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Parsing)
    // ==========================================

    private void loadCoreSettings(FileConfiguration config) {
        this.limitsMaxGroupLoadSize = config.getInt("limits.max-group-load-size", 50); // [NEW] Fail-Fast Limit
        this.profileTtlDays = config.getInt("profile-ttl-days", 180);
        this.showPasteMessage = config.getBoolean("show-paste-message", false);
    }

    private void loadDefaultBrushes(FileConfiguration config) {
        this.defaultBrushes.clear();
        ConfigurationSection section = config.getConfigurationSection("default-brushes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material mat = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                if (mat != null) {
                    this.defaultBrushes.put(mat, section.getString(key));
                } else {
                    plugin.getLogger().warning("[1b1v] Invalid material in default-brushes: '" + key + "'");
                }
            }
        }
    }
}