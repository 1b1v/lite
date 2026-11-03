package com.almarel.orion.brush.input;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.Rangeable;
import com.almarel.orion.brush.state.BrushStateManager;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import com.google.inject.Inject;

/**
 * Сервис для взаимодействия с миром (RayTrace, TargetBlock).
 * Инкапсулирует логику поиска блока, на который смотрит игрок.
 */
public class InteractionService {

    private final BrushStateManager brushStateManager;

    // TODO: Inject BrushStateManager here (update instantiation in your Dependency Injection setup)
    @Inject
    public InteractionService(BrushStateManager brushStateManager) {
        this.brushStateManager = brushStateManager;
    }

    /**
     * Определяет дистанцию для рейтрейса.
     * Memory-First: Читает закэшированные данные из состояния кисти.
     */
    public double getRayTraceDistance(Player p, double defaultDist) {
        Material material = p.getInventory().getItemInMainHand().getType();

        if (material.isAir()) {
            return defaultDist;
        }

        BrushState state = brushStateManager.resolveActiveState(p, material);

        if (state instanceof Rangeable rangeable) {
            double range = rangeable.getRange();
            if (range > 0) {
                return range;
            }
        }

        return defaultDist;
    }

    /**
     * Возвращает точку (BlockVector3), в которую смотрит игрок.
     * Если блок не найден в пределах distance, возвращает точку в воздухе.
     */
    public com.sk89q.worldedit.math.BlockVector3 getRayTracePoint(Player p, double distance) {
        RayTraceResult rr = p.rayTraceBlocks(distance, FluidCollisionMode.NEVER);
        if (rr != null && rr.getHitBlock() != null) {
            return BukkitAdapter.asBlockVector(rr.getHitBlock().getRelative(rr.getHitBlockFace()).getLocation());
        }

        org.bukkit.Location loc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(distance));
        return BukkitAdapter.asBlockVector(loc);
    }

    /**
     * Получает целевой блок с учетом клика мыши.
     * Если был клик по блоку (ПКМ), возвращает его (с учетом грани).
     * Иначе делает рейтрейс.
     */
    public com.sk89q.worldedit.math.BlockVector3 getTargetBlockPos(Player p, double distance, PlayerInteractEvent e) {
        if (e != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            return BukkitAdapter.asBlockVector(e.getClickedBlock().getRelative(e.getBlockFace()).getLocation());
        }
        RayTraceResult rr = p.rayTraceBlocks(distance, FluidCollisionMode.NEVER);
        if (rr != null && rr.getHitBlock() != null) {
            return BukkitAdapter.asBlockVector(rr.getHitBlock().getRelative(rr.getHitBlockFace()).getLocation());
        }
        return null;
    }

    /**
     * Получает сам блок Bukkit API.
     */
    public Block getTargetBlock(Player p, double distance, PlayerInteractEvent e) {
        if (e != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            return e.getClickedBlock();
        }
        RayTraceResult rr = p.rayTraceBlocks(distance, FluidCollisionMode.NEVER);
        return (rr != null) ? rr.getHitBlock() : null;
    }
}