package com.almarel.orion.brush.handler;

import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage; // [NEW] Import
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Базовый абстрактный класс для всех хендлеров кистей.
 * Содержит общие зависимости и вспомогательные методы.
 */
public abstract class BaseBrushHandler implements BrushHandler {

    protected final JavaPlugin plugin;
    protected final BrushStateManager stateManager;
    protected final MessageManager messageManager;

    public BaseBrushHandler(JavaPlugin plugin,
                            BrushStateManager stateManager,
                            MessageManager messageManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.messageManager = messageManager;
    }

    /**
     * Проверяет права игрока.
     * Если прав нет, автоматически отправляет сообщение об ошибке.
     * @return true, если у игрока ЕСТЬ права.
     */
    protected boolean checkPermission(Player player, String permissionNode) {
        if (!player.hasPermission(permissionNode)) {
            // [CHANGED] Используем Enum
            messageManager.send(player, OrionMessage.CMD_NO_PERM);
            return false;
        }
        return true;
    }
}