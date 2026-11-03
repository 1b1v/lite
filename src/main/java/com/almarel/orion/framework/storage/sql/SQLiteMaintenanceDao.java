package com.almarel.orion.framework.storage.sql;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class SQLiteMaintenanceDao {

    private final DatabaseService db;

    @Inject
    public SQLiteMaintenanceDao(
            DatabaseService db
    ) {
        this.db = db;
    }

    public Map<String, Integer> countUntranscodedSchematics() {
        Map<String, Integer> counts = new HashMap<>();
        // WAL режим позволяет нам читать базу без блокировки writeLock
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT module_key, json_data FROM player_data WHERE module_key IN ('clipboard_main', 'brush_buffers')");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String key = rs.getString("module_key");
                String json = rs.getString("json_data");
                if (json == null || json.isEmpty() || json.equals("null")) continue;

                try {
                    JsonElement root = JsonParser.parseString(json);
                    if ("clipboard_main".equals(key) && root.isJsonArray()) {
                        extractCountsFromArray(root.getAsJsonArray(), counts);
                    } else if ("brush_buffers".equals(key) && root.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                            if (entry.getValue().isJsonArray()) {
                                extractCountsFromArray(entry.getValue().getAsJsonArray(), counts);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Игнорируем битый JSON у конкретного игрока
                }
            }
        } catch (SQLException e) {
            // Игнорируем ошибки БД при старте
        }
        return counts;
    }

    private void extractCountsFromArray(JsonArray arr, Map<String, Integer> counts) {
        for (JsonElement el : arr) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("untranscoded") && obj.get("untranscoded").getAsBoolean()) {
                    if (obj.has("source_schem")) {
                        String path = obj.get("source_schem").getAsString();
                        counts.merge(path, 1, Integer::sum);
                    }
                }
            }
        }
    }

    public void cleanupExpiredPlayers(int cacheTtlDays, int profileTtlDays) {
        if (cacheTtlDays > 0) {
            List<UUID> cacheExpired = getExpiredUUIDs(cacheTtlDays);
            if (!cacheExpired.isEmpty()) {
                boolean dbSuccess = executeWriteTransaction(conn -> deleteCacheData(conn, cacheExpired));
                if (dbSuccess) {
                    purgePhysicalCaches(cacheExpired);
                }
            }
        }

        if (profileTtlDays > 0) {
            List<UUID> profileExpired = getExpiredUUIDs(profileTtlDays);
            if (!profileExpired.isEmpty()) {
                boolean dbSuccess = executeWriteTransaction(conn -> deleteProfileData(conn, profileExpired));
                if (dbSuccess) {
                    purgePhysicalCaches(profileExpired);
                }
            }
        }
    }

    @FunctionalInterface
    private interface SQLTransaction {
        void execute(Connection conn) throws SQLException;
    }

    private boolean executeWriteTransaction(SQLTransaction action) {
        db.getWriteLock().lock();
        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);
            try {
                action.execute(conn);
                conn.commit();
                return true;
            } catch (SQLException ex) {
                if (conn != null) conn.rollback();
            }
        } catch (SQLException e) {
            // TODO: Add logger
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {}
            }
            db.getWriteLock().unlock();
        }
        return false;
    }

    private void deleteCacheData(Connection conn, List<UUID> uuids) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM player_data WHERE uuid = ? AND module_key IN ('clipboard_main', 'brush_buffers')")) {
            for (UUID u : uuids) {
                ps.setString(1, u.toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteProfileData(Connection conn, List<UUID> uuids) throws SQLException {
        String[] tables = {"brushes", "macros", "player_data", "players"};
        for (String table : tables) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE uuid = ?")) {
                for (UUID u : uuids) {
                    ps.setString(1, u.toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private void purgePhysicalCaches(List<UUID> uuids) {
        for (UUID u : uuids) {
        }
    }

    private List<UUID> getExpiredUUIDs(int days) {
        List<UUID> expired = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM players WHERE last_updated <= datetime('now', ?)")) {
            ps.setString(1, "-" + days + " days");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expired.add(UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            // TODO: Add logger
        }
        return expired;
    }
}