package com.almarel.orion.brush.presentation.gui.settings.providers;

import com.google.inject.Inject;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.ScrollActionable;
import com.almarel.orion.brush.presentation.gui.api.WidgetProvider;
import com.almarel.orion.brush.presentation.gui.settings.UniversalBrushController;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import com.almarel.orion.brush.presentation.gui.settings.widgets.grid.DynamicGridBuilder;
import com.almarel.orion.brush.presentation.gui.settings.widgets.WidgetScroll;
import org.bukkit.entity.Player;

public class ScrollToggleWidgetProvider implements WidgetProvider {

    private final GuiStateManager sessionManager; // [FIX] Заменено
    private final ConfigurationGuiStore configStore;

    @Inject
    public ScrollToggleWidgetProvider(GuiStateManager sessionManager,
                                      ConfigurationGuiStore configStore) { // [FIX] Заменено
        this.sessionManager = sessionManager;
        this.configStore = configStore;
    }

    @Override
    public void inject(OrionMenuHolder holder, BrushState state, Player player,
                       UniversalBrushController controller, DynamicGridBuilder grid, Runnable refreshCallback) {

        if (!(state instanceof ScrollActionable)) return;

        GuiSession session = sessionManager.getSession(player); // [FIX] Прямой доступ
        boolean enabled = session.isScrollModeEnabled();

        WidgetScroll.drawToggle(holder, 37, configStore.getCommon(), enabled, () -> {
            session.setScrollModeEnabled(!enabled);
            refreshCallback.run();
        });
    }
}