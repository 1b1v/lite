package com.almarel.orion.framework.gui.session;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;

/**
 * Отвечает за жизненный цикл GuiSession.
 * Создает сессию при первом обращении, удаляет при выходе.
 */
@Singleton
public class GuiStateManager implements Listener {

    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    @Inject
    public GuiStateManager(
            JavaPlugin plugin) {
    }

    public void cleanupPlayer(UUID uuid) {
        sessions.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().closeInventory();
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    public GuiSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> {
            File rootDir = null;
            File favDir = null;
            return new GuiSession(rootDir, favDir);
        });
    }

}