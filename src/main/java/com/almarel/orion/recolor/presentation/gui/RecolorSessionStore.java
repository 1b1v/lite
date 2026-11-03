package com.almarel.orion.recolor.presentation.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит активную RecolorGuiSession на игрока между открытиями //recolor.
 *
 * Жизненный цикл: сессия создаётся/заменяется в
 * RecolorMenuPresenter.getOrCreateSession при первом открытии команды или
 * при изменении границ выделения; удаляется только при выходе игрока
 * с сервера. НЕ очищается при простом закрытии инвентаря (Esc) — это
 * осознанное требование: пары "было/станет" должны сохраняться между
 * открытиями меню, пока регион не изменился.
 *
 * ConcurrentHashMap используется не ради потокобезопасности мутаций
 * отдельной сессии (они все происходят в main thread), а как общий
 * контейнер на случай параллельных открытий разными игроками —
 * конвенция проекта (см. GuiStateManager).
 */
@Singleton
public class RecolorSessionStore implements Listener {

    private final Map<UUID, RecolorGuiSession> sessions = new ConcurrentHashMap<>();

    @Inject
    public RecolorSessionStore() {
    }

    @Nullable
    public RecolorGuiSession getExisting(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void put(Player player, RecolorGuiSession session) {
        sessions.put(player.getUniqueId(), session);
    }

    public void remove(Player player) {
        sessions.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }
}