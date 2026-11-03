package com.almarel.orion.brush.services;

import com.almarel.orion.brush.OrionBrushRegistry;
import com.almarel.orion.brush.api.BrushFeature;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.presentation.gui.selector.BrushSelectorPresenter.BrushViewItem;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.brush.presentation.gui.selector.BrushSelectorSession.BrushFilter;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Singleton
public class BrushQueryService {

    private final BrushStateManager brushStateManager;
    private final BrushSyncService brushSyncService;
    private final OrionBrushRegistry orionBrushRegistry;
    private final Gson gson;

    @Inject
    public BrushQueryService(BrushStateManager brushStateManager,
                             BrushSyncService brushSyncService,
                             OrionBrushRegistry orionBrushRegistry,
                             Gson gson) {
        this.brushStateManager = brushStateManager;
        this.brushSyncService = brushSyncService;
        this.orionBrushRegistry = orionBrushRegistry;
        this.gson = gson;
    }

    public List<BrushViewItem> getMergedBrushes(Player player, BrushFilter filter) {
        // JIT Синхронизация перед отрисовкой
        brushSyncService.syncActiveTool(player);

        if (filter == BrushFilter.PRESETS) {
            List<BrushViewItem> presetBrushes = new ArrayList<>();
            return presetBrushes;
        }

        return getPersonalBrushes(player);
    }


    private List<BrushViewItem> getPersonalBrushes(Player player) {
        List<BrushViewItem> result = new ArrayList<>();
        for (Object obj : brushStateManager.getAllActiveBrushes(player)) {
            if (obj instanceof BrushState state && hasAccess(player, state)) {
                result.add(createPersonalViewItem(state));
            }
        }
        return result;
    }

    private boolean hasAccess(Player player, BrushState state) {
        String brushId = state.getBrushType().name().toLowerCase(Locale.ROOT);
        BrushFeature<?> feature = orionBrushRegistry.get(brushId);
        return feature == null || feature.hasPermission(player);
    }

    private BrushViewItem createPersonalViewItem(BrushState state) {
        // Для личных кистей presetWrapper = null
        return new BrushViewItem(state
        );
    }
}