package com.almarel.orion.framework.integration.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.data.OrionEdition;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Initializes bStats metrics collection.
 * <p>
 * Plugin ID depends on the build: LITE maps to the public "1b1v" bStats project,
 * everything else (technical edition = full, published to users as 1b1v Pro)
 * maps to the "1b1v-pro" bStats project.
 * <p>
 * Resolved via the single {@link OrionEdition} source of truth (runtime enum,
 * FULL/LITE only) — there is no separate "pro" build variant to branch on here;
 * "pro" is purely the public name for the full-edition build (see
 * Orion_Infrastructure.txt, section 0 — Naming Conventions).
 */
@Singleton
public class BStatsService {

    private static final int PRO_BSTATS_ID = 34272;
    private static final int LITE_BSTATS_ID = 33796;

    private final JavaPlugin plugin;
    private final OrionEdition edition;

    @Inject
    public BStatsService(JavaPlugin plugin, OrionEdition edition) {
        this.plugin = plugin;
        this.edition = edition;
    }

    public void start() {
        int pluginId = resolvePluginId();

        if (pluginId <= 0) {
            plugin.getLogger().warning("[Metrics] bStats plugin-id is missing or invalid. Metrics disabled.");
            return;
        }

        new Metrics(plugin, pluginId);
        plugin.getLogger().fine("[Metrics] bStats initialized (ID: " + pluginId + ", edition: " + edition + ").");
    }

    private int resolvePluginId() {
        return edition == OrionEdition.LITE ? LITE_BSTATS_ID : PRO_BSTATS_ID;
    }
}