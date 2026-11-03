package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class ClipboardGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.clipboard-browser.";

    private final Message message = new Message();
    private final Item item = new Item();
    private final Tabs tabs = new Tabs();
    private final FileSection file = new FileSection();
    private final Bottom bottom = new Bottom();
    private final Quick quick = new Quick();

    public ClipboardGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    public String getTitleMain() { return get(ROOT + "title-main", TITLE); }
    public String getTitleBrush(int index) { return format(ROOT + "title-brush", ERROR, String.valueOf(index)); }

    public Message message() { return message; } // почему-то no usage
    public Item item() { return item; } // почему-то no usage
    public Tabs tabs() { return tabs; } // почему-то no usage
    public FileSection file() { return file; } // почему-то no usage
    public Bottom bottom() { return bottom; } // почему-то no usage
    public Quick quick() { return quick; } // почему-то no usage

    public class Message {
        private final String BASE = ROOT + "message.";
        public String getCleared() { return get(BASE + "cleared", VAL_ON); } // почему-то no usage
        public String getSchemsAdded(int count) { return format(BASE + "schems-added", VAL_ON, String.valueOf(count)); }
        public String getFolderNotEmpty() { return get(BASE + "folder-not-empty", ERROR); } // почему-то no usage
        public String getDuplicateIgnored() { return get(BASE + "duplicate-ignored", ERROR); } // почему-то no usage
        public String getReadError(String err) { return format(BASE + "read-error", ERROR, err); } // почему-то no usage
        public String getGroupUnpacked() { return get(BASE + "group-unpacked", VAL_ON); } // почему-то no usage
        public String getGroupCopyError() { return get(BASE + "group-copy-error", ERROR); } // почему-то no usage
    }

    public class Item {
        private final String BASE = ROOT + "item.";
        public String getSize(String size) { return format(BASE + "size", LORE_TEXT, size); }

        // [FIX] Желтый (BTN_SET) для ключа "Схема"
        public String getSchemeActive(String name) { return format(BASE + "scheme-active", BTN_SET, name); }
        public String getSchemeNormal(String name) { return format(BASE + "scheme-normal", BTN_SET, name); }

        public String getInMemory() { return get(BASE + "in-memory", LORE_TEXT); }
        public String getCountSuffix(int count) { return format(BASE + "count-suffix", LORE_META, String.valueOf(count)); }
        public String getBlocks() { return get(BASE + "blocks", LORE_TEXT); }
        public String getGroupPrefix() { return get(BASE + "group-prefix", LORE_TEXT); }
    }

    public class Tabs {
        private final String BASE = ROOT + "tabs.";
        public String getShared() { return get(BASE + "shared", BTN_SET); }
        public String getFavorites() { return get(BASE + "favorites", BTN_SET); }
        public String getSchems() { return get(BASE + "schems", BTN_SET); }

        // [FIX] Серый (LORE_TEXT) для "Текущая вкладка"
        public String getCurrent() { return get(BASE + "current", LORE_TEXT); }
        public String getClick() { return get(BASE + "click", LORE_TEXT); }

        // [FIX] Желтый (BTN_SET) для ключей режимов
        public String getModeCopy(String state) { return format(BASE + "mode-copy", BTN_SET, state); }
        public String getModeCopyLore() { return get(BASE + "mode-copy-lore", LORE_TEXT); }
        public String getModeRotate(String state) { return format(BASE + "mode-rotate", BTN_SET, state); }
        public String getModeRotateLore() { return get(BASE + "mode-rotate-lore", LORE_TEXT); }

        public String getClear() { return get(BASE + "clear", ERROR); }
        public List<String> getClearLore() { return getList(BASE + "clear-lore"); }
    }

    public class FileSection {
        private final String BASE = ROOT + "file.";
        public String getEmpty() { return get(BASE + "empty", LORE_META); }
        public String getOpen() { return get(BASE + "open", LORE_TEXT); }
        public String getDeleteFolder() { return get(BASE + "delete-folder", ERROR); }
        public String getRenameFolder() { return get(BASE + "rename-folder", BTN_NAV); }

        public String getLkmAddBrush() { return get(BASE + "lkm-add-brush", BTN_ACT); }
        public String getLkmLoadGlobal() { return get(BASE + "lkm-load-global", BTN_ACT); }
        public String getPkmDeleteFav() { return get(BASE + "pkm-delete-fav", ERROR); }

        // [FIX] Желтый (BTN_SET) для "Сортировка"
        public String getSort(String type) { return format(BASE + "sort", BTN_SET, type); }

        public String getSortFolders() { return get(BASE + "sort-folders", LORE_VAL); }
        public String getSortSchems() { return get(BASE + "sort-schems", LORE_VAL); }
        public String getSortLore() { return get(BASE + "sort-lore", LORE_TEXT); }

        public String getCreate() { return get(BASE + "create", BTN_MACRO); }
        public String getCreateLore() { return get(BASE + "create-lore", LORE_TEXT); }
        public String getHistoryMode(String state) { return format(BASE + "history-mode", LORE_TEXT, state); }

        // Новые методы (добавленные ранее)
        public String getAddAll() { return get(BASE + "add-all", BTN_ACT); }
        public String getAddAllLore() { return get(BASE + "add-all-lore", LORE_TEXT); }
    }

    public class Bottom {
        private final String BASE = ROOT + "bottom.";
        public String getBufferCurrent() { return get(BASE + "buffer-current", HEADER_AQUA); }
        public String getBufferNormal() { return get(BASE + "buffer-normal", LORE_TEXT); }
        public String getMore(int count) { return format(BASE + "more", LORE_META, String.valueOf(count)); }
        public String getSwitchLore() { return get(BASE + "switch-lore", BTN_SET); }
        public String getFaweActive() { return get(BASE + "fawe-active", LORE_TEXT); } // почему-то no usage
        public String getFaweLore() { return get(BASE + "fawe-lore", LORE_TEXT); } // почему-то no usage
    }

    public class Quick {
        private final String BASE = ROOT + "quick.";
        public String getPath(String path) { return format(BASE + "path", LORE_META, path); }
        public String getClick() { return get(BASE + "click", LORE_TEXT); }
    }

    // --- Helpers ---
    private String get(String path, String colorPrefix) {
        String val = config.getString(path);
        return val != null ? TextFormatter.process(val, colorPrefix) : "§cMissing: " + path;
    }

    private String format(String path, String prefix, String arg0) {
        return get(path, prefix).replace("{0}", LORE_VAL + arg0 + prefix);
    }

    private List<String> getList(String path) {
        if (!config.contains(path)) return new ArrayList<>();
        return config.getStringList(path).stream()
                .map(s -> LORE_TEXT + s)
                .collect(Collectors.toList());
    }

    @Override
    public void updateSource(FileConfiguration newSource) {
        this.config = newSource;
    }
}