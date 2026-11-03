package com.almarel.orion.brush.presentation.gui.settings;

import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.framework.gui.data.enums.BrushTab;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.brush.services.BrushSyncService;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.NavBarRenderer;
import com.almarel.orion.framework.gui.GuiWindowManager;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.data.GuiLayout;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.events.OrionGuiNavigationEvent;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.inject.Inject;

@Singleton
public class BrushSettingsRouter implements Listener {

    private final BrushStateManager stateManager;
    private final BrushSyncService brushSyncService;
    private final GuiWindowManager guiWindowManager;
    private final UniversalBrushRenderer universalRenderer;
    private final NavBarRenderer navigationService;
    private final GuiStateManager sessionManager;

    // Удали поле private final Provider<BrushMacroSelectorMenu> macroSelectorProvider;

    @Inject
    public BrushSettingsRouter(JavaPlugin plugin,
                               BrushStateManager stateManager,
                               BrushSyncService brushSyncService,
                               GuiWindowManager guiWindowManager,
                               UniversalBrushRenderer universalRenderer
            ,NavBarRenderer navigationService,
                               GuiStateManager sessionManager) { // [FIX] Убрали Provider
        this.stateManager = stateManager;
        this.brushSyncService = brushSyncService;
        this.guiWindowManager = guiWindowManager;
        this.universalRenderer = universalRenderer;
        this.navigationService = navigationService;
        this.sessionManager = sessionManager;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNavigationRequest(OrionGuiNavigationEvent event) {
        if (event.getTargetMenu() == MenuType.SETTINGS) {
            if (event.getContext() instanceof BrushState state) {
                open(event.getPlayer(), state);
            } else {
                open(event.getPlayer());
            }
        }
    }



    public void open(Player player) {
        open(player, null);
    }

    public void open(Player player, BrushState targetState) {
        BrushState activeState = targetState;

        // 1. Поиск состояния кисти, если не передано явно
        if (activeState == null) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() != Material.AIR) {
                activeState = stateManager.resolveActiveState(player, heldItem.getType());
            }
        }

        // 2. Если кисть не найдена -> Перенаправляем в Селектор
        if (activeState == null) {
            guiWindowManager.openBrushSelector(player);
            return;
        }

        // 3. Memory-First Sync (Синхронизация FAWE перед рендером)
        brushSyncService.syncActiveTool(player);

        // 4. Определение заголовка меню (Строгий FDD + Динамика вкладок)
        GuiSession session = sessionManager.getSession(player);
        String title = resolveMenuTitle(activeState, session);

        // 5. Создание инвентаря
        OrionMenuHolder holder = new OrionMenuHolder();
        Inventory menu = Bukkit.createInventory(holder, GuiLayout.ROWS_6, title);
        holder.setInventory(menu);

        // 6. Отрисовка статической навигации
        drawBorders(menu, player);

        // 7. Универсальный Data-Driven Рендер
        universalRenderer.draw(holder, activeState, player);

        // 8. Открытие инвентаря
        player.openInventory(menu);
    }

    private void drawBorders(Inventory menu, Player player) {
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) menu.setItem(i, grayGlass);
        for (int i = 45; i < 54; i++) {
            if (i == 48 || i == 49 || i == 50 || i == 52) continue;
            menu.setItem(i, grayGlass);
        }
        navigationService.draw(menu, player, MenuType.SETTINGS);
    }

    /**
     * Безопасное получение заголовка меню.
     * Динамически меняется в зависимости от активной вкладки и идентификатора кисти.
     */
    private String resolveMenuTitle(BrushState state, GuiSession session) {
        BrushTab currentTab = session.getActiveBrushTab();

        {
            // [FIX] Используем getGuiIdentifier() для получения точного имени кисти (Sphere, Macro, Plump и т.д.)
            String brushName = state.getGuiIdentifier();
            return "§9Brush §a" + brushName;
        }
    }

    /**
     * Превращает строгий Enum в красивое читаемое имя.
     */
    private String formatBrushName(OrionBrushType type) { // no usage
        if (type == null || type == OrionBrushType.GENERIC) return "Settings";
        if (type == OrionBrushType.CP) return "CopyPaste";
        if (type == OrionBrushType.FAWE) return "FAWE";

        String name = type.name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public void openMacroSelector(Player player, BrushState state) { // no usage
        // [FIX] Вместо Provider кричим в Event-Bus, передавая BrushState как context
        OrionGuiNavigationEvent event =
                new OrionGuiNavigationEvent(player, MenuType.BRUSH_MACRO_SELECTOR, state);
        Bukkit.getPluginManager().callEvent(event);
    }
}