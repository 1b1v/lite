package com.almarel.orion.brush.presentation.gui.settings.widgets;

import com.almarel.orion.brush.data.enums.ScrollAction;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;

import java.util.Set;

public class WidgetScroll {

    public interface ToggleCallback {
        void onToggle();
    }

    public interface ActionCallback {
        void onActionToggle(ScrollAction action);
    }


    // --- Legacy Implementation ---

    public static void drawToggle(OrionMenuHolder holder, int slot, CommonGuiConfig config,
                                  boolean isEnabled, ToggleCallback callback) {

        String st = isEnabled
                ? Palette.VAL_ON + config.getText("values.on")
                : Palette.VAL_OFF + config.getText("values.off");

        var heart = new ItemBuilder(Material.HEART_OF_THE_SEA)
                // Используем getColored с цветом HEADER_AQUA (Голубой жирный)
                .name(config.getColored("scroll.main-name", Palette.HEADER_AQUA, st))
                .lore("  " + config.getText("controls.toggle-lkm"));

        if (isEnabled) heart.glow();

        holder.addButton(slot, new GuiMenuButton(heart.build(), e -> callback.onToggle()));
    }

    public static void drawCheckbox(OrionMenuHolder holder, int slot, CommonGuiConfig config,
                                    String name, ScrollAction action,
                                    Set<ScrollAction> activeSet,
                                    ActionCallback callback) {

        boolean isActive = activeSet.contains(action);
        Material icon = isActive ? Material.RED_DYE : Material.WHITE_DYE;

        String status = isActive
                ? Palette.VAL_ON + config.getText("values.on")
                : Palette.VAL_OFF + config.getText("values.off");

        var builder = new ItemBuilder(icon)
                // Используем getColored с цветом BTN_NAV (Светло-голубой)
                .name(config.getColored("scroll.checkbox", Palette.BTN_NAV, name, status))
                .lore("  " + config.getText("scroll.checkbox-lore"));

        if (isActive) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), e -> callback.onActionToggle(action)));
    }
}