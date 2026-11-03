package com.almarel.orion.brush.services;

import com.almarel.orion.framework.session.QuitCleanupHook;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

/**
 * Синхронизирует активный инструмент игрока в постоянное хранилище при
 * выходе. Выделен из SessionLifecycleListener (SRP) — см. QuitCleanupHook.
 */
@Singleton
public class BrushSyncQuitHook implements QuitCleanupHook {

    private final BrushSyncService brushSyncService;

    @Inject
    public BrushSyncQuitHook(BrushSyncService brushSyncService) {
        this.brushSyncService = brushSyncService;
    }

    @Override
    public void onQuit(Player player) {
        brushSyncService.syncActiveTool(player);
    }
}