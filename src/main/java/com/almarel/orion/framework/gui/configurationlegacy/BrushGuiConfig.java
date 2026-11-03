package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class BrushGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.brush-gui.";

    public BrushGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    public String getFallbackTitle() {
        return get(ROOT + "fallback-title", TITLE);
    }

    // Заменили цвет суффикса на желтый (BTN_SET)
    public String getSuffixHand() {
        return " " + get(ROOT + "suffix-hand", BTN_SET);
    }

    // Заменили цвет суффикса на желтый (BTN_SET)
    public String getSuffixBuffer(String index) {
        return " " + format(ROOT + "suffix-buffer", BTN_SET, index);
    }

    // --- Helpers ---
    private String get(String path, String colorPrefix) {
        String val = config.getString(path);
        return val != null ? TextFormatter.process(val, colorPrefix) : "§cMissing: " + path;
    }

    private String format(String path, String prefix, String arg0) {
        return get(path, prefix).replace("{0}", LORE_VAL + arg0 + prefix);
    }

    @Override
    public void updateSource(FileConfiguration newSource) {
        this.config = newSource;
    }
}