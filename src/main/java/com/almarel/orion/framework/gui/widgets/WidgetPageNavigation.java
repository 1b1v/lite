package com.almarel.orion.framework.gui.widgets;

import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.common.GuiItemService;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class WidgetPageNavigation {

    public interface PageCallback {
        void onPageChange(int newPage);
    }

    private static final Material ICON_MATERIAL = Material.PAPER;
    private static final int SLOT_PREV = 52;
    private static final int SLOT_NEXT = 53;

    /**
     * Точка входа: координирует отрисовку элементов навигации.
     */
    public static void draw(OrionMenuHolder holder, CommonGuiConfig config,
                            int page, boolean hasPrev, boolean hasNext,
                            PageCallback callback) {
        if (hasPrev) {
            drawPrevButton(holder, config, page, callback);
        }
        if (hasNext) {
            drawNextButton(holder, config, page, callback);
        }
    }

    private static void drawPrevButton(OrionMenuHolder holder, CommonGuiConfig config,
                                       int page, PageCallback callback) {
        // page - это текущий 0-based индекс, поэтому для прошлой страницы отображаем номер page
        ItemStack item = new ItemBuilder(ICON_MATERIAL)
                .name(config.pagination().getPrev(page))
                .lore("  " + config.pagination().getPrevLore())
                .build();

        GuiItemService.hideAttributes(item);

        holder.addButton(SLOT_PREV, new GuiMenuButton(item, event -> callback.onPageChange(page - 1)));
    }

    private static void drawNextButton(OrionMenuHolder holder, CommonGuiConfig config,
                                       int page, PageCallback callback) {
        // Следующая страница по счёту для игрока = (page + 1) + 1
        ItemStack item = new ItemBuilder(ICON_MATERIAL)
                .name(config.pagination().getNext(page + 2))
                .lore("  " + config.pagination().getNextLore())
                .build();

        GuiItemService.hideAttributes(item);

        holder.addButton(SLOT_NEXT, new GuiMenuButton(item, event -> callback.onPageChange(page + 1)));
    }
}