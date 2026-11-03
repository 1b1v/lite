package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.stream.Collectors;

import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class BrushListGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.brush-list.";

    private final Status status = new Status();
    private final Headers headers = new Headers();
    private final Items items = new Items();
    private final Buttons buttons = new Buttons();
    private final Errors errors = new Errors();

    public BrushListGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    public String getTitle() { return get("title", TITLE); }

    public Status status() { return status; }
    public Headers headers() { return headers; }
    public Items items() { return items; }
    public Buttons buttons() { return buttons; }
    public Errors errors() { return errors; }

    // --- Inner Classes ---

    public class Status {
        public String getDefault() { return get("status.default", "§e"); }
        public String getTemp() { return get("status.temp", "§e"); }
        public String getPerm() { return get("status.perm", "§6"); }
        public String getBuffered() { return get("status.buffered", "§c"); }
    }

    public class Headers {
        public String getCp() { return get("headers.cp", HEADER_AQUA); }
        public String getCopy() { return get("headers.copy", HEADER_AQUA); }
        public String getCrystal() { return get("headers.crystal", HEADER_AQUA); }
        public String getPlump() { return get("headers.plump", HEADER_PINK); }
        public String getEditor() { return get("headers.editor", HEADER_PINK); }
        public String getGen() { return get("headers.gen", HEADER_PURPLE); }
    }

    public class Buttons {
        public String getBufferName() { return get("buttons.buffer", BTN_ACT); }
        public List<String> getBufferLore() { return getList("buttons.buffer-lore", LORE_TEXT); }

        public String getBufferMainName() { return get("buttons.buffer-main", BTN_ACT); }
        public List<String> getBufferMainLore() { return getList("buttons.buffer-main-lore", LORE_TEXT); }

        public String getListName() { return get("buttons.list", BTN_ACT); }
        public String getListLore() { return get("buttons.list-lore", LORE_TEXT); }

        public String getSettingsName() { return get("buttons.settings", BTN_SET); }
        public List<String> getSettingsLore() { return getList("buttons.settings-lore", LORE_TEXT); }

        public String getMacroName() { return get("buttons.macro", BTN_MACRO); }
        public List<String> getMacroLore() { return getList("buttons.macro-lore", LORE_TEXT); }
    }

    public class Errors {
        public String getDevMode() { return get("errors.dev-mode", ERROR); }
        public String getLimitBuffer() { return get("errors.limit-buffer", ERROR); }
    }

    public class Items {
        private final String BASE = "items.lore.";

        public String getLoreSchemsCount(String count) {
            return "§e" + getRaw(BASE + "schems-count") + " " + VAL_HIGH + count;
        }

        // [NEW] Унифицированные контроли
        public String getLoreEquip() { return get(BASE + "equip", LORE_TEXT); }
        public String getLoreDelete() { return get(BASE + "delete", LORE_TEXT); }
        public String getLoreMakePerm() { return get(BASE + "make-perm", LORE_TEXT); }
        public String getLoreMakeBuffer() { return get(BASE + "make-buffer", LORE_TEXT); }
        public String getLoreMakeTemp() { return get(BASE + "make-temp", LORE_TEXT); }
        public String getLoreSettings() { return get(BASE + "settings", LORE_TEXT); }
        public String getLoreExpandView() { return get(BASE + "expand-view", LORE_TEXT); }
        public String getLoreCollapseView() { return get(BASE + "collapse-view", LORE_TEXT); }

        public String getLoreFlags(String flags) { return format(BASE + "flags", LORE_META, flags); }
        public String getLoreScrollMode(String status) { return format(BASE + "scroll-mode", LORE_META, status); }
        public String getLoreAnchor(String anchor) { return format(BASE + "anchor", LORE_META, anchor); }
        public String getLoreRange(String range) { return format(BASE + "range", LORE_META, range); }
        public String getLoreFormula(String formula) { return format(BASE + "formula", LORE_META, formula); }
        public String getLoreFormulaFull() { return get(BASE + "formula-full", LORE_META); }

        public String getLorePattern(String pattern) { return format(BASE + "pattern", LORE_TEXT, pattern); }
        public String getLoreRadius(String radius) { return format(BASE + "radius", LORE_TEXT, radius); }
        public String getLoreGrain(String grain) { return format(BASE + "grain", LORE_TEXT, grain); }
        public String getLoreEditorMode() { return get(BASE + "editor-mode", LORE_TEXT); }
        public String getLorePointsCount(String count) { return format(BASE + "points-count", LORE_TEXT, count); }

        public String getLoreConstructsOn(String active) {
            String raw = getRaw(BASE + "constructs-on");
            return LORE_TEXT + raw.replace("ВКЛ", VAL_ON + "ВКЛ" + LORE_TEXT).replace("{0}", LORE_VAL + active + LORE_TEXT);
        }

        public String getLoreConstructsOff() {
            return LORE_TEXT + getRaw(BASE + "constructs-off").replace("ВЫКЛ", VAL_OFF + "ВЫКЛ");
        }
    }

    // --- Helpers ---

    private String getRaw(String path) {
        return config.getString(ROOT + path, "Missing: " + path);
    }

    private String get(String path, String colorPrefix) {
        String val = config.getString(ROOT + path);
        return val != null ? colorPrefix + val : "§cMissing: " + path;
    }

    private List<String> getList(String path, String colorPrefix) {
        return config.getStringList(ROOT + path).stream()
                .map(s -> colorPrefix + s)
                .collect(Collectors.toList());
    }

    private String format(String path, String prefix, String arg0) {
        String valColor = (prefix.equals(LORE_META)) ? "§7" : LORE_VAL;
        if (prefix.equals(LORE_META) && path.contains("flags")) valColor = "§7";
        return get(path, prefix).replace("{0}", valColor + arg0);
    }

    @Override
    public void updateSource(FileConfiguration newSource) {
        this.config = newSource;
    }
}