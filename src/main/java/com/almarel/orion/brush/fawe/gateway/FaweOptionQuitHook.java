package com.almarel.orion.brush.fawe.gateway;

import com.almarel.orion.framework.session.QuitCleanupHook;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

/**
 * Инвалидирует кэш FAWE-опций игрока при выходе. Выделен из
 * SessionLifecycleListener (SRP) — см. QuitCleanupHook.
 */
@Singleton
public class FaweOptionQuitHook implements QuitCleanupHook {

    private final FaweOptionWriter faweOptionWriter;

    @Inject
    public FaweOptionQuitHook(FaweOptionWriter faweOptionWriter) {
        this.faweOptionWriter = faweOptionWriter;
    }

    @Override
    public void onQuit(Player player) {
        faweOptionWriter.invalidatePlayerCache(player.getUniqueId());
    }
}