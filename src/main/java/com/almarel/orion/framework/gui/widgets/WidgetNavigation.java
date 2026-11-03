package com.almarel.orion.framework.gui.widgets;

import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent; // Импорт
import java.util.List;
import java.util.function.Consumer; // Импорт

public class WidgetNavigation {

    public static void drawBackButton(OrionMenuHolder holder, int slot, CommonGuiConfig config, List<String> lore, Consumer<InventoryClickEvent> action) {
        var builder = new ItemBuilder(Material.BARRIER)
                .name(config.actions().getBack());

        if (lore != null && !lore.isEmpty()) builder.lore(lore);

        holder.addButton(slot, new GuiMenuButton(builder.build(), action));
    }
}