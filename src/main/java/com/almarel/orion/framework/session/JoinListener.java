// TYPE B: FULL FILE
package com.almarel.orion.framework.session;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.notification.NotificationDigestService;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import com.almarel.orion.framework.storage.LoadOutcome;
import com.almarel.orion.framework.storage.StorageManager;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * [AI CONTEXT] Отвечает ТОЛЬКО за оркестровку входа игрока: запуск загрузки
 * данных и вызов доменных JoinRestoreHook в детерминированном порядке ПОСЛЕ
 * успешной загрузки. Листенер НЕ знает о конкретных доменных фичах (кисти,
 * скорость и т.д.) — каждая фича регистрирует свой JoinRestoreHook в
 * МОДУЛЕ СВОЕГО ДОМЕНА (см. BrushModule, PlayerModule, PresetModule).
 *
 * Хуки выполняются в try-catch по отдельности — сбой одного домена не
 * должен блокировать восстановление остальных (Fault Isolation).
 *
 * ЕДИНСТВЕННОЕ ИСКЛЮЧЕНИЕ, намеренно НЕ являющееся JoinRestoreHook:
 * BufferLifecycleService.clearAllClipboards() — выполняется ДО загрузки
 * данных (edge-case защита quick quit->rejoin), это неотъемлемая часть
 * ветки needsLoad именно этого метода, а не доменное пост-восстановление —
 * вынос в хук нарушил бы семантику интерфейса ("вызывается ПОСЛЕ загрузки").
 */
public class JoinListener implements Listener {

    private static final long JOIN_PROCESS_DELAY_TICKS = 4L;

    private final JavaPlugin plugin;
    private final StorageManager storageManager;
    private final OrionSessionManager orionSessionManager;
    private final NotificationDigestService notificationService;
    private final TaskRunner taskRunner;

    private final List<JoinRestoreHook> sortedHooks;

    @Inject
    public JoinListener(JavaPlugin plugin,
                        StorageManager storageManager,
                        OrionSessionManager orionSessionManager,
                        Set<JoinRestoreHook> joinRestoreHooks,
                        NotificationDigestService notificationService,
                        TaskRunner taskRunner) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.orionSessionManager = orionSessionManager;
        this.notificationService = notificationService;
        this.taskRunner = taskRunner;
        this.sortedHooks = joinRestoreHooks.stream()
                .sorted(Comparator.comparingInt(JoinRestoreHook::priority))
                .toList();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final OrionSessionManager.SessionResult result = orionSessionManager.getSessionWithResult(player.getUniqueId());

        if (storageManager == null) {
            plugin.getLogger().severe("StorageManager is null! Cannot load data for " + player.getName());
            finalizeJoinLogic(player);
            return;
        }

        taskRunner.runSyncLater(
                () -> processDeferredJoin(player, result),
                JOIN_PROCESS_DELAY_TICKS
        );
    }

    private void processDeferredJoin(Player player, OrionSessionManager.SessionResult result) {
        if (!player.isOnline()) {
            return;
        }

        orionSessionManager.updateCachedLimits(player);

        if (isDataLoadRequired(player.getUniqueId(), result.isNew())) {
            initiateStateLoading(player, result.getSession());
        } else {
            finalizeJoinLogic(player);
        }
    }

    private boolean isDataLoadRequired(UUID uuid, boolean isFreshSession) {
        return isFreshSession || !orionSessionManager.isDataLoaded(uuid);
    }

    private void initiateStateLoading(Player player, OrionPlayerSession session) {
        // [EDGE CASE FIX] Покрывает случай быстрого quit->rejoin ДО завершения
        // первой загрузки: сессия уже существовала в памяти (isFreshSession=false),
        // но данные так и не были применены. Без этой проверки правки игрока
        // во второй сессии не сохранялись бы вплоть до AFK-выгрузки.

        storageManager.loadPlayerState(player, session, outcome -> handleLoadOutcome(player, outcome));
    }

    private void handleLoadOutcome(Player player, LoadOutcome outcome) {
        if (outcome != LoadOutcome.FAILED) {
            orionSessionManager.markDataLoaded(player.getUniqueId());
        } else {
            plugin.getLogger().warning("[1b1v] Data load failed for " + player.getName() +
                    ". Auto-save disabled for this session to prevent data loss. " +
                    "Player should re-join to retry loading.");
        }
        finalizeJoinLogic(player);
    }

    /**
     * Выполняет финальную настройку игрока строго в Main Thread после
     * загрузки сессии: доменные JoinRestoreHook + нотификации.
     */
    private void finalizeJoinLogic(Player player) {
        if (!player.isOnline()) {
            return;
        }

        executeRestoreHooks(player);
        notificationService.onPlayerJoin(player);
    }

    private void executeRestoreHooks(Player player) {
        for (JoinRestoreHook hook : sortedHooks) {
            try {
                hook.onJoin(player);
            } catch (Exception e) {
                plugin.getLogger().severe("[JoinRestoreHook] " + hook.getClass().getSimpleName()
                        + " failed for " + player.getName() + ": " + e.getMessage());
            }
        }
    }
}