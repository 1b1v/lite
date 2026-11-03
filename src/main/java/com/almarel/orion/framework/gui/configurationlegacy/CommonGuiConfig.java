package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.stream.Collectors;

import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class CommonGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.common.";

    // --- Legacy Inner Classes (Deprecated, to be removed) ---
    private final Values values = new Values();
    private final Nav nav = new Nav();
    private final Actions actions = new Actions();
    private final Controls controls = new Controls();
    private final Scroll scroll = new Scroll();
    private final Pagination pagination = new Pagination();
    private final Rotation rotation = new Rotation();
    private final Tabs tabs = new Tabs();
    private final Anchor anchor = new Anchor();
    private final Ignore ignore = new Ignore();
    private final Editors editors = new Editors(); // [NEW]

    public CommonGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    // =================================================================================
    // [NEW] Hybrid System API
    // =================================================================================

    /**
     * Получает текст для лора (Серый по умолчанию).
     */
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

    /**
     * Получает текст для названия предмета (Желтый по умолчанию).
     */
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

    /**
     * Получает текст с кастомным базовым цветом из Palette.
     */
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

    public Values values() { return values; }
    public Nav nav() { return nav; }
    public Actions actions() { return actions; }
    public Controls controls() { return controls; }
    public Scroll scroll() { return scroll; }
    public Pagination pagination() { return pagination; }
    public Rotation rotation() { return rotation; }
    public Tabs tabs() { return tabs; }
    public Anchor anchor() { return anchor; }
    public Ignore ignore() { return ignore; }
    public Editors editors() { return editors; }

    // --- Sections ---

    public class Editors {
        public String getPattern() { return getName("editors.pattern"); }
        public String getMask() { return getName("editors.mask"); }
        public String getSourceMask() { return getName("editors.source-mask"); }
    }

    public class Values {
        private final String BASE = ROOT + "values.";
        public String getOn() { return get(BASE + "on", VAL_ON); }
        public String getOff() { return get(BASE + "off", VAL_OFF); }
        public String getNone() { return get(BASE + "none", ""); }
    }


    public class Nav {
        private final String BASE = ROOT + "nav.";

        public String getBufferName() { return get(BASE + "buffer.name", BTN_ACT); }
        public List<String> getBufferLore() { return getList(BASE + "buffer.lore", LORE_TEXT); }

        public String getBufferActiveName() { return get(BASE + "buffer.name-active", BTN_ACT); }
        public List<String> getBufferActiveLore() { return getList(BASE + "buffer.lore-active", LORE_TEXT); }

        public String getCommandsName() { return get(BASE + "commands.name", BTN_MACRO); }
        public List<String> getCommandsLore() { return getList(BASE + "commands.lore", LORE_TEXT); }
        public String getCommandsLoreCurrent() { return get(BASE + "commands.lore-current", LORE_TEXT); }

        public String getCenterListName() { return get(BASE + "center.list-name", BTN_NAV); }
        public List<String> getCenterListLore() { return getList(BASE + "center.list-lore", LORE_TEXT); }

        public String getCenterSettingsName(String brushName) {
            return get(BASE + "center.settings-name", BTN_SET).replace("{0}", brushName);
        }
        public List<String> getCenterSettingsLore() { return getList(BASE + "center.settings-lore", LORE_TEXT); }
    }

    public class Actions {
        private final String BASE = ROOT + "actions.";

        public String getBack() { return get(BASE + "back", BTN_NAV); }
        public String getClose() { return get(BASE + "close", BTN_CLOSE); }
        public String getNext() { return get(BASE + "next", BTN_NAV); }
        public String getSave() { return get(BASE + "save", BTN_ACT); }
        public String getCancel() { return get(BASE + "cancel", BTN_CLOSE); }
    }

    public class Controls {
        private final String BASE = ROOT + "controls.";

        public String getLkmPkm(String pos, String neg) { return format(BASE + "lkm-pkm", LORE_TEXT, pos, neg); }


        public String getToggleSkm() { return get(BASE + "toggle-skm", LORE_TEXT); }
        public String getEditSkm() { return get(BASE + "edit-skm", LORE_TEXT); }

        public String getApplyLKM() { return get(BASE + "apply-lkm", LORE_TEXT); }
        public String getDeletePKM() { return get(BASE + "delete-pkm", LORE_TEXT); }

        public String getDeselectAny() { return get(BASE + "deselect-any", LORE_TEXT); }
        public String getMultiShiftLKM() { return get(BASE + "multi-shift-lkm", LORE_TEXT); }
        public String getCopyGlobalShiftPKM() { return get(BASE + "copy-global-shift-pkm", LORE_TEXT); }
        public String getUnpackSKM() { return get(BASE + "unpack-skm", LORE_TEXT); }
        public String getAddFavSKM() { return get(BASE + "add-fav-skm", LORE_TEXT); }
    }

    public class Scroll {
        private final String BASE = ROOT + "scroll.";

        public String getBufferSlot() { return get(BASE + "buffer-slot", ""); }
        public String getRotation() { return get(BASE + "rotation", ""); }
        public String getScaleAxis() { return get(BASE + "scale-axis", ""); }
        public String getScale() { return get(BASE + "scale", ""); }
        public String getRadius() { return get(BASE + "radius", ""); }
    }

    public class Pagination {
        private final String BASE = ROOT + "pagination.";

        public String getPrev(int pageNum) { return format(BASE + "prev", BTN_NAV, String.valueOf(pageNum)); }
        public String getPrevLore() { return get(BASE + "prev-lore", LORE_TEXT); }

        public String getNext(int pageNum) { return format(BASE + "next", BTN_NAV, String.valueOf(pageNum)); }
        public String getNextLore() { return get(BASE + "next-lore", LORE_TEXT); }
    }

    public class ScaleAxis {
        private final String BASE = ROOT + "scale-axis.";
        public String getName(String axis, String val) {
            return get(BASE + "name", LORE_TEXT)
                    .replace("{0}", LORE_VAL + axis + LORE_TEXT)
                    .replace("{1}", LORE_VAL + val + LORE_TEXT);
        }
    }

    public class Rotation {
        private final String BASE = ROOT + "rotation.";

        public String getName(String dir, String val) {
            return get(BASE + "name", LORE_TEXT)
                    .replace("{0}", LORE_VAL + dir + LORE_TEXT)
                    .replace("{1}", LORE_VAL + val + LORE_TEXT);
        }

        public String getUp() { return get(BASE + "up", BTN_SET); }
        public String getLeft() { return get(BASE + "left", BTN_SET); }
        public String getRight() { return get(BASE + "right", BTN_SET); }
    }

    public class Tabs {
        private final String BASE = ROOT + "tabs.";
        public String getTitleSettings() { return get(BASE + "title-settings", HEADER_PURPLE); }
    }

    public class Anchor {
        private final String BASE = ROOT + "anchor.";
        public String getName(String mode) { return format(BASE + "name", BTN_SET, mode); }
        public String getLoreOffset(String offset) { return format(BASE + "lore-offset", LORE_TEXT, offset); }

        public String getNone() { return get(BASE + "none", ""); }

    }

    public class Params {
        private final String BASE = ROOT + "params.";
        public String getMask(String val) { return format(BASE + "mask", LORE_TEXT, val); }

        public String getRange(String val) { return format(BASE + "range", LORE_TEXT, val); }
        public String getPattern(String val) { return format(BASE + "pattern", LORE_TEXT, val); }
    }



    // --- Helpers (Legacy & Internal) ---

    private String get(String path, String colorPrefix) {
        String val = config.getString(path);
        if (val == null) {
            val = "Missing: " + path;
            return "§c" + val;
        }
        return TextFormatter.process(val, colorPrefix);
    }

    private List<String> getList(String path, String colorPrefix) {
        return config.getStringList(path).stream()
                .map(s -> TextFormatter.process(s, colorPrefix))
                .collect(Collectors.toList());
    }

    private String format(String path, String prefix, String... args) {
        String val = get(path, prefix);
        for (int i = 0; i < args.length; i++) {
            val = val.replace("{" + i + "}", LORE_VAL + args[i] + prefix);
        }
        return val;
    }

    public class Ignore {
        private final String BASE = ROOT + "ignore.";
        public String getName() { return get(BASE + "name", BTN_SET); }
        public String getListHeader() { return get(BASE + "list-header", LORE_TEXT); }
        public String getItem(String item) { return format(BASE + "item", VAL_ON, item); }
        public String getMore(String count) { return format(BASE + "more", LORE_TEXT, count); }
        public String getEmpty() { return get(BASE + "empty", LORE_TEXT); }
        public String getAddHint() { return get(BASE + "add-hint", LORE_TEXT); }
        public String getClearHint() { return get(BASE + "clear-hint", LORE_TEXT); }

        public String getModeWhite() { return get(BASE + "mode-white", LORE_TEXT).replace("{0}", VAL_ON).replace("{1}", LORE_TEXT); }
        public String getModeBlack() { return get(BASE + "mode-black", LORE_TEXT).replace("{0}", ERROR).replace("{1}", LORE_TEXT); }
        public String getToggleHint() { return get(BASE + "toggle-hint", BTN_SET).replace("{0}", BTN_SET); }
    }

    @Override
    public void updateSource(FileConfiguration newSource) {
        this.config = newSource;
    }
}