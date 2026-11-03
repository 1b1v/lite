package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.stream.Collectors;
import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class ConstructGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.construct.";

    private final ClassicMode classicMode = new ClassicMode();
    private final GroupItem groupItem = new GroupItem();
    private final EditorPanel editorPanel = new EditorPanel();
    private final Buttons buttons = new Buttons();
    private final Controls controls = new Controls();
    private final Info info = new Info();
    private final GroupEditor groupEditor = new GroupEditor();

    public ConstructGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    // =================================================================================
    // [NEW] Hybrid System API
    // =================================================================================

    public String getText(String relativePath) {
        return get(ROOT + relativePath, LORE_TEXT);
    }

    public String getText(String relativePath, Object... args) {
        String val = getText(relativePath);
        for (int i = 0; i < args.length; i++) {
            val = val.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return val;
    }

    public List<String> getTextList(String relativePath) {
        return getList(ROOT + relativePath, LORE_TEXT);
    }

    public String getName(String relativePath) {
        return get(ROOT + relativePath, BTN_SET);
    }

    public String getName(String relativePath, Object... args) {
        String val = getName(relativePath);
        for (int i = 0; i < args.length; i++) {
            val = val.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return val;
    }

    public String getColored(String relativePath, String colorPrefix, Object... args) {
        String val = get(ROOT + relativePath, colorPrefix);
        for (int i = 0; i < args.length; i++) {
            val = val.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return val;
    }

    // =================================================================================
    // [LEGACY] Getters for Inner Classes
    // =================================================================================

    public String getTitle(String suffix) { return get(ROOT + "title", VAL_HIGH) + suffix; }
    public String getSuffixEditor() { return get(ROOT + "suffix-editor", LORE_TEXT); }
    public String getSuffixTool(String name) { return get(ROOT + "suffix-tool", LORE_TEXT).replace("{0}", name); }

    public ClassicMode classicMode() { return classicMode; }
    public GroupItem groupItem() { return groupItem; }
    public EditorPanel editorPanel() { return editorPanel; }
    public Buttons buttons() { return buttons; }
    public Controls controls() { return controls; }
    public Info info() { return info; }
    public GroupEditor groupEditor() { return groupEditor; }

    // --- Sections ---

    public class ClassicMode {
        private final String BASE = ROOT + "classic-mode.";
        public String getName() { return get(BASE + "name", BTN_SET); }
        public List<String> getLore(String status) { return formatList(BASE + "lore", LORE_TEXT, status); }
        public String getStatusActive() { return get(BASE + "status-active", VAL_ON); }
        public String getStatusInactive() { return get(BASE + "status-inactive", LORE_TEXT); }
    }

    public class GroupItem {
        private final String BASE = ROOT + "group-item.";
        public String getLoreCount(String count) { return format(BASE + "lore-count", LORE_TEXT, count); }
        public String getStatusMulti() { return get(BASE + "status-multi", BTN_MACRO); }
        public String getStatusActive() { return get(BASE + "status-active", VAL_ON); }
        public String getStatusInactive() { return get(BASE + "status-inactive", LORE_TEXT); }
        public String getStatusPrefix(String status) { return format(BASE + "status-prefix", LORE_TEXT, status); }
        public String getLoreSelect() { return get(BASE + "lore-select", BTN_SET); }
        public String getLoreNoBrush() { return get(BASE + "lore-no-brush", LORE_TEXT); }
    }

    public class EditorPanel {
        private final String BASE = ROOT + "editor-panel.";
        public String getSaveName() { return get(BASE + "save-name", BTN_ACT); }
        public List<String> getSaveLore(String count) { return formatList(BASE + "save-lore", LORE_TEXT, count); }
        public String getClearName() { return get(BASE + "clear-name", ERROR); }
        public String getClearLore() { return get(BASE + "clear-lore", LORE_TEXT); }
        public String getEmptyName() { return get(BASE + "empty-name", LORE_TEXT); }
        public List<String> getEmptyLore() { return getList(BASE + "empty-lore", LORE_TEXT); }
    }

    public class Buttons {
        private final String BASE = ROOT + "buttons.";
        public String getEditorMode() { return get(BASE + "editor-mode", ERROR); }
        public String getEditorModeLore() { return get(BASE + "editor-mode-lore", LORE_TEXT); }
    }

    public class Controls {
        private final String BASE = ROOT + "controls.";
        public String getSeedOn() { return get(BASE + "seed-on", VAL_ON); }
        public String getSeedOff() { return get(BASE + "seed-off", VAL_OFF); }
        public String getSeedLoreOn() { return get(BASE + "seed-lore-on", LORE_TEXT); }
        public String getSeedLoreOff() { return get(BASE + "seed-lore-off", LORE_TEXT); }
        public List<String> getScaleLore() { return getList(BASE + "scale-lore", LORE_TEXT); }
    }

    public class Info {
        private final String BASE = ROOT + "info.";
        public String getTitle() { return get(BASE + "title", BTN_SET); }
        public List<String> getLore() { return getList(BASE + "lore", LORE_TEXT); }
    }

    public class GroupEditor {
        private final String BASE = ROOT + "group-editor.";

        public String getTitle(String groupName) {
            return get(BASE + "title", ChatColor.DARK_AQUA.toString() + ChatColor.BOLD).replace("{0}", Palette.BTN_SET + groupName);
        }

        public Point point() { return new Point(); }
        public Buttons buttons() { return new Buttons(); }

        public class Point {
            private final String SUB = BASE + "point.";
            public String getName(String index) { return format(SUB + "name", BTN_MACRO, index); }
            public String getLoreX(String val) { return format(SUB + "lore-x", LORE_TEXT, val); }
            public String getLoreY(String val) { return format(SUB + "lore-y", LORE_TEXT, val); }
            public String getLoreZ(String val) { return format(SUB + "lore-z", LORE_TEXT, val); }
            public String getLoreRot(String y, String x) {
                return get(SUB + "lore-rot", LORE_TEXT).replace("{0}", LORE_VAL + y + LORE_TEXT).replace("{1}", LORE_VAL + x + LORE_TEXT);
            }
        }

        public class Buttons {
            private final String SUB = BASE + "buttons.";
            public String getBackLore() { return get(SUB + "back-lore", LORE_TEXT); }
            public String getRefresh() { return get(SUB + "refresh", BTN_ACT); }
            public String getRefreshLore() { return get(SUB + "refresh-lore", LORE_TEXT); }
        }
    }

    // --- Helpers ---
    private String get(String path, String colorPrefix) {
        String val = config.getString(path);
        return val != null ? TextFormatter.process(val, colorPrefix) : "§cMissing: " + path;
    }

    private List<String> getList(String path, String colorPrefix) {
        return config.getStringList(path).stream()
                .map(s -> ChatColor.translateAlternateColorCodes('&', colorPrefix + s))
                .collect(Collectors.toList());
    }

    private String format(String path, String prefix, String arg0) {
        return get(path, prefix).replace("{0}", LORE_VAL + arg0 + prefix);
    }

    private List<String> formatList(String path, String prefix, String arg0) {
        return getList(path, prefix).stream()
                .map(s -> s.replace("{0}", LORE_VAL + arg0 + prefix))
                .collect(Collectors.toList());
    }

    @Override
    public void updateSource(FileConfiguration newSource) {
        this.config = newSource;
    }
}