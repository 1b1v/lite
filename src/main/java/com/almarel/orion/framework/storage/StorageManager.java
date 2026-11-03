package com.almarel.orion.framework.storage;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import com.almarel.orion.framework.storage.snapshot.SessionSnapshotMapper;
import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Мост между In-Memory хранилищем (OrionPlayerSession) и Диском
 * (StorageBackend). Единственная ответственность — конвертация снапшотов
 * и делегирование чтения/записи в backend.
 *
 * [AI CONTEXT] Раньше здесь же жили прогрев Clipboard-кэша и планирование
 * TTL-чистки БД (SRP-нарушение) — вынесены в ClipboardCacheWarmupService и
 * DatabaseMaintenanceScheduler соответственно. Этот класс больше НЕ имеет
 * фазы start()/shutdown() — ему нечего запускать/останавливать, он просто
 * вызывается по требованию (JoinListener, SessionPersistenceOrchestrator).
 */
@Singleton
public class StorageManager {

    private final JavaPlugin plugin;
    private final TaskRunner taskRunner;
    private final SessionSnapshotMapper snapshotMapper;
    private final StorageBackend backend;

    @Inject
    public StorageManager(JavaPlugin plugin,
                          TaskRunner taskRunner,
                          SessionSnapshotMapper snapshotMapper,
                          StorageBackend backend) {
        this.plugin = plugin;
        this.taskRunner = taskRunner;
        this.snapshotMapper = snapshotMapper;
        this.backend = backend;
    }

    public void loadPlayerState(Player player, OrionPlayerSession session, Consumer<LoadOutcome> onComplete) {
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        taskRunner.runAsync(() -> {
            SnapshotFetchResult fetchResult = fetchSnapshotSafely(uuid, playerName);

            taskRunner.runSyncIfOnline(player, p -> applySnapshot(p, session, fetchResult, onComplete));
        });
    }

    // =========================================
    // LOADING HELPERS
    // =========================================

    /** Внутренний носитель результата async-чтения между двумя потоками. */
    private record SnapshotFetchResult(OrionSnapshot snapshot, boolean failed) {}

    private SnapshotFetchResult fetchSnapshotSafely(UUID uuid, String playerName) {
        try {
            OrionSnapshot snapshot = backend.loadSnapshot(uuid);
            return new SnapshotFetchResult(snapshot, false);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load data for " + playerName +
                    ". Existing DB data (if any) will be preserved untouched for this session.");
            plugin.getLogger().severe("Error: " + e.getMessage());
            return new SnapshotFetchResult(null, true);
        }
    }

    private void applySnapshot(Player player, OrionPlayerSession session, SnapshotFetchResult fetchResult, Consumer<LoadOutcome> onComplete) {
        if (fetchResult.failed()) {
            notifyCompletion(onComplete, LoadOutcome.FAILED);
            return;
        }

        if (fetchResult.snapshot() == null) {
            notifyCompletion(onComplete, LoadOutcome.NO_DATA);
            return;
        }

        try {
            snapshotMapper.applySnapshot(session, fetchResult.snapshot());
            notifyCompletion(onComplete, LoadOutcome.SUCCESS);
        } catch (Exception ex) {
            plugin.getLogger().severe("Error applying snapshot for " + player.getName() + ": " + ex.getMessage());
            notifyCompletion(onComplete, LoadOutcome.FAILED);
        }
    }

    private void notifyCompletion(Consumer<LoadOutcome> onComplete, LoadOutcome outcome) {
        if (onComplete != null) {
            onComplete.accept(outcome);
        }
    }

    // =========================================
    // SAVING API
    // =========================================

    public void saveSessionSync(UUID uuid, String playerName, OrionPlayerSession session) {
        OrionSnapshot snapshot = snapshotMapper.createSnapshot(session);
        backend.saveFullSnapshot(uuid, playerName, snapshot);
    }

    public void saveLightSnapshotSync(UUID uuid, String playerName, OrionPlayerSession session) {
        OrionSnapshot snapshot = snapshotMapper.createLightSnapshot(session);
        backend.saveLightSnapshot(uuid, playerName, snapshot);
    }

}