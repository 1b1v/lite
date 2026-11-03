package com.almarel.orion.framework.gui;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.gui.common.GuiItemService;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig; // [NEW]
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.data.GuiLayout;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import com.google.inject.Inject;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NavBarRenderer {

    private final GuiWindowManager guiWindowManager;
    private final CommonGuiConfig commonGuiConfig; // [NEW] Конфиг
    private final BrushStateManager stateManager;
    private final GuiStateManager sessionManager;

    private final ItemStack glassPane;

    @Inject
    public NavBarRenderer(GuiStateManager sessionManager,
                          GuiWindowManager guiWindowManager,
                          CommonGuiConfig commonGuiConfig,
                          BrushStateManager stateManager) {
        this.guiWindowManager = guiWindowManager;
        this.commonGuiConfig = commonGuiConfig;
        this.stateManager = stateManager;
        this.sessionManager = sessionManager;
        this.glassPane = new GuiItemService.ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
    }

    /**
     * Точка входа: оркестрация отрисовки панели навигации.
     */
    public void draw(Inventory menu, Player player, MenuType currentMenu) {
        if (!(menu.getHolder() instanceof OrionMenuHolder holder)) return;

        if (currentMenu != MenuType.CLIPBOARD) {
            fillBackground(menu);

        }

        drawToolsButton(holder, player, currentMenu);

    }

    private void fillBackground(Inventory menu) {
        for (int i = GuiLayout.NAV_BAR_START; i < GuiLayout.ROWS_6; i++) {
            menu.setItem(i, glassPane);
        }
    }

    // ========================================================================
    // COMMANDS BUTTON
    // ========================================================================

    private void drawCommandsButton(OrionMenuHolder holder, Player player, MenuType currentMenu) {
        boolean isActive = (currentMenu == MenuType.COMMANDS);
        ItemStack item = createCommandsItem(isActive);

        holder.addButton(GuiLayout.SLOT_NAV_NEXT, new GuiMenuButton(item, event -> {
            if (!isActive) {
                guiWindowManager.openCommandsMenu(player);
            }
        }));
    }

    private ItemStack createCommandsItem(boolean isActive) {
        String name = commonGuiConfig.nav().getCommandsName();
        var builder = new ItemBuilder(Material.NAME_TAG).name(name);

        if (isActive) {
            builder.glow();
            builder.addLore(commonGuiConfig.nav().getCommandsLoreCurrent());
        } else {
            builder.lore(commonGuiConfig.nav().getCommandsLore());
        }

        return buildSanitizedItem(builder);
    }

    // ========================================================================

    // ========================================================================
    // TOOLS BUTTON (DISPATCHER & MODES)
    // ========================================================================

    private void drawToolsButton(OrionMenuHolder holder, Player player, MenuType currentMenu) {
        BrushVisualContext context = resolveBrushContext(player);

        switch (currentMenu) {
            case LIST -> drawButtonForListMenu(holder, player, context);
            case SETTINGS -> drawButtonForSettingsMenu(holder, player, context);
            case CLIPBOARD, COMMANDS, RECOLOR -> drawButtonForOtherMenus(holder, player, context);
        }
    }

    private void drawButtonForListMenu(OrionMenuHolder holder, Player player, BrushVisualContext context) {
        var builder = new ItemBuilder(Material.NAME_TAG)
                .name(commonGuiConfig.nav().getCenterListName())
                .glow();

        if (context.hasBrush()) {
            builder.lore(commonGuiConfig.nav().getCenterSettingsLore());
            // Убираем хардкод §7
            builder.addLore(Palette.LORE_TEXT + "(" + context.title() + ")");

            holder.addButton(GuiLayout.SLOT_NAV_CENTER, new GuiMenuButton(buildSanitizedItem(builder), event -> {
                guiWindowManager.openBrushSettings(player);
            }));
        } else {
            builder.lore(commonGuiConfig.nav().getCenterListLore());

            holder.addButton(GuiLayout.SLOT_NAV_CENTER, new GuiMenuButton(buildSanitizedItem(builder), event -> {
                guiWindowManager.openBrushSelector(player);
            }));
        }
    }

    private void drawButtonForSettingsMenu(OrionMenuHolder holder, Player player, BrushVisualContext context) {
        var builder = new ItemBuilder(context.icon())
                .name(context.title())
                .lore(commonGuiConfig.nav().getCenterListLore()) // "Нажмите для списка"
                .glow();

        holder.addButton(GuiLayout.SLOT_NAV_CENTER, new GuiMenuButton(buildSanitizedItem(builder), event -> {
            guiWindowManager.openBrushSelector(player);
        }));
    }

    private void drawButtonForOtherMenus(OrionMenuHolder holder, Player player, BrushVisualContext context) {
        MenuType target = sessionManager.getSession(player).getLastToolsMenuType();

        if (target == MenuType.SETTINGS && !context.hasBrush()) {
            target = MenuType.LIST;
        }

        if (target == MenuType.SETTINGS) {
            var builder = new ItemBuilder(context.icon())
                    .name(context.title())
                    .lore(commonGuiConfig.nav().getCenterSettingsLore());

            holder.addButton(GuiLayout.SLOT_NAV_CENTER, new GuiMenuButton(buildSanitizedItem(builder), event -> {
                guiWindowManager.openBrushSettings(player);
            }));
        } else {
            var builder = new ItemBuilder(Material.NAME_TAG)
                    .name(commonGuiConfig.nav().getCenterListName())
                    .lore(commonGuiConfig.nav().getCenterListLore());

            holder.addButton(GuiLayout.SLOT_NAV_CENTER, new GuiMenuButton(buildSanitizedItem(builder), event -> {
                guiWindowManager.openBrushSelector(player);
            }));
        }
    }

    // ========================================================================
    // CONTEXT & ITEM FACTORY HELPERS
    // ========================================================================

    private BrushVisualContext resolveBrushContext(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return new BrushVisualContext(false, Material.BARRIER, commonGuiConfig.nav().getCenterSettingsName("Unknown"));
        }

        // [REFACTOR] Получаем активный стейт одним вызовом (O(1))
        BrushState state = stateManager.resolveActiveState(player, hand.getType());
        if (state == null) {
            return new BrushVisualContext(false, Material.BARRIER, commonGuiConfig.nav().getCenterSettingsName("Unknown"));
        }

        // [FEATURE CUTTING] Делегируем определение имени самому стейту через полиморфизм
        String brushName = state.getGuiIdentifier();
        String brushTitle = commonGuiConfig.nav().getCenterSettingsName(brushName);
        return new BrushVisualContext(true, hand.getType(), brushTitle);
    }

    private ItemStack buildSanitizedItem(ItemBuilder builder) {
        ItemStack item = builder.build();
        GuiItemService.hideAttributes(item);
        return item;
    }

    /**
     * DTO для изоляции визуальных свойств активной кисти в руке игрока.
     */
    private record BrushVisualContext(boolean hasBrush, Material icon, String title) {}
}