package com.almarel.orion.brush.fawe.adapters;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.BrushManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Универсальный динамический адаптер для всех кистей Orion.
 * Перенаправляет триггеры строительства из FAWE в ядро Orion.
 */
public class OrionBrushAdapter implements Brush {

    private final BrushManager brushManager;
    private final OrionBrushType type; // [NEW] Заменяет необходимость плодить классы

    public OrionBrushAdapter(BrushManager brushManager, OrionBrushType type) {
        this.brushManager = brushManager;
        this.type = type;
    }

    /**
     * Позволяет узнать, какая именно кисть Orion сейчас привязана к этому адаптеру.
     */
    public OrionBrushType getType() {
        return type;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        Actor actor = editSession.getActor();
        if (actor == null || !actor.isPlayer()) {
            return;
        }

        CommandSender sender = BukkitAdapter.adapt(actor);
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;

        // Вызываем универсальный менеджер (он сам разберется, какой Handler активировать)
        brushManager.handleFaweBuild(player, editSession, position, pattern, size);
    }
}