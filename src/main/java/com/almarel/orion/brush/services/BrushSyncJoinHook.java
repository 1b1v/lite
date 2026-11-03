package com.almarel.orion.brush.services;

import com.almarel.orion.framework.session.JoinRestoreHook;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

/**
 * Восстанавливает активный инструмент (кисть) игрока в руке после входа.
 * Выделен из JoinListener (SRP) — см. JoinRestoreHook.
 */
@Singleton
public class BrushSyncJoinHook implements JoinRestoreHook {

    private final BrushSyncService brushSyncService;

    @Inject
    public BrushSyncJoinHook(BrushSyncService brushSyncService) {
        this.brushSyncService = brushSyncService;
    }

    @Override
    public void onJoin(Player player) {
        brushSyncService.restoreActiveTool(player, player.getInventory().getItemInMainHand());
    }
}