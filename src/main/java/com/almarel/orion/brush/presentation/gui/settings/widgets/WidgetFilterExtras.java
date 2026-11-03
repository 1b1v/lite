package com.almarel.orion.brush.presentation.gui.settings.widgets;

import com.almarel.orion.brush.data.capabilities.IgnoreListable;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import org.bukkit.Material;

public class WidgetFilterExtras {

    /**
     * Создает кнопку "Target Block Mask" для меню списка материалов.
     */
    public static GuiMenuButton createTargetBlockMaskButton(IgnoreListable state, Runnable reOpenEditor) {
        boolean active = state.isUseTargetBlock();

        ItemBuilder builder = new ItemBuilder(Material.TARGET)
                .name("§eTarget Block Mask: " + (active ? "§aON" : "§cOFF"))
                .lore(" ",
                        "  §7If enabled, the brush will ignore",
                        "  §7the list and strictly replace",
                        "  §7ONLY the type of block you",
                        "  §7initially clicked on.",
                        " ",
                        "  §fClick §7to toggle");

        if (active) builder.glow();

        return new GuiMenuButton(builder.build(), event -> {
            boolean nextState = !active;
            state.setUseTargetBlock(nextState);

            // [FIX] Форсируем Whitelist (Заменять ТОЛЬКО такие же блоки), если включили Target
            if (nextState) {
                state.setIgnoreWhitelist(true);
            }

            reOpenEditor.run(); // Перерисовываем меню
        });
    }

    /**
     * Создает кнопку переключения режима (WhiteList / BlackList) для меню списка материалов.
     */
    public static GuiMenuButton createWhitelistToggleButton(IgnoreListable state, Runnable reOpenEditor) {
        boolean isWhitelist = state.isIgnoreWhitelist();

        Material icon = isWhitelist ? Material.LIME_DYE : Material.GRAY_DYE;
        String modeName = isWhitelist ? "§aInclude List (Whitelist)" : "§cExclude List (Blacklist)";

        ItemBuilder builder = new ItemBuilder(icon)
                .name("§eMode: " + modeName)
                .lore(" ",
                        "  §7Current behavior:",
                        isWhitelist ? "  §aMatches ONLY blocks" : "  §cIgnores ALL blocks",
                        isWhitelist ? "  §athat are in this list." : "  §cthat are in this list.",
                        " ",
                        "  §fClick §7to toggle mode");

        if (isWhitelist) builder.glow();

        return new GuiMenuButton(builder.build(), event -> {
            state.setIgnoreWhitelist(!isWhitelist);
            reOpenEditor.run(); // Перерисовываем меню
        });
    }
}