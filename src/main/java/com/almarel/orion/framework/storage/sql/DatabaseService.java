package com.almarel.orion.framework.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.almarel.orion.Orion; // Импорт твоего главного класса (для логгера и папки)

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

import com.google.inject.Inject;

/**
 * Сервис управления базой данных SQLite.
 * Отвечает за:
 * 1. Создание пула соединений (HikariCP).
 * 2. Инициализацию таблиц при старте.
 * 3. Корректное закрытие соединения.
 */
public class DatabaseService {

    private final Orion plugin;
    private HikariDataSource dataSource;
    private final ReentrantLock writeLock = new ReentrantLock(true);

    @Inject
    public DatabaseService(Orion plugin) {
        this.plugin = plugin;
    }

    public ReentrantLock getWriteLock() {
        return writeLock;
    }

    /**
     * Инициализация базы данных.
     */
    public void initialize() {
        File folder = new File(plugin.getDataFolder(), "storage");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // [SILENCE HIKARICP - LOG4J2 METHOD]
        // Обращаемся напрямую к ядру логирования Minecraft (Log4j2)
        try {
            org.apache.logging.log4j.core.config.Configurator.setLevel("com.zaxxer.hikari", org.apache.logging.log4j.Level.WARN);
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("Could not silence HikariCP (Log4j2 not found).");
        }

        setupDataSource(folder);

        // 3. Создаем таблицы
        createTables();
    }

    private void setupDataSource(File folder) {
        // 2. Настройка HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(folder, "1b1v_data.db").getAbsolutePath());
        config.setPoolName("1b1v-SQLite-Pool");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // [CRITICAL FIX] Настройка SQLite для многопоточности (WAL Mode)
        // WAL позволяет читать данные параллельно с записью. busy_timeout заставит потоки ждать освобождения файла вместо краша.
        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=30000;");

        // Оптимизации SQLite
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Получение соединения из пула.
     * Используй это в try-with-resources!
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DatabaseService not initialized!");
        }
        return dataSource.getConnection();
    }

    /**
     * Закрытие пула (при выключении плагина).
     * Включает принудительный чекпоинт WAL для защиты от потери данных.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            // Принудительно сливаем WAL-файл в основной .db перед закрытием.
            // Уровень FINE: не засоряет консоль по умолчанию, но доступен при увеличенной verbosity логов.
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                plugin.getLogger().fine("Executing SQLite WAL checkpoint...");
                stmt.execute("PRAGMA wal_checkpoint(TRUNCATE);");
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to execute WAL checkpoint: " + e.getMessage());
            }

            plugin.getLogger().fine("Closing HikariCP database pool...");
            dataSource.close();
        }
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            createPlayerSchema(conn);
            createSharedGroupSchema(conn);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables!");
            plugin.getLogger().severe("Error: " + e.getMessage());
        }
    }

    private void createPlayerSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Таблица игроков
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(32), " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Таблица настроек кистей (Key-Value JSON store per brush)
            stmt.execute("CREATE TABLE IF NOT EXISTS brushes (" +
                    "uuid VARCHAR(36), " +
                    "brush_key VARCHAR(64), " +
                    "json_data TEXT, " +
                    "PRIMARY KEY (uuid, brush_key), " +
                    "FOREIGN KEY(uuid) REFERENCES players(uuid) ON DELETE CASCADE)");

            // Таблица макросов
            stmt.execute("CREATE TABLE IF NOT EXISTS macros (" +
                    "uuid VARCHAR(36), " +
                    "macro_name VARCHAR(64), " +
                    "json_data TEXT, " +
                    "PRIMARY KEY (uuid, macro_name), " +
                    "FOREIGN KEY(uuid) REFERENCES players(uuid) ON DELETE CASCADE)");

            // Таблица для остальных больших блобов (Clipboard, ConstructGroups)
            stmt.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36), " +
                    "module_key VARCHAR(32), " +
                    "json_data TEXT, " +
                    "PRIMARY KEY (uuid, module_key), " +
                    "FOREIGN KEY(uuid) REFERENCES players(uuid) ON DELETE CASCADE)");
        }
    }

    private void createSharedGroupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // --- Shared Groups System ---
            stmt.execute("CREATE TABLE IF NOT EXISTS shared_groups (" +
                    "group_id VARCHAR(36) PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "group_name VARCHAR(64) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS shared_group_members (" +
                    "group_id VARCHAR(36) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "PRIMARY KEY (group_id, player_uuid), " +
                    "FOREIGN KEY(group_id) REFERENCES shared_groups(group_id) ON DELETE CASCADE)");
        }
    }
}