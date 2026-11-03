package com.almarel.orion.framework.storage.sql;

import com.google.inject.Inject;
import com.almarel.orion.framework.storage.StorageBackend;
import com.almarel.orion.framework.storage.StorageLoadException;
import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

/**
 * Тонкий оркестратор транзакций и реестра игроков (таблица players).
 * НЕ знает деталей сериализации доменов (Brush/Macro/Construct/History/Clipboard) —
 * делегирует их в LightSnapshotSection / ClipboardSnapshotSection реализации
 * (см. OrionCoreModule.provideLightSnapshotSections / provideClipboardSnapshotSections).
 *
 * [AI CONTEXT] Результат SRP-рефакторинга: раньше здесь жила бизнес-логика
 * сериализации всех доменов сразу (см. историю бага с потерей
 * brushActiveIndices при ручной сборке meta-Map). Теперь каждый домен —
 * отдельный класс со своей ответственностью, легко тестируемый в изоляции.
 */
public class SQLiteStorage implements StorageBackend {

    private final DatabaseService db;
    private final Set<LightSnapshotSection> lightSections;
    private final Set<ClipboardSnapshotSection> clipboardSections;

    @Inject
    public SQLiteStorage(DatabaseService db,
                         Set<LightSnapshotSection> lightSections,
                         Set<ClipboardSnapshotSection> clipboardSections) {
        this.db = db;
        this.lightSections = lightSections;
        this.clipboardSections = clipboardSections;
    }

    @Override
    public void saveFullSnapshot(UUID uuid, String playerName, OrionSnapshot s) {
        String uuidStr = uuid.toString();
        db.getWriteLock().lock();
        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            upsertPlayerRow(conn, uuidStr, playerName);
            writeLightSections(conn, uuidStr, s);
            writeClipboardSections(conn, uuidStr, uuid, s, true);

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn, playerName);
        } finally {
            closeQuietly(conn);
            db.getWriteLock().unlock();
        }
    }

    @Override
    public void saveLightSnapshot(UUID uuid, String playerName, OrionSnapshot s) {
        String uuidStr = uuid.toString();
        db.getWriteLock().lock();
        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            upsertPlayerRow(conn, uuidStr, playerName);
            writeLightSections(conn, uuidStr, s);

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn, playerName);
        } finally {
            closeQuietly(conn);
            db.getWriteLock().unlock();
        }
    }

    @Override
    public void saveClipboardSnapshot(UUID uuid, OrionSnapshot s) {
        String uuidStr = uuid.toString();
        db.getWriteLock().lock();
        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            writeClipboardSections(conn, uuidStr, uuid, s, false);

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn, uuidStr);
        } finally {
            closeQuietly(conn);
            db.getWriteLock().unlock();
        }
    }

    @Override
    public OrionSnapshot loadSnapshot(UUID uuid) {
        String uuidStr = uuid.toString();
        OrionSnapshot s = new OrionSnapshot();
        boolean foundAny = false;

        try (Connection conn = db.getConnection()) {
            for (LightSnapshotSection section : lightSections) {
                foundAny |= section.load(conn, uuidStr, s);
            }
            for (ClipboardSnapshotSection section : clipboardSections) {
                foundAny |= section.load(conn, uuidStr, uuid, s);
            }
        } catch (SQLException e) {
            // [CRITICAL] Пробрасываем наверх — StorageManager/JoinListener должны
            // отличить "новый игрок" от "реальный сбой чтения".
            throw new StorageLoadException("Failed to load snapshot for uuid=" + uuidStr, e);
        }

        return foundAny ? s : null;
    }

    private void writeLightSections(Connection conn, String uuidStr, OrionSnapshot s) throws SQLException {
        for (LightSnapshotSection section : lightSections) {
            section.save(conn, uuidStr, s);
        }
    }

    private void writeClipboardSections(Connection conn, String uuidStr, UUID uuid, OrionSnapshot s, boolean forceSave) throws SQLException {
        for (ClipboardSnapshotSection section : clipboardSections) {
            section.save(conn, uuidStr, uuid, s, forceSave);
        }
    }

    private void upsertPlayerRow(Connection conn, String uuidStr, String playerName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO players (uuid, username, last_updated) VALUES (?, ?, CURRENT_TIMESTAMP) " +
                        "ON CONFLICT(uuid) DO UPDATE SET username = ?, last_updated = CURRENT_TIMESTAMP")) {
            ps.setString(1, uuidStr);
            ps.setString(2, playerName);
            ps.setString(3, playerName);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection conn, String context) {
        if (conn == null) return;
        try {
            System.out.println("Error saving data for " + context + ". Rolling back.");
            conn.rollback();
        } catch (SQLException ignored) {}
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ignored) {}
    }
}