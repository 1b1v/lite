package com.almarel.orion.framework.storage;

/**
 * Оборачивает низкоуровневые ошибки чтения хранилища (SQLException и т.п.).
 * [CRITICAL] Реализации StorageBackend ОБЯЗАНЫ пробрасывать это исключение
 * вместо молчаливого return null при реальном сбое — иначе теряется различие
 * между "у игрока нет данных" и "данные есть, но не читаются".
 */
public class StorageLoadException extends RuntimeException {
    public StorageLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}