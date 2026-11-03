package com.almarel.orion.framework.storage.sql;

import com.google.inject.Singleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Единственное место с SQL-запросами к generic key-value таблице player_data
 * (module_key -> json_data). Используется всеми "generic blob" гейтвеями
 * (Construct, CommandHistory, Clipboard*) вместо копипасты PreparedStatement.
 * Brush/Macro НЕ используют этот репозиторий — у них выделенные таблицы
 * (brushes, macros) с собственной схемой.
 */
@Singleton
public class PlayerDataRepository {

    public void upsert(Connection conn, String uuid, String moduleKey, String json) throws SQLException {
        if (json == null || json.equals("null")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO player_data (uuid, module_key, json_data) VALUES (?, ?, ?) " +
                        "ON CONFLICT(uuid, module_key) DO UPDATE SET json_data = ?")) {
            ps.setString(1, uuid);
            ps.setString(2, moduleKey);
            ps.setString(3, json);
            ps.setString(4, json);
            ps.executeUpdate();
        }
    }

    /** @return json_data, либо null если строки нет (модуль не сохранялся для этого игрока). */
    public String select(Connection conn, String uuid, String moduleKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT json_data FROM player_data WHERE uuid = ? AND module_key = ?")) {
            ps.setString(1, uuid);
            ps.setString(2, moduleKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("json_data") : null;
            }
        }
    }
}