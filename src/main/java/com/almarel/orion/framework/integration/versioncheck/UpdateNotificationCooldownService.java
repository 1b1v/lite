package com.almarel.orion.framework.integration.versioncheck;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.UUID;

/**
 * Decides whether a given cooldown key (player + notification type) should
 * be shown again, based on a persistent per-key cooldown that survives restarts.
 * Knows nothing about HTTP or manifests — see UpdateCheckService for that.
 */
@Singleton
public class UpdateNotificationCooldownService {

    private static final long NOTIFY_COOLDOWN_MILLIS = 12L * 60 * 60 * 1000; // 12 hours

    private final TaskRunner taskRunner;
    private final UpdateStateRepository stateRepository;

    @Inject
    public UpdateNotificationCooldownService(TaskRunner taskRunner, UpdateStateRepository stateRepository) {
        this.taskRunner = taskRunner;
        this.stateRepository = stateRepository;
    }

    /**
     * Fast, synchronous, main-thread-safe check — reads only the in-memory snapshot.
     */
    public boolean shouldShow(String cooldownKey) {
        Long lastNotified = stateRepository.snapshot().getLastNotifiedPerKey().get(cooldownKey);
        if (lastNotified == null) {
            return true;
        }
        return (System.currentTimeMillis() - lastNotified) >= NOTIFY_COOLDOWN_MILLIS;
    }

    /**
     * Marks the key as notified "now". Persists asynchronously to avoid blocking the caller.
     */
    public void markShown(String cooldownKey) {
        taskRunner.runAsync(() ->
                stateRepository.update(state ->
                        state.getLastNotifiedPerKey().put(cooldownKey, System.currentTimeMillis())
                )
        );
    }

    /**
     * Помечает ключ показанным и одновременно удаляет любые другие устаревшие
     * записи с тем же префиксом (тот же игрок + тип уведомления, но другая версия).
     * Предотвращает неограниченный рост update-check-state.json с каждым новым релизом.
     */
    public void markShownAndPruneStale(String cooldownKey, String prunePrefix) {
        taskRunner.runAsync(() ->
                stateRepository.update(state -> {
                    var map = state.getLastNotifiedPerKey();
                    map.put(cooldownKey, System.currentTimeMillis());
                    map.keySet().removeIf(key -> key.startsWith(prunePrefix) && !key.equals(cooldownKey));
                })
        );
    }

    /**
     * Удаляет ВСЕ cooldown-записи показа для игрока, независимо от типа
     * уведомления. Вызывается при ручном включении через "/1b1v notice" —
     * иначе игрок мог бы явно "включить" уведомления, но не увидеть их
     * из-за оставшегося окна кулдауна, выставленного ДО того, как он их выключил.
     */
    public void clearForPlayer(UUID playerId) {
        String marker = playerId.toString();
        taskRunner.runAsync(() ->
                stateRepository.update(state ->
                        state.getLastNotifiedPerKey().keySet().removeIf(key -> key.contains(marker))
                )
        );
    }
}