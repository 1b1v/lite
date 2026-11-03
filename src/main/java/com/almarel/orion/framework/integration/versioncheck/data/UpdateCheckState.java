package com.almarel.orion.framework.integration.versioncheck.data;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent state stored on disk (update-check-state.json).
 * Survives server restarts. Owned exclusively by UpdateStateRepository.
 *
 * Both maps use String keys (not UUID objects) because Gson does not cleanly
 * serialize complex object keys without enableComplexMapKeySerialization(),
 * which is not configured for this project's Gson instance.
 */
@Getter
@Setter
public class UpdateCheckState {
    private long lastFetchAtEpochMillis = 0L;
    private VersionManifest cachedManifest = null;

    // Key format: "<notificationType>:<playerUUID>" — see UpdateNotificationCooldownService.
    private ConcurrentHashMap<String, Long> lastNotifiedPerKey = new ConcurrentHashMap<>();

    // Key: playerUUID.toString(), Value: epoch millis until which notifications are muted for that player.
    private ConcurrentHashMap<String, Long> snoozedUntilPerPlayer = new ConcurrentHashMap<>();
}