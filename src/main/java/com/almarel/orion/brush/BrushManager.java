package com.almarel.orion.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.almarel.orion.brush.handler.BrushHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.inject.Inject;

public class BrushManager {

    private final List<BrushHandler> handlers = new CopyOnWriteArrayList<>();

    @Inject
    public BrushManager(Set<BrushHandler> injectedHandlers) {
        this.handlers.addAll(injectedHandlers);
    }

    public boolean handleLeftClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return false;
        Player p = e.getPlayer();
        Material tool = p.getInventory().getItemInMainHand().getType();

        for (BrushHandler handler : handlers) {
            if (handler.isActive(p, tool)) {
                if (handler.onLeftClick(p, e)) return true;
            }
        }
        return false;
    }

    public boolean handleRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return false;
        Player p = e.getPlayer();
        Material tool = p.getInventory().getItemInMainHand().getType();

        for (BrushHandler handler : handlers) {
            if (handler.isActive(p, tool)) {
                if (handler.onRightClick(p, e)) return true;
            }
        }
        return false;
    }

    public void handleFaweBuild(Player p, EditSession session, BlockVector3 target, Pattern pattern, double size) {
        Material tool = p.getInventory().getItemInMainHand().getType();
        for (BrushHandler handler : handlers) {
            if (handler.isActive(p, tool)) {
                handler.onFaweBuild(p, session, target, pattern, size);
                return;
            }
        }
    }
}