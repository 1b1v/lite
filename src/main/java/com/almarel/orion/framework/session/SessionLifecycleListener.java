package com.almarel.orion.framework.session;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * [AI CONTEXT] Отвечает ТОЛЬКО за оркестровку выхода игрока: вызов доменных
 * QuitCleanupHook в детерминированном порядке + финальный markIdle
 * (единственная истинная ответственность ЭТОГО класса — перевод сессии в
 * idle-состояние). Листенер НЕ знает о конкретных доменных фичах — каждая
 * фича регистрирует свой QuitCleanupHook в МОДУЛЕ СВОЕГО ДОМЕНА (см.
 * BrushModule, MacroModule, PlayerModule, ClipboardModule).
 *
 * Хуки выполняются в try-catch по отдельности — сбой одного домена
 * (например, Clipboard) не должен блокировать очистку остальных.
 */
public class SessionLifecycleListener implements Listener {

    private final OrionSessionManager orionSessionManager;
    private final List<QuitCleanupHook> sortedHooks;

    @Inject
    public SessionLifecycleListener(OrionSessionManager orionSessionManager,
                                    Set<QuitCleanupHook> quitCleanupHooks) {
        this.orionSessionManager = orionSessionManager;
        this.sortedHooks = quitCleanupHooks.stream()
                .sorted(Comparator.comparingInt(QuitCleanupHook::priority))
                .toList();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (QuitCleanupHook hook : sortedHooks) {
            try {
                hook.onQuit(player);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[QuitCleanupHook] " + hook.getClass().getSimpleName()
                        + " failed for " + player.getName() + ": " + e.getMessage());
            }
        }

        orionSessionManager.markIdle(player.getUniqueId());
    }
}