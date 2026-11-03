package com.almarel.orion.framework.storage.sql;

import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Контракт "легкого" модуля снапшота (кисти, макросы, конструкты, история).
 * Реализации инжектируются списком в SQLiteStorage (Direct Constructor
 * Injection, без Multibinder — см. OrionCoreModule.provideLightSnapshotSections).
 */
public interface LightSnapshotSection {

    void save(Connection conn, String uuidStr, OrionSnapshot snapshot) throws SQLException;

    /** @return true, если для игрока найдены данные этого модуля в БД. */
    boolean load(Connection conn, String uuidStr, OrionSnapshot snapshot) throws SQLException;
}