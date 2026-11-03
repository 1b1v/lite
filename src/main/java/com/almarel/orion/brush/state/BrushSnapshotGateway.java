package com.almarel.orion.brush.state;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;
import com.almarel.orion.framework.storage.sql.LightSnapshotSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Отвечает ИСКЛЮЧИТЕЛЬНО за персистентность кистей (таблица brushes) в SQL.
 * Выделен из SQLiteStorage в рамках SRP-рефакторинга.
 *
 * [AI CONTEXT] Логика перенесена ВЕРБАТИМ (JIT Gson, без BrushStateGson.create()).
 * Известный риск десериализации полиморфных подклассов BrushState —
 * см. отдельную заметку в истории рефакторинга, требует отдельного разбора.
 */
@Singleton
public class BrushSnapshotGateway implements LightSnapshotSection {

    private static final String DELETE_QUERY = "DELETE FROM brushes WHERE uuid = ?";
    private static final String INSERT_QUERY = "INSERT INTO brushes (uuid, brush_key, json_data) VALUES (?, ?, ?)";
    private static final String SELECT_QUERY = "SELECT brush_key, json_data FROM brushes WHERE uuid = ?";

    private final Gson gson;

    @Inject
    public BrushSnapshotGateway(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void save(Connection conn, String uuidStr, OrionSnapshot snapshot) throws SQLException {
        deleteExistingBrushes(conn, uuidStr);
        if (hasBrushes(snapshot.brushesData)) {
            insertBrushesBatch(conn, uuidStr, snapshot.brushesData);
        }
    }

    private void deleteExistingBrushes(Connection conn, String uuidStr) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_QUERY)) {
            ps.setString(1, uuidStr);
            ps.executeUpdate();
        }
    }

    private boolean hasBrushes(Map<String, BrushState> data) {
        return data != null && !data.isEmpty();
    }

    private void insertBrushesBatch(Connection conn, String uuidStr, Map<String, BrushState> brushesData) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_QUERY)) {
            for (Map.Entry<String, BrushState> entry : brushesData.entrySet()) {
                BrushState state = entry.getValue();
                if (state != null) {
                    addBatchRecord(ps, uuidStr, entry.getKey(), state);
                }
            }
            ps.executeBatch();
        }
    }

    private void addBatchRecord(PreparedStatement ps, String uuidStr, String key, BrushState state) throws SQLException {
        ps.setString(1, uuidStr);
        ps.setString(2, key);
        ps.setString(3, gson.toJson(state));
        ps.addBatch();
    }

    @Override
    public boolean load(Connection conn, String uuidStr, OrionSnapshot snapshot) throws SQLException {
        snapshot.brushesData = new HashMap<>();
        return loadBrushesFromDb(conn, uuidStr, snapshot.brushesData);
    }

    private boolean loadBrushesFromDb(Connection conn, String uuidStr, Map<String, BrushState> targetMap) throws SQLException {
        boolean foundAny = false;
        try (PreparedStatement ps = conn.prepareStatement(SELECT_QUERY)) {
            ps.setString(1, uuidStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    foundAny = true;
                    processBrushRow(rs, targetMap);
                }
            }
        }
        return foundAny;
    }

    private void processBrushRow(ResultSet rs, Map<String, BrushState> targetMap) throws SQLException {
        String dbKey = rs.getString("brush_key");
        String json = rs.getString("json_data");
        BrushState brush = parseBrushState(json);
        if (brush != null) {
            targetMap.put(dbKey, brush);
        }
    }

    private BrushState parseBrushState(String json) {
        try {
            return gson.fromJson(json, BrushState.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}