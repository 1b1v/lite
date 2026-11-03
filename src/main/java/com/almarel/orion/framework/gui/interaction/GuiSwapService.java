// TYPE B: FULL FILE
package com.almarel.orion.framework.gui.interaction;

import com.almarel.orion.framework.compat.InventoryViewCompat;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.gui.session.GuiSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Универсальный сервис для управления честным перетаскиванием (Hold & Drag) в GUI.
 */
@Singleton
public class GuiSwapService {

    private final JavaPlugin plugin;

    @Inject
    public GuiSwapService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void prepareDrag(Player player, GuiSession session, String sourceKey, ItemStack icon) {
        if (session == null || sourceKey == null || icon == null) return;

        session.setSwapSourceKey(sourceKey);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory() != null && session.isSwapping()) {
                player.setItemOnCursor(icon.clone());
            }
        });
    }

    /**
     * Поглощает текущий процесс перетаскивания.
     * Возвращает ключ источника (sourceKey), который перетаскивал игрок, и очищает состояние/курсор.
     * Если игрок ничего не перетаскивал, вернет null.
     */
    public String consumeSwap(Player player, GuiSession session) {
        if (session == null || !session.isSwapping()) {
            return null;
        }

        String sourceKey = session.getSwapSourceKey();
        cancelSwap(player, session); // Сбрасываем состояние и очищаем курсор
        return sourceKey;
    }

    /**
     * Безопасно отменяет перетаскивание (очищает курсор и сессию).
     * ВАЖНО: Вызывайте этот метод также в InventoryCloseEvent, чтобы предметы не выпадали на землю.
     */
    public void cancelSwap(Player player, GuiSession session) {
        if (session != null) {
            session.clearSwapSourceKey();
        }
        if (player != null) {
            // Очищаем сразу
            player.setItemOnCursor(null);

            // И железобетонно очищаем на следующем тике (обход event.setCancelled в Bukkit)
            Bukkit.getScheduler().runTask(plugin, () -> {
                InventoryViewCompat.setCursor(player, null);
                player.setItemOnCursor(null);
                player.updateInventory(); // Принудительно синхронизируем инвентарь клиента
            });
        }
    }
}