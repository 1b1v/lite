package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class CopyPasteGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.brush-cp.";

    private final Title title = new Title();
    private final Icon icon = new Icon();
    private final RotMode rotMode = new RotMode();
    private final Mask mask = new Mask();

    // Удалены дубликаты: Smooth, Smear, Anchor, ScaleAxis, ManualRot

    public CopyPasteGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    public Title title() { return title; }
    public Icon icon() { return icon; }
    public RotMode rotMode() { return rotMode; }
    public Mask mask() { return mask; }

    // --- Sections ---

    public class Title {
        private final String BASE = ROOT + "title.";
        public String getHand(String index) { return get(BASE + "hand", ERROR).replace("{0}", VAL_HIGH + index + ERROR); }
        public String getSimple() { return get(BASE + "simple", BTN_SET); }
        public String getPreview() { return get(BASE + "preview", BTN_SET); }
    }

    public class Icon {
        private final String BASE = ROOT + "icon.";
        public String getFlags(String flags) { return format(BASE + "flags", LORE_META, flags); }
        public String getScroll(String status) { return format(BASE + "scroll", LORE_META, status); }
        public String getAnchor(String anchor) { return format(BASE + "anchor", LORE_META, anchor); }
        public String getRange(String range) { return format(BASE + "range", LORE_META, range); }
        public String getNote() { return get(BASE + "note", LORE_TEXT); }
    }

    public class RotMode {
        private final String BASE = ROOT + "rotation-mode.";
        public String getPrefix() { return get(BASE + "prefix", BTN_SET); }
        // getLore() удален -> используйте CommonConfig
    }

    public class Mask {
        private final String BASE = ROOT + "mask.";
        public String getName(String status) { return format(BASE + "name", BTN_SET, status); }
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