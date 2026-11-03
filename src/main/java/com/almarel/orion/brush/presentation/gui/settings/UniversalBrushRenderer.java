package com.almarel.orion.brush.presentation.gui.settings;

import com.almarel.orion.brush.data.presentation.TitledBrush;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.presentation.gui.settings.widgets.grid.DynamicGridBuilder;
import com.almarel.orion.brush.services.BrushRotationService;
import com.almarel.orion.framework.gui.data.enums.BrushTab;
import com.almarel.orion.framework.gui.data.GuiLayout;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.events.OrionGuiNavigationEvent;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.brush.presentation.gui.api.TabProvider;
import com.almarel.orion.brush.presentation.gui.api.WidgetProvider;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import com.almarel.orion.brush.presentation.gui.selector.BrushDescriptionService;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import com.almarel.orion.brush.presentation.gui.settings.widgets.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import com.almarel.orion.framework.gui.GuiRegistry;
import com.almarel.orion.framework.gui.widgets.WidgetTabPanel;
import com.almarel.orion.framework.gui.widgets.WidgetFilter;

/**
 * Ядро отрисовки GUI настроек.
 * Очищено от God-Object зависимостей. Теперь это чистый холст,
 * делегирующий специфичную логику провайдерам (IoC).
 */
public class UniversalBrushRenderer {

    private final ConfigurationGuiStore configStore;
    private final GuiMaterialListMenu maskMenu;
    private final FaweIntegrationService faweIntegrationService;
    private final BrushDescriptionService descriptionService;
    private final MessageManager messageManager;
    private final GuiStateManager sessionManager;
    private final Provider<GuiRegistry> registryProvider;
    private final BrushRotationService brushRotationService;

    @Inject
    public UniversalBrushRenderer(GuiStateManager sessionManager,
                                  ConfigurationGuiStore configStore,
                                  GuiMaterialListMenu maskMenu,
                                  FaweIntegrationService faweIntegrationService,
                                  BrushDescriptionService descriptionService,
                                  MessageManager messageManager,
                                  Provider<GuiRegistry> registryProvider,
                                  BrushRotationService brushRotationService) {
        this.configStore = configStore;
        this.maskMenu = maskMenu;
        this.faweIntegrationService = faweIntegrationService;
        this.descriptionService = descriptionService;
        this.messageManager = messageManager;
        this.sessionManager = sessionManager;
        this.registryProvider = registryProvider;
        this.brushRotationService = brushRotationService;
    }

    public void draw(com.almarel.orion.framework.gui.common.OrionMenuHolder holder, BrushState state, org.bukkit.entity.Player player) {
        // [FIX] Вместо провайдера отправляем событие на обновление/возврат в настройки
        Runnable refreshCallback = () -> {
            OrionGuiNavigationEvent event = new OrionGuiNavigationEvent(player, MenuType.SETTINGS, state);
            Bukkit.getPluginManager().callEvent(event);
        };

        UniversalBrushController ubc = new UniversalBrushController(
                state, player, faweIntegrationService, maskMenu, configStore.getCommon(), messageManager, refreshCallback, brushRotationService
        );

        org.bukkit.inventory.Inventory menu = holder.getInventory();
        for (int i = GuiLayout.ROW_SIZE; i < GuiLayout.NAV_BAR_START; i++) menu.setItem(i, null);

        GuiSession session = sessionManager.getSession(player);
        BrushTab currentTab = drawTabNavigation(holder, player, state, session, refreshCallback);

        drawContentByTab(holder, state, player, ubc, session, currentTab, refreshCallback);
    }

    private void drawSettingsContent(OrionMenuHolder holder, BrushState state, Player player, UniversalBrushController ubc, Runnable refreshCallback) {
        DynamicGridBuilder grid = new DynamicGridBuilder(state);

        // Все провайдеры (включая наш новый StandardBrushWidgetProvider) сами внедряют свои кнопки в сетку
        for (WidgetProvider provider : registryProvider.get().getWidgetProviders()) {
            provider.inject(holder, state, player, ubc, grid, refreshCallback);
        }

        // Рендерим итоговую сетку
        grid.commit();
    }

    private BrushTab drawTabNavigation(OrionMenuHolder holder, Player player, BrushState state, GuiSession session, Runnable refreshCallback) {

        return BrushTab.SETTINGS;
    }

    private void drawContentByTab(OrionMenuHolder holder, BrushState state, Player player, UniversalBrushController ubc, GuiSession session, BrushTab currentTab, Runnable refreshCallback) {
        if (BrushTab.SETTINGS.equals(currentTab)) {
            drawSettingsContent(holder, state, player, ubc, refreshCallback);
            drawBrushInfo(holder, state, player, session, refreshCallback);
        }
    }

    private void drawBrushInfo(OrionMenuHolder holder, BrushState state, Player player, GuiSession session, Runnable refreshCallback) {
        boolean isExpanded = session.isFormulaExpanded();
        WidgetInfoIcon.drawBrushDetails(holder, player, state, descriptionService, configStore.getCommon(), isExpanded, (event) -> {
            if (event.getClick() == ClickType.MIDDLE) {
                session.setFormulaExpanded(!session.isFormulaExpanded());
            }
            refreshCallback.run();
        });
    }
}