package com.almarel.orion.brush.state;

import com.almarel.orion.framework.session.QuitCleanupHook;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

/**
 * Очищает in-memory состояние активных кистей игрока при выходе. Выделен
 * из SessionLifecycleListener (SRP) — см. QuitCleanupHook.
 */
@Singleton
public class BrushStateQuitHook implements QuitCleanupHook {

    private final BrushStateManager brushStateManager;

    @Inject
    public BrushStateQuitHook(BrushStateManager brushStateManager) {
        this.brushStateManager = brushStateManager;
    }

    @Override
    public void onQuit(Player player) {
        brushStateManager.cleanupSession(player.getUniqueId());
    }
}