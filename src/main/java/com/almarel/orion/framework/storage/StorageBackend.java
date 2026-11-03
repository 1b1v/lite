package com.almarel.orion.framework.storage;

import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;

import java.util.UUID;

/**
 * Интерфейс для низкоуровневого сохранения данных сессии.
 * Скрывает детали реализации БД (Core CRUD).
 *
 * [AI CONTEXT] Три независимых пути сохранения соответствуют трем разным
 * циклам жизни данных:
 * - saveFullSnapshot: AFK-выгрузка / Graceful Shutdown. Атомарно, игнорирует dirty.
 * - saveLightSnapshot: периодический автосейв кистей/макросов/конструктов (частый, дешевый).
 * - saveClipboardSnapshot: периодический автосейв буфера (редкий, вызывается ТОЛЬКО
 *   если вызывающий код убедился, что SessionClipboardData.isDirty() == true).
 */
public interface StorageBackend {

    void saveFullSnapshot(UUID uuid, String playerName, OrionSnapshot snapshot);

    void saveLightSnapshot(UUID uuid, String playerName, OrionSnapshot snapshot);

    void saveClipboardSnapshot(UUID uuid, OrionSnapshot snapshot);

    /**
     * Загружает снимок данных игрока.
     * @return Snapshot, или null если у игрока действительно нет сохраненных данных
     *         (легитимный новый игрок).
     * @throws StorageLoadException если произошла ошибка чтения (БД недоступна,
     *         повреждены данные и т.д.). [CRITICAL] Реализация ОБЯЗАНА пробрасывать
     *         эту ошибку, а не возвращать null молча — иначе вызывающий код не сможет
     *         отличить "новый игрок" от "сбой чтения", что создаёт риск перезаписи
     *         существующих данных пустым снапшотом при следующем автосохранении.
     */
    OrionSnapshot loadSnapshot(UUID uuid);
}