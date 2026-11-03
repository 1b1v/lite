package com.almarel.orion.framework.gui.widgets;

import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.gui.data.enums.BrushTab;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.data.GuiLayout;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WidgetTabPanel {

    public interface TabSwitcher {
        void switchTab(BrushTab tab);
    }

    // --- Main Implementation ---

    public static void draw(OrionMenuHolder holder, CommonGuiConfig config,
                            BrushTab currentTab, String brushTitle, Player player, TabSwitcher switcher) {
        drawInternal(holder, config, currentTab, brushTitle, player, true, switcher);
    }

    private static void drawInternal(OrionMenuHolder holder, CommonGuiConfig config,
                                     BrushTab currentTab, String brushTitle, Player player,
                                     boolean supportsConstruct, TabSwitcher switcher) {

        // 1. Отрисовка базовой вкладки настроек
        drawSettingsTab(holder, config, currentTab, brushTitle, switcher);

    }

    // --- Tab Renderers ---

    private static void drawSettingsTab(OrionMenuHolder holder, CommonGuiConfig config,
                                        BrushTab currentTab, String brushTitle, TabSwitcher switcher) {
        boolean isSet = (currentTab == BrushTab.SETTINGS);
        String prefix = isSet ? ChatColor.RED.toString() + ChatColor.BOLD : Palette.TITLE;

        var item = new ItemBuilder(isSet ? Material.BEETROOT_SOUP : Material.MUSHROOM_STEW)
                .name(prefix + brushTitle)
                .lore(config.getTextList("tabs.settings"));

        if (isSet) item.glow();

        holder.addButton(GuiLayout.SLOT_TAB_SETTINGS, new GuiMenuButton(item.build(), e -> switcher.switchTab(BrushTab.SETTINGS)));
    }

}