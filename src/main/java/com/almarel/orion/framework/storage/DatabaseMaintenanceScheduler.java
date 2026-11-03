package com.almarel.orion.framework.storage;

import com.almarel.orion.framework.data.MainConfig;
import com.almarel.orion.framework.storage.sql.SQLiteMaintenanceDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Периодическая чистка устаревших данных игроков в БД по TTL (profile TTL —
 * всегда, cache TTL — только если присутствует Clipboard-модуль). Выделена
 * из StorageManager (SRP) — задача планирования обслуживания БД не связана
 * с задачей "сохранить/загрузить сессию игрока".
 */
@Singleton
public class DatabaseMaintenanceScheduler {

    private static final long DELAY_TICKS = 20L * 60 * 5;
    private static final long PERIOD_TICKS = 20L * 60 * 60 * 24;

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final SQLiteMaintenanceDao maintenanceDao;

    private BukkitTask cleanupTask;

    @Inject
    public DatabaseMaintenanceScheduler(JavaPlugin plugin,
                                        MainConfig mainConfig,
                                        SQLiteMaintenanceDao maintenanceDao
    ) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        this.maintenanceDao = maintenanceDao;
    }

    public void start() {
        if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
            return;
        }

        this.cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int cacheTtl = 0;


            int profileTtl = mainConfig.getProfileTtlDays();

            if (cacheTtl > 0 || profileTtl > 0) {
                maintenanceDao.cleanupExpiredPlayers(cacheTtl, profileTtl);
            }
        }, DELAY_TICKS, PERIOD_TICKS);
    }

    public void shutdown() {
        if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
            this.cleanupTask.cancel();
            this.cleanupTask = null;
        }
    }
}