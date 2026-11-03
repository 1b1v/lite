package com.almarel.orion.framework.integration.versioncheck;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.UUID;

/**
 * Per-player on/off switch for update/promotion notifications.
 * Distinct from UpdateNotificationCooldownService: this is a manual mute
 * triggered explicitly by a player via "/1b1v notice", not an automatic
 * anti-spam cooldown.
 */
@Singleton
public class UpdateNotificationSnoozeService {

    private static final long SNOOZE_DURATION_MILLIS = 60L * 24 * 60 * 60 * 1000; // 60 days

    private final TaskRunner taskRunner;
    private final UpdateStateRepository stateRepository;

    @Inject
    public UpdateNotificationSnoozeService(TaskRunner taskRunner, UpdateStateRepository stateRepository) {
        this.taskRunner = taskRunner;
        this.stateRepository = stateRepository;
    }

    /**
     * Fast, synchronous, main-thread-safe check against the in-memory snapshot.
     */
    public boolean isSnoozed(UUID playerId) {
        Long until = stateRepository.snapshot().getSnoozedUntilPerPlayer().get(playerId.toString());
        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * Silences all update/promotion notifications for this player for 60 days. Persists asynchronously.
     */
    public void snoozeFor60Days(UUID playerId) {
        long until = System.currentTimeMillis() + SNOOZE_DURATION_MILLIS;
        taskRunner.runAsync(() ->
                stateRepository.update(state -> state.getSnoozedUntilPerPlayer().put(playerId.toString(), until))
        );
    }

    /**
     * Immediately re-enables notifications for this player. Persists asynchronously.
     */
    public void clearSnooze(UUID playerId) {
        taskRunner.runAsync(() ->
                stateRepository.update(state -> state.getSnoozedUntilPerPlayer().remove(playerId.toString()))
        );
    }
}