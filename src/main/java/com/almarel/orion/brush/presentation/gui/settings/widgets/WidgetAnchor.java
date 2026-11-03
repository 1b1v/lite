package com.almarel.orion.brush.presentation.gui.settings.widgets;

import com.almarel.orion.brush.data.capabilities.Anchorable;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

public class WidgetAnchor {

    public interface AnchorCallback {
        void onOffsetChange(int delta);
        void onModeSwitch();
    }

    public static void draw(OrionMenuHolder holder, CommonGuiConfig config,
                            String modeDisplayName, boolean isActive, int currentOffset,
                            AnchorCallback callback) {
        // Хардкод слота 43 оставлен для обратной совместимости
        draw(holder, 43, config, modeDisplayName, isActive, currentOffset, callback);
    }

    private static void draw(OrionMenuHolder holder, int slot, CommonGuiConfig config,
                             String modeDisplayName, boolean isActive, int currentOffset,
                             AnchorCallback callback) {

        Material icon = isActive ? Material.GOLD_INGOT : Material.IRON_INGOT;
        String offsetStr = (currentOffset > 0 ? "+" : "") + currentOffset;

        var builder = new ItemBuilder(icon)
                .name(config.anchor().getName(modeDisplayName))
                .lore("  " + config.controls().getToggleSkm(),
                        "  " + config.anchor().getLoreOffset(offsetStr),
                        "  " + config.controls().getLkmPkm("+1", "-1"));

        if (isActive) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            ClickType click = event.getClick();
            if (click == ClickType.MIDDLE) {
                callback.onModeSwitch();
            } else if (click.isLeftClick() || click.isRightClick()) {
                int delta = 0;
                if (event.isShiftClick()) delta = event.isRightClick() ? -5 : 5;
                else delta = event.isRightClick() ? -1 : 1;

                callback.onOffsetChange(delta);
            }
        }));
    }
}