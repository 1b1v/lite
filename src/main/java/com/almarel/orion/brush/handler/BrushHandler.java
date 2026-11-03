package com.almarel.orion.brush.handler;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public interface BrushHandler {
    boolean isActive(Player player, Material tool);
    boolean onLeftClick(Player player, PlayerInteractEvent event);
    boolean onRightClick(Player player, PlayerInteractEvent event);

    /**
     * Вызывается адаптером FAWE, когда игрок использует кисть.
     * @param session Готовая сессия с примененными масками (/gmask).
     * @param target Целевой блок (рассчитан FAWE).
     */
    default void onFaweBuild(Player player, EditSession session, BlockVector3 target, com.sk89q.worldedit.function.pattern.Pattern pattern, double size) {
        // По умолчанию ничего не делает, чтобы не заставлять реализовывать этот метод во всех кистях сразу
    }
}