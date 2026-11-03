package com.almarel.orion.framework.storage.sql;

import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Контракт модуля снапшота буфера обмена. Отдельный от LightSnapshotSection,
 * т.к. требует UUID (для ClipboardStateSerializer/файлового кэша) и флаг
 * forceSave (см. Справка 2, Dirty-Tracking & Partial Persistence Contract).
 */
public interface ClipboardSnapshotSection {

    void save(Connection conn, String uuidStr, UUID uuid, OrionSnapshot snapshot, boolean forceSave) throws SQLException;

    /** @return true, если для игрока найдены данные этого модуля в БД. */
    boolean load(Connection conn, String uuidStr, UUID uuid, OrionSnapshot snapshot) throws SQLException;
}