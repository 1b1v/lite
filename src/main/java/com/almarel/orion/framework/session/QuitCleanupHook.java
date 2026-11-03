package com.almarel.orion.framework.session;

import org.bukkit.entity.Player;

/**
 * Хук очистки доменного состояния игрока ПРИ выходе (см.
 * SessionLifecycleListener.onPlayerQuit). Каждая фича регистрирует свой
 * хук через Multibinder<QuitCleanupHook> В МОДУЛЕ СВОЕГО ДОМЕНА —
 * SessionLifecycleListener не содержит знаний о конкретных фичах.
 *
 * Конвенция priority(): отрицательное значение — "должен выполниться раньше",
 * положительное — "должен выполниться позже", default 0 — "порядок с другими
 * хуками осознанно не важен" (независимые домены). Переопределять priority()
 * ТОЛЬКО при реальной выявленной зависимости между конкретными хуками —
 * не проставлять "на всякий случай".
 */
public interface QuitCleanupHook {

    default int priority() {
        return 0;
    }

    void onQuit(Player player);
}