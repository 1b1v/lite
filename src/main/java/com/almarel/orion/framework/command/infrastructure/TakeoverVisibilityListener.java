package com.almarel.orion.framework.command.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Collection;
import java.util.Map;

/**
 * Возвращает забранные имена (/mirror, /bridge) в список команд клиента.
 * FAWE (@LOW) убирает их из PlayerCommandSendEvent как «свои» (по праву worldedit.clipboard.flip /
 * arceon.tool.rope), из-за чего клиент не получает узел и красит команду красным.
 * Работаем на HIGH, после FAWE. Опираемся на то, что Paper 1.21.8 фильтрует дерево по
 * contains() в изменяемом наборе (Commands.runSync); при обновлении Paper перепроверить (D25).
 */
@Singleton
public class TakeoverVisibilityListener implements Listener {

    private final TakeoverRegistry takeoverRegistry;

    @Inject
    public TakeoverVisibilityListener(TakeoverRegistry takeoverRegistry) {
        this.takeoverRegistry = takeoverRegistry;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        Collection<String> visible = event.getCommands();
        for (Map.Entry<String, Command> entry : takeoverRegistry.view().entrySet()) {
            if (entry.getValue().testPermissionSilent(player)) {
                visible.add(entry.getKey());
            }
        }
    }
}