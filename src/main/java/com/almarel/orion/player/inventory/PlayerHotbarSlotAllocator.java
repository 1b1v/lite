package com.almarel.orion.player.inventory;

import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Домен-агностичная утилита размещения предмета в хотбаре игрока.
 * Ничего не знает о кистях — чистая Bukkit-инвентарная механика.
 * Вынесена из BrushSelectorService (см. аудит selector-пакета):
 * эта логика не имела отношения к GUI или к домену Brush.
 */
@Singleton
public class PlayerHotbarSlotAllocator {

    public void equip(Player player, Material tool) {
        if (tool == null || tool == Material.AIR) {
            return;
        }

        PlayerInventory inv = player.getInventory();

        Integer existingSlot = findSlotWithItem(inv, tool);
        if (existingSlot != null) {
            inv.setHeldItemSlot(existingSlot);
            return;
        }

        Integer emptySlot = findEmptyHotbarSlot(inv);
        if (emptySlot != null) {
            inv.setItem(emptySlot, new ItemStack(tool, 1));
            inv.setHeldItemSlot(emptySlot);
            return;
        }

        safeSetSlot(player, inv, inv.getHeldItemSlot(), tool);
    }

    public void placeInSlot(Player player, Material tool, int slot) {
        if (tool == null || tool == Material.AIR) {
            return;
        }
        safeSetSlot(player, player.getInventory(), slot, tool);
    }

    private void safeSetSlot(Player player, PlayerInventory inv, int slot, Material tool) {
        ItemStack currentItem = inv.getItem(slot);
        inv.setItem(slot, new ItemStack(tool, 1));

        if (currentItem != null && currentItem.getType() != Material.AIR) {
            dropOverflowItems(player, inv.addItem(currentItem));
        }
    }

    private Integer findSlotWithItem(PlayerInventory inv, Material tool) {
        for (int i = 0; i <= 8; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == tool) {
                return i;
            }
        }
        return null;
    }

    private Integer findEmptyHotbarSlot(PlayerInventory inv) {
        for (int i = 0; i <= 8; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return i;
            }
        }
        return null;
    }

    private void dropOverflowItems(Player player, Map<Integer, ItemStack> leftover) {
        if (leftover == null || leftover.isEmpty()) {
            return;
        }
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }
}