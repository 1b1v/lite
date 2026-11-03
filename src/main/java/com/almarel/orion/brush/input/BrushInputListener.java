package com.almarel.orion.brush.input;

import com.google.inject.Singleton;
import com.almarel.orion.brush.BrushManager;
import com.almarel.orion.brush.services.BrushSyncService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import com.google.inject.Inject;

@Singleton
public class BrushInputListener implements Listener {

    private final BrushManager brushManager;
    private final BrushSyncService brushSyncService;

    @Inject
    public BrushInputListener(
            BrushManager brushManager,
            BrushSyncService brushSyncService) {
        this.brushManager = brushManager;
        this.brushSyncService = brushSyncService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeftClick(PlayerInteractEvent e) {
        if (!isMainHand(e) || !isLeftClick(e.getAction())) return;

        syncActiveTool(e);

        if (brushManager.handleLeftClick(e)) {
            suppressVanillaInteraction(e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(PlayerInteractEvent e) {
        if (!isMainHand(e) || !isRightClick(e.getAction())) return;

        syncActiveTool(e);

        if (brushManager.handleRightClick(e)) {
            suppressVanillaInteraction(e);
        }
    }


    private boolean isMainHand(PlayerInteractEvent e) {
        return e.getHand() == EquipmentSlot.HAND;
    }

    private boolean isLeftClick(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    private boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private void syncActiveTool(PlayerInteractEvent e) {
        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        brushSyncService.restoreActiveTool(e.getPlayer(), item);
    }

    private void suppressVanillaInteraction(PlayerInteractEvent e) {
        e.setCancelled(true);
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
    }
}