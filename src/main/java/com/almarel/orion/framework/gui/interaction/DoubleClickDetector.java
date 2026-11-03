// TYPE B: FULL FILE
package com.almarel.orion.framework.gui.interaction;

import com.google.inject.Singleton;
import com.almarel.orion.framework.gui.session.GuiSession;

/**
 * Универсальный сервис для отслеживания двойных кликов в GUI.
 */
@Singleton
public class DoubleClickDetector {

    private static final long DEFAULT_THRESHOLD_MS = 300L;

    /**
     * Проверяет, является ли текущий клик по ключу/элементу повторным в пределах порога времени.
     *
     * @param session Сессия игрока
     * @param targetKey Уникальный идентификатор кликнутого элемента (например, StorageKey или slot)
     * @param thresholdMs Порог времени в миллисекундах
     * @return true, если зафиксирован двойной клик
     */
    public boolean isDoubleClick(GuiSession session, String targetKey, long thresholdMs) {
        if (session == null || targetKey == null) return false;

        long now = System.currentTimeMillis();
        long lastTime = session.getLastClickTime();
        String lastKey = session.getLastClickKey();

        boolean isDouble = lastKey != null
                && lastKey.equals(targetKey)
                && (now - lastTime) <= thresholdMs;

        if (isDouble) {
            // Сбрасываем ключ после успешного двойного клика, чтобы 3-й клик не засчитался за повторный double
            session.setLastClickKey(null);
            session.setLastClickTime(0);
        } else {
            session.setLastClickKey(targetKey);
            session.setLastClickTime(now);
        }

        return isDouble;
    }

    public boolean isDoubleClick(GuiSession session, String targetKey) {
        return isDoubleClick(session, targetKey, DEFAULT_THRESHOLD_MS);
    }
}