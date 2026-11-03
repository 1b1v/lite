package com.almarel.orion.framework.session;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.Orion;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import com.almarel.orion.framework.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Оркестратор-посредник.
 * Связывает In-Memory хранилище (SessionManager) и Диск (StorageManager).
 * Отвечает за:
 * 1. Выгрузку AFK/Idle игроков (полное сохранение + удаление из RAM).
 * 2. Периодическое частичное автосохранение АКТИВНЫХ (не-idle) сессий:
 *    - Light-цикл (кисти/макросы/конструкты/история) — часто, безусловно.
 *    - Clipboard-цикл (буфер обмена) — редко, только при isDirty().
 * 3. Экстренное синхронное сохранение при остановке сервера.
 *
 * [AI CONTEXT] Все три цикла работают ИСКЛЮЧИТЕЛЬНО через OrionSessionManager
 * (Lock-Free). Запрещено обращаться к Bukkit.getOnlinePlayers() из этого класса —
 * методы выполняются в асинхронных потоках, а Player-коллекции Bukkit не
 * гарантированно потокобезопасны для чтения вне Main Thread.
 */
@Singleton
public class SessionPersistenceOrchestrator {

    private static final long EXPIRATION_TIME_MILLIS = 10 * 60 * 1000L; // 10 минут
    private static final long MONITOR_INTERVAL_TICKS = 20 * 60L;        // 1 минута

    private static final long LIGHT_SAVE_INTERVAL_TICKS = 20L * 60 * 5;   // 5 минут
    private static final long CLIPBOARD_SAVE_INTERVAL_TICKS = 20L * 60 * 20; // 20 минут

    private static final long LIGHT_SAVE_STAGGER_MILLIS = 150L;     // задержка между игроками
    private static final long CLIPBOARD_SAVE_STAGGER_MILLIS = 400L; // задержка между игроками

    private final Orion plugin;
    private final OrionSessionManager orionSessionManager;
    private final StorageManager storageManager;
    private final TaskRunner taskRunner;

    private BukkitTask monitorTask;
    private BukkitTask lightSaveTask;

    @Inject
    public SessionPersistenceOrchestrator(
            Orion plugin,
            OrionSessionManager orionSessionManager,
            StorageManager storageManager,
            TaskRunner taskRunner
    ) {
        this.plugin = plugin;
        this.orionSessionManager = orionSessionManager;
        this.storageManager = storageManager;
        this.taskRunner = taskRunner;
    }

    public void start() {
        startAfkMonitor();
        startLightSaveCycle();
    }

    
    // CYCLE 1: AFK MONITOR
    

    private void startAfkMonitor() {
        this.monitorTask = taskRunner.runAsyncTimer(
                this::processAfkSessions,
                MONITOR_INTERVAL_TICKS,
                MONITOR_INTERVAL_TICKS
        );
    }

    private void processAfkSessions() {
        if (!plugin.isEnabled()) return;

        List<UUID> idleSessions = orionSessionManager.getIdleSessions(EXPIRATION_TIME_MILLIS);
        for (UUID uuid : idleSessions) {
            if (orionSessionManager.hasSession(uuid)) {
                unloadAndSaveSession(uuid, "AFK session");
            }
        }
    }

    
    // CYCLE 2: LIGHT PERIODIC SAVE (brushes/macros/constructs/history)
    

    private void startLightSaveCycle() {
        this.lightSaveTask = taskRunner.runAsyncTimer(
                this::processLightSaveCycle,
                LIGHT_SAVE_INTERVAL_TICKS,
                LIGHT_SAVE_INTERVAL_TICKS
        );
    }

    private void processLightSaveCycle() {
        if (!plugin.isEnabled()) return;
        runStaggered(orionSessionManager.getOnlineSessions(), LIGHT_SAVE_STAGGER_MILLIS, this::saveLightSnapshotSafely);
    }

    private void saveLightSnapshotSafely(UUID uuid) {
        OrionPlayerSession session = orionSessionManager.getSessionIfPresent(uuid);
        if (!isEligibleForSnapshot(uuid, session)) return;

        String playerName = getPlayerName(uuid);
        try {
            storageManager.saveLightSnapshotSync(uuid, playerName, session);
        } catch (Exception e) {
            plugin.getLogger().warning("[Periodic Light-Save] Failed for " + playerName + ": " + e.getMessage());
        }
    }

    
    // CYCLE 3: CLIPBOARD PERIODIC SAVE (dirty-checked)
    


    
    // SHUTDOWN & FLUSH
    

    /**
     * Вызывается исключительно из Main Thread при остановке плагина.
     * Синхронно сохраняет оставшихся игроков в базу, чтобы не потерять данные
     * при убийстве Bukkit'ом асинхронных потоков.
     */
    public void shutdownSync() {
        stopAllTasks();
        flushRemainingSessions();
    }

    private void stopAllTasks() {
        cancelTask(monitorTask);
        cancelTask(lightSaveTask);
    }

    private void flushRemainingSessions() {
        Set<UUID> remainingSessions = orionSessionManager.getAllActiveSessions();
        for (UUID uuid : remainingSessions) {
            if (orionSessionManager.hasSession(uuid)) {
                unloadAndSaveSession(uuid, "shutdown");
            }
        }
    }

    
    // UNLOAD & EVICITON LOGIC
    

    private void unloadAndSaveSession(UUID uuid, String logContext) {
        OrionPlayerSession session = orionSessionManager.getSessionIfPresent(uuid);
        if (session == null) return; // Сессия уже была удалена параллельным циклом — нечего делать

        String playerName = getPlayerName(uuid);

        try {
            persistSessionSync(uuid, playerName, session, logContext);
        } finally {
            evictSessionResources(uuid, session);
        }
    }

    private void persistSessionSync(UUID uuid, String playerName, OrionPlayerSession session, String logContext) {
        try {
            if (orionSessionManager.isDataLoaded(uuid)) {
                storageManager.saveSessionSync(uuid, playerName, session);
            } else {
                plugin.getLogger().warning("[Session Unload] Skipped save for " + playerName +
                        " (" + logContext + "): data was never fully loaded from storage. " +
                        "Existing DB data preserved as-is.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save session (" + logContext + ") for " + playerName);
            plugin.getLogger().severe("Error: " + e.getMessage());
        }
    }

    private void evictSessionResources(UUID uuid, OrionPlayerSession session) {
        orionSessionManager.removeSession(uuid);
    }

    
    // PRIVATE HELPERS
    

    private boolean isEligibleForSnapshot(UUID uuid, OrionPlayerSession session) {
        return session != null && orionSessionManager.isDataLoaded(uuid);
    }

    private void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Выполняет action последовательно для каждого UUID с задержкой между
     * итерациями, чтобы размазать нагрузку на БД во времени и не создавать
     * пиковую пачку одновременных транзакций при большом онлайне.
     */
    private void runStaggered(List<UUID> uuids, long delayMillisBetween, Consumer<UUID> action) {
        for (UUID uuid : uuids) {
            if (!plugin.isEnabled()) return; // Сервер начал остановку — прерываем батч
            action.accept(uuid);
            sleepQuietly(delayMillisBetween);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getPlayerName(UUID uuid) {
        return orionSessionManager.getLastKnownName(uuid);
    }
}