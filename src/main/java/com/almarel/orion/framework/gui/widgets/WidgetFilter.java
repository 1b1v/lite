package com.almarel.orion.framework.gui.widgets;

import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;

public class WidgetFilter {

    /**
     * Отрисовывает виджет фильтрации в виде таб-бара (3 предмета).
     *
     * @param holder     Контейнер меню, куда добавляется кнопка.
     * @param slots      Массив из 3 слотов (например: 3, 4, 5), где будет отрисован таб-бар.
     * @param isPersonal true, если текущий фильтр Personal, false — если Presets.
     * @param onClick    Действие при клике (переключение фильтра).
     */
    public static void draw(OrionMenuHolder holder, int[] slots, boolean isPersonal, Runnable onClick) {

        Material activeMaterial = isPersonal ? Material.BLACK_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;

        String activeText;
        String inactiveText;

        if (isPersonal) {
            activeText = "§b§lMy";
            inactiveText = "§7Presets";
        } else {
            activeText = "§e§lPresets";
            inactiveText = "§7My";
        }

        String title = activeText + " §8[ " + inactiveText + " §8]";
        String lore = "  §7Switch to " + (isPersonal ? "server presets" : "your brushes");

        ItemBuilder builder = new ItemBuilder(activeMaterial)
                .name(title)
                .lore(
                        lore,
                        "",
                        "  §fLMB §7- Toggle"
                );

        for (int slot : slots) {
            holder.addButton(slot, new GuiMenuButton(builder.build(), e -> {
                if (e.isLeftClick()) {
                    onClick.run();
                }
            }));
        }
    }
}