// TYPE B: FULL FILE (Новый класс)
package com.almarel.orion.brush.presentation.gui.settings.widgets.grid;

import com.almarel.orion.brush.data.capabilities.FloodFillable;
import com.almarel.orion.brush.data.capabilities.IgnoreListable;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.widgets.WidgetControls;
import org.bukkit.Material;

/**
 * Отвечает за виджеты, управляющие алгоритмами пространственного выделения (Selection Engine).
 */
public class WidgetSelection {

    public static void drawFloodFillWidget(OrionMenuHolder holder, int slot, FloodFillable state, CommonGuiConfig config, WidgetControls.ToggleCallback callback) {
        boolean enabled = state.isFloodFillEnabled();
        boolean exact = state.isExactSelection();

        String statusText;
        Material icon = Material.WATER_BUCKET;
        boolean glow = false;

        if (!enabled) {
            statusText = "§cDefault (Radius)";
            icon = Material.BUCKET;
        } else if (!exact) {
            statusText = "§eConnected Box";
        } else {
            statusText = "§aConnected Exact";
            glow = true;
        }

        var builder = new ItemBuilder(icon)
                .name("§eSelection: " + statusText)
                .lore("  §7Click to cycle selection modes:",
                        "  §8- Default: Standard radius-based brush",
                        "  §8- Connected Box: Cuboid around connected blocks",
                        "  §8- Connected Exact: Pixel-perfect connectivity");

        if (glow) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            if (!state.isFloodFillEnabled()) {
                state.setFloodFillEnabled(true);
                state.setExactSelection(false);
            } else if (!state.isExactSelection()) {
                state.setExactSelection(true);
            } else {
                state.setFloodFillEnabled(false);
                state.setExactSelection(false);
            }
            callback.onToggle();
        }));
    }

    public static void drawIgnoreListWidget(OrionMenuHolder holder, int slot, IgnoreListable state, CommonGuiConfig config,
                                            boolean hasIgnore, java.util.List<String> baseLore,
                                            java.util.function.Consumer<Material> onAddMaterial,
                                            Runnable onClear, Runnable onOpenEditor) {
        boolean whitelistMode = state.isIgnoreWhitelist();
        boolean useTarget = state.isUseTargetBlock();

        Material icon = useTarget ? Material.TARGET : (whitelistMode ? Material.REDSTONE : (hasIgnore ? Material.GLOWSTONE_DUST : Material.SUGAR));

        String name;
        if (useTarget) {
            name = "§eFilter: §bTarget Block";
        } else if (whitelistMode) {
            name = "§eFilter: §aInclude List";
        } else {
            name = "§eFilter: §cExclude List";
        }

        // [FIX] Собираем весь лор в один список, чтобы ничего не перезаписалось
        java.util.List<String> finalLore = new java.util.ArrayList<>();

        if (useTarget) {
            finalLore.add("  §7Ignores the list and strictly");
            finalLore.add("  §7flows only through the block");
            finalLore.add("  §7you initially clicked on.");
        } else {
            finalLore.addAll(baseLore);
        }

        finalLore.add(" ");
        finalLore.add("  §fLMB §7- Open Setup Menu");

        if (!useTarget) {
            finalLore.add("  §fClick with Block §7- Quick Add");
            if (hasIgnore) {
                finalLore.add("  §fRMB §7- Quick Clear");
            }
        }

        var builder = new ItemBuilder(icon).name(name).lore(finalLore);

        if (hasIgnore || whitelistMode || useTarget) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            org.bukkit.inventory.ItemStack cursor = event.getCursor();
            boolean hasItemInHand = cursor != null && cursor.getType() != Material.AIR && cursor.getType().isBlock();

            // Если кликаем предметом -> Добавляем в фильтр
            if (hasItemInHand) {
                onAddMaterial.accept(cursor.getType());
                return;
            }

            // Если кликаем пустой мышкой:
            if (event.isLeftClick()) {
                if (onOpenEditor != null) onOpenEditor.run();
            } else if (event.isRightClick()) {
                onClear.run();
            }
        }));
    }
}