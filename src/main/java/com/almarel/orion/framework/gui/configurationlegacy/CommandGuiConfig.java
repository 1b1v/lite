package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

public class CommandGuiConfig implements ReloadableConfig {

    private FileConfiguration config;
    private final String ROOT = "gui.command.";

    private final Record record = new Record();
    private final Item item = new Item();
    private final Message message = new Message();

    public CommandGuiConfig(FileConfiguration fullConfig) {
        this.config = fullConfig;
    }

    public String getTitle(int page) {
        // Формат: Title (p. 1)
        return get(ROOT + "title", HEADER_PURPLE) + LORE_TEXT + " (p. " + page + ")";
    }

    public Record record() { return record; }
    public Item item() { return item; }
    public Message message() { return message; }

    public class Record {
        private final String BASE = ROOT + "record.";
        public String getName() { return get(BASE + "name", BTN_MACRO); }
        public List<String> getLore() {
            return getList(BASE + "lore").stream()
                    .map(s -> LORE_TEXT + s)
                    .collect(Collectors.toList());
        }
    }

    public class Item {
        private final String BASE = ROOT + "item.";

        public String getName(String macroName) { return format(BASE + "name", BTN_SET, macroName); }
        public String getCmdPrefix() { return get(BASE + "cmd-prefix", VAL_ON); }
        public String getEmpty() { return get(BASE + "empty", LORE_META); }
        public String getMore(int count) { return format(BASE + "more", LORE_META, String.valueOf(count)); }
    }

    public class Message {
        private final String BASE = ROOT + "message.";
        public String getDeleted() { return get(BASE + "deleted", VAL_ON); }
    }

    // --- Helpers ---
    private String get(String path, String colorPrefix) {
        String val = config.getString(path);
        // Вызываем наш новый форматтер
        return val != null ? TextFormatter.process(val, colorPrefix) : "§cMissing: " + path;
    }

    private List<String> getList(String path) {
        if (!config.contains(path)) return new ArrayList<>();
        return config.getStringList(path);
    }

    private String format(String path, String prefix, String arg0) {
        return get(path, prefix).replace("{0}", LORE_VAL + arg0 + prefix);
    }

    @Override
    public void updateSource(FileConfiguration newSource) {
        this.config = newSource;
    }
}