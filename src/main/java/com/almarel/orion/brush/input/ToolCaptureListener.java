package com.almarel.orion.brush.input;

import com.almarel.orion.brush.data.BrushState;
import com.google.inject.Singleton;
import com.almarel.orion.brush.services.BrushSyncService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import com.google.inject.Inject;

@Singleton
public class ToolCaptureListener implements Listener {

    private final BrushSyncService brushSyncService;
    private final com.almarel.orion.brush.state.BrushStateManager brushStateManager; // [NEW]

    @Inject
    public ToolCaptureListener(BrushSyncService brushSyncService,
                               com.almarel.orion.brush.state.BrushStateManager brushStateManager) { // [NEW]
        this.brushSyncService = brushSyncService;
        this.brushStateManager = brushStateManager; // [NEW]
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSwitch(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        ItemStack oldItem = player.getInventory().getItem(e.getPreviousSlot());

        if (oldItem != null && oldItem.getType() != Material.AIR) {
            brushSyncService.savePassiveTool(player, oldItem);
        }

        ItemStack newItem = player.getInventory().getItem(e.getNewSlot());
        if (newItem != null && newItem.getType() != Material.AIR) {
            BrushState state = brushStateManager.resolveActiveState(player, newItem.getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent e) {
        // Если игрок выбросил кисть, нужно сохранить её состояние перед тем, как она покинет инвентарь
        ItemStack droppedItem = e.getItemDrop().getItemStack();
        if (droppedItem.getType() != Material.AIR) {
            brushSyncService.savePassiveTool(e.getPlayer(), droppedItem);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Если игрок выходит с сервера с кистью в руке, сохраняем её состояние в БД
        Player player = e.getPlayer();
        ItemStack currentItem = player.getInventory().getItemInMainHand();

        if (currentItem.getType() != Material.AIR) {
            brushSyncService.savePassiveTool(player, currentItem);
        }
    }
}