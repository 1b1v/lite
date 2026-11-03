package com.almarel.orion.brush.presentation.gui.selector;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит BrushSelectorSession на игрока между открытиями меню селектора.
 * По образцу RecolorSessionStore (recolor.presentation.gui) — см. его
 * javadoc для обоснования паттерна. В отличие от Recolor, здесь нет
 * привязки к региону: сессия создаётся лениво и живёт до выхода игрока.
 */
@Singleton
public class BrushSelectorSessionStore implements Listener {

    private final Map<UUID, BrushSelectorSession> sessions = new ConcurrentHashMap<>();

    @Inject
    public BrushSelectorSessionStore() {
    }

    public BrushSelectorSession getOrCreate(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> new BrushSelectorSession());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }
}