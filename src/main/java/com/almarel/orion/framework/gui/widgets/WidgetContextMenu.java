// TYPE B: FULL FILE
package com.almarel.orion.framework.gui.widgets;

import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;

/**
 * Универсальный виджет контекстного меню (5-я строка).
 * Используется для специальных действий над выбранным элементом (Экспорт, Форк / Публикация).
 */
public class WidgetContextMenu {

    public static void draw(OrionMenuHolder holder,
                            boolean isGlobal, boolean isAdmin,
                            Runnable onPublishFork, Runnable onExport) {

        // [1] Экспорт (Слот 38) - Рисуется только если передан коллбек
        if (onExport != null) {
            holder.addButton(38, new GuiMenuButton(new ItemBuilder(Material.WRITTEN_BOOK)
                    .name("§bExport")
                    .lore("  §7Get JSON code", "", "  §fLMB §7- Copy to chat")
                    .glow().build(), e -> {
                if (e.isLeftClick()) onExport.run();
            }));
        }

        // [2] Форк / Публикация (Слот 40)
        if (isGlobal || isAdmin) {
            Material pIcon = isGlobal ? (isAdmin ? Material.PAPER : Material.DISPENSER) : Material.HOPPER;
            String pTitle = isGlobal ? (isAdmin ? "§eCreate local draft" : "§aCopy to self [Fork]") : "§aPublish to server";
            String pDesc = isGlobal ? "§7Creates a local copy for editing" : "§7Template will be available to all players";

            holder.addButton(40, new GuiMenuButton(new ItemBuilder(pIcon)
                    .name(pTitle)
                    .lore("  " + pDesc, "", "  §fLMB §7- Confirm")
                    .glow().build(), e -> {
                if (e.isLeftClick() && onPublishFork != null) onPublishFork.run();
            }));
        }
    }
}