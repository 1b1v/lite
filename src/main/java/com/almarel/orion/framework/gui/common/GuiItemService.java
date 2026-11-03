package com.almarel.orion.framework.gui.common;

import com.almarel.orion.framework.compat.AttributeCompatSupport;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class GuiItemService {
    private GuiItemService() {}

    // --- UTILITIES ---

    public static String humanSize(long bytes) {
        ChatColor color = ChatColor.GREEN; // < 100KB
        if (bytes >= 1024 * 1024) color = ChatColor.RED; // >= 1MB
        else if (bytes >= 100 * 1024) color = ChatColor.YELLOW; // >= 100KB

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double b = bytes;
        int u = 0;
        while (b >= 1024 && u < units.length - 1) { b /= 1024; u++; }
        return color + String.format(Locale.ROOT, "%.1f %s", b, units[u]);
    }

    public static int slot(int row, int col) {
        return row * 9 + col;
    }

    // --- HELPERS (Используются внутри Builder) ---

    public static void addGlow(ItemMeta meta) {
        if (meta == null) return;
        try {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
            if (ench == null) ench = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));
            if (ench == null) ench = Enchantment.getByKey(NamespacedKey.minecraft("lure"));

            if (ench != null) {
                meta.addEnchant(ench, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } catch (Throwable ignored) {}
    }

    private static volatile Attribute cachedDummyAttribute;
    private static volatile boolean dummyAttributeResolved = false;

    /**
     * Версио-независимый способ получить ЛЮБОЙ существующий Attribute из реестра.
     * Нам не важно КАКОЙ именно атрибут — важен сам факт наличия модификатора,
     * чтобы клиент корректно применил флаг HIDE_ATTRIBUTES.
     * Это защищает от переименования конкретных констант (как ATTACK_DAMAGE/GENERIC_ATTACK_DAMAGE) между версиями API.
     */
    private static Attribute resolveDummyAttribute() {
        if (dummyAttributeResolved) return cachedDummyAttribute;
        synchronized (GuiItemService.class) {
            if (dummyAttributeResolved) return cachedDummyAttribute;
            Attribute result = null;
            try {
                Iterator<Attribute> it = Registry.ATTRIBUTE.iterator();
                if (it.hasNext()) result = it.next();
            } catch (Throwable ignored) {
                // Registry недоступен в данной версии API — хак просто не применится.
            }
            cachedDummyAttribute = result;
            dummyAttributeResolved = true;
            return result;
        }
    }

    public static void hideAttributes(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        AttributeCompatSupport.attachHideAttributesHack(meta);
        item.setItemMeta(meta);
    }


    // --- ITEM BUILDER ---

    public static class ItemBuilder {
        private final ItemStack item;
        private final ItemMeta meta;

        public ItemBuilder(Material material) {
            this.item = new ItemStack(material);
            this.meta = item.getItemMeta();
            initFlags();
        }

        public ItemBuilder addLore(List<String> lines) { // no usage
            if (meta != null && lines != null && !lines.isEmpty()) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.addAll(lines);
                meta.setLore(lore);
            }
            return this;
        }

        public ItemBuilder(ItemStack stack) {
            this.item = stack.clone();
            this.meta = item.getItemMeta();
            initFlags();
        }

        private void initFlags() {
            if (this.meta != null) {
                this.meta.addItemFlags(ItemFlag.values());
            }
        }

        public ItemBuilder amount(int amount) { // no usage
            item.setAmount(amount);
            return this;
        }

        public ItemBuilder name(String name) {
            if (meta != null && name != null) meta.setDisplayName(name);
            return this;
        }

        public ItemBuilder lore(String... lore) {
            return lore(Arrays.asList(lore));
        }

        public ItemBuilder lore(List<String> lore) {
            if (meta != null && lore != null && !lore.isEmpty()) meta.setLore(lore);
            return this;
        }

        public ItemBuilder addLore(String line) {
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(line);
                meta.setLore(lore);
            }
            return this;
        }

        public ItemBuilder modelData(int data) {
            if (meta != null) {
                meta.setCustomModelData(data);
            }
            return this;
        }

        public ItemBuilder glow() {
            addGlow(meta);
            return this;
        }

        public ItemBuilder action(NamespacedKey key, String actionValue) {
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, actionValue);
            }
            return this;
        }

        public ItemBuilder data(NamespacedKey key, int value) {
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
            }
            return this;
        }

        public ItemBuilder data(NamespacedKey key, String value) {
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            }
            return this;
        }

        public ItemStack build() {
            if (meta != null) item.setItemMeta(meta);
            // Этот метод теперь содержит хак, который уберет текст "Урон при атаке"
            hideAttributes(item);
            return item;
        }

        public ItemBuilder leatherColor(int r, int g, int b) {
            if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
                leatherArmorMeta.setColor(Color.fromRGB(r, g, b));
            }
            return this;
        }
    }
}