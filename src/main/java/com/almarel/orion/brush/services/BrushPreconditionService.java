package com.almarel.orion.brush.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.EnumSet;

@Singleton
public class BrushPreconditionService {

    private final MessageManager messageManager;
    private final BrushStateManager stateManager;
    private final FaweIntegrationService faweIntegrationService;

    private static final EnumSet<Material> BLACKLISTED_TOOLS = EnumSet.of(Material.ARROW, Material.GUNPOWDER);

    @Inject
    public BrushPreconditionService(MessageManager messageManager,
                                    BrushStateManager stateManager,
                                    FaweIntegrationService faweIntegrationService) {
        this.messageManager = messageManager;
        this.stateManager = stateManager;
        this.faweIntegrationService = faweIntegrationService;
    }

    /**
     * Выполняет полную валидацию перед привязкой кисти.
     * Проверяет права, инструмент (на воздух, черный список, блоки),
     * а затем очищает старые стейты Orion и FAWE.
     *
     * @return Material инструмента, если всё успешно. null, если проверка провалена (сообщение уже отправлено).
     */
    @Nullable
    public Material prepareTool(Player p, @Nullable String requiredPermission, boolean strictBlockCheck) {
        if (requiredPermission != null && !p.hasPermission(requiredPermission)) {
            messageManager.send(p, OrionMessage.CMD_NO_PERM);
            return null;
        }

        Material tool = p.getInventory().getItemInMainHand().getType();

        if (tool == Material.AIR) {
            messageManager.send(p, OrionMessage.BRUSH_BIND_AIR);
            return null;
        }

        if (BLACKLISTED_TOOLS.contains(tool)) {
            messageManager.send(p, OrionMessage.BRUSH_CANT_USE);
            return null;
        }

        if (strictBlockCheck && tool.isBlock()) {
            messageManager.send(p, OrionMessage.BRUSH_BIND_AIR);
            return null;
        }

        // Отказ от Middle Man: чистим напрямую в обоих сервисах
        stateManager.removeBrushSettings(p, tool);
        faweIntegrationService.unbindFaweBrushSilently(p, tool);

        return tool;
    }
}