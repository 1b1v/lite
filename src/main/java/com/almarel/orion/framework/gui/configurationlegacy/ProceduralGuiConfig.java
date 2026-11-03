package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class ProceduralGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.brush-pro.";

    private final Titles titles = new Titles();
    private final Info info = new Info();
    private final Constructs constructs = new Constructs();
    private final Buttons buttons = new Buttons();

    public ProceduralGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    public Titles titles() { return titles; }
    public Info info() { return info; }
    public Constructs constructs() { return constructs; }
    public Buttons buttons() { return buttons; }

    // --- Sections ---

    public class Titles {
        private final String BASE = ROOT + "titles.";
        public String getPlump() { return get(BASE + "plump", HEADER_PINK); }
        public String getGen() { return get(BASE + "gen", HEADER_PURPLE); }
        public String getCrystal() { return get(BASE + "crystal", HEADER_AQUA); }
    }

    public class Info {
        private final String BASE = ROOT + "info.";
        public String getPattern(String val) { return format(BASE + "pattern", LORE_META, val); }
        // Radius удален -> используем commonConfig.pro().getRadius(val)
        public String getFlags(String val) { return format(BASE + "flags", LORE_META, val); }
        public String getRange(String val) { return format(BASE + "range", LORE_META, val); }
        public String getFormula(String val) { return format(BASE + "formula", LORE_META, val); }
        public String getFormulaFull() { return get(BASE + "formula-full", LORE_META); }
        public String getResetHint() { return get(BASE + "reset-hint", ERROR); }

        public String getModePlump() { return get(BASE + "mode-plump", LORE_META); }
        public String getModeCrystal() { return get(BASE + "mode-crystal", LORE_META); }
    }

    public class Constructs {
        private final String BASE = ROOT + "constructs.";
        public String getName() { return get(BASE + "name", VAL_HIGH); }
        public String getLoreMode(String val) { return format(BASE + "lore-mode", LORE_TEXT, val); }
        public String getLoreActive(String val) { return format(BASE + "lore-active", LORE_TEXT, val); }
        public String getLoreHint() { return get(BASE + "lore-hint", BTN_SET); }
    }

    public class Buttons {
        private final String BASE = ROOT + "buttons.";
        // Rotation и Hollow удалены -> используем commonConfig.pro()

        // Seed оставляем (уникален для Gen/Pro)
        public String getSeed(String val) { return format(BASE + "seed", BTN_SET, val); }
        public String getSeedLore() { return get(BASE + "seed-lore", LORE_TEXT); }
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