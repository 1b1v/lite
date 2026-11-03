// TYPE B: FULL FILE
package com.almarel.orion.framework.integration.versioncheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.integration.versioncheck.data.VersionManifest;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/**
 * Owns the network-fetch lifecycle for the update manifest.
 * Responsible ONLY for deciding when to hit the network and caching the result.
 * Knows nothing about players or chat — see notification classes for that.
 */
@Singleton
public class UpdateCheckService {

    private static final long FETCH_COOLDOWN_MILLIS = 5 * 60 * 1000L; // 5 min
    private static final long FETCH_INTERVAL_TICKS = 12L * 60 * 60 * 20; // 12 hours, in ticks

    private final JavaPlugin plugin;
    private final UpdateManifestHttpClient httpClient;
    private final UpdateStateRepository stateRepository;

    @Inject
    public UpdateCheckService(JavaPlugin plugin,
                              UpdateManifestHttpClient httpClient,
                              UpdateStateRepository stateRepository) {
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.stateRepository = stateRepository;
    }

    /**
     * Starts the background check cycle. Call once during plugin startup.
     * All work runs off the main thread.
     */
    public void start() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            stateRepository.loadFromDisk();
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::performCheckCycle, 0L, FETCH_INTERVAL_TICKS);
        });
    }

    /**
     * @return the last successfully fetched manifest, or null if none is available yet.
     */
    public VersionManifest getCachedManifest() {
        return stateRepository.snapshot().getCachedManifest();
    }

    private void performCheckCycle() {
        if (isFetchCooldownActive()) {
            return;
        }

        try {
            fetchAndCacheManifest();
        } catch (Exception ex) {
            plugin.getLogger().warning("[1b1v] Update check failed: " + ex.getMessage());
        }
    }

    private boolean isFetchCooldownActive() {
        long now = System.currentTimeMillis();
        long lastFetch = stateRepository.snapshot().getLastFetchAtEpochMillis();
        return (now - lastFetch) < FETCH_COOLDOWN_MILLIS;
    }

    private VersionManifest fetchAndCacheManifest() throws Exception {
        VersionManifest manifest = httpClient.fetchManifest();
        long now = System.currentTimeMillis();
        stateRepository.update(state -> {
            state.setCachedManifest(manifest);
            state.setLastFetchAtEpochMillis(now);
        });
        return manifest;
    }

    /**
     * Форсирует немедленный повторный запрос манифеста, игнорируя внутренний
     * 5-минутный fetch-кулдаун. Только для ручного вызова через
     * "/1b1v checkupdate" — НЕ используется во внутреннем 12-часовом цикле.
     */
    public void forceRefresh(Consumer<Boolean> onComplete) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = executeForcedFetch();
            Bukkit.getScheduler().runTask(plugin, () -> onComplete.accept(success));
        });
    }

    private boolean executeForcedFetch() {
        try {
            fetchAndCacheManifest();
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("[1b1v] Forced update check failed: " + ex.getMessage());
            return false;
        }
    }
}