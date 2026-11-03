package com.almarel.orion.framework.session;

import org.bukkit.entity.Player;

/**
 * Хук восстановления доменного состояния игрока ПОСЛЕ успешной загрузки
 * сессии (см. JoinListener.finalizeJoinLogic). Каждая фича регистрирует
 * свой хук через Multibinder<JoinRestoreHook> В МОДУЛЕ СВОЕГО ДОМЕНА —
 * JoinListener не содержит знаний о конкретных фичах (SRP, Open/Closed).
 *
 * Конвенция priority(): отрицательное значение — "должен выполниться раньше",
 * положительное — "должен выполниться позже", default 0 — "порядок с другими
 * хуками осознанно не важен" (независимые домены). Переопределять priority()
 * ТОЛЬКО при реальной выявленной зависимости между конкретными хуками —
 * не проставлять "на всякий случай".
 */
public interface JoinRestoreHook {

    default int priority() {
        return 0;
    }

    void onJoin(Player player);
}