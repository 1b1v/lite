package com.almarel.orion.framework.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * КАРАНТИННАЯ ЗОНА: org.bukkit.inventory.InventoryView сменил природу типа
 * между версиями API (abstract class -> interface, начиная примерно с 1.21).
 * <p>
 * Байткод, скомпилированный против одной природы типа (invokevirtual/invokeinterface),
 * несовместим с рантаймом другой природы — JVM бросает:
 * {@link IncompatibleClassChangeError}: "Found class ..., but interface was expected".
 * <p>
 * Reflection обходит это, так как {@link Method#invoke} выполняет динамическую
 * диспетчеризацию в рантайме и не привязан к bytecode-инструкции, зафиксированной
 * во время компиляции плагина.
 */
public final class InventoryViewCompat {

    private static volatile Method getOpenInventoryMethod;
    private static volatile Method getTopInventoryMethod;
    private static volatile Method setCursorMethod;
    private static volatile boolean resolved = false;

    private InventoryViewCompat() {}

    /**
     * Безопасно получает верхний (GUI) инвентарь открытого у игрока окна,
     * независимо от того, является ли InventoryView классом или интерфейсом
     * в установленной на сервере версии API.
     */
    public static Inventory getTopInventory(HumanEntity human) {
        if (human == null) return null;
        try {
            resolveMethods();
            if (getOpenInventoryMethod == null || getTopInventoryMethod == null) return null;

            Object view = getOpenInventoryMethod.invoke(human);
            if (view == null) return null;

            Object inventory = getTopInventoryMethod.invoke(view);
            return (inventory instanceof Inventory) ? (Inventory) inventory : null;
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to get topInventory via compat layer", t);
            return null;
        }
    }

    /**
     * Безопасно очищает курсор игрока через InventoryView, независимо от того,
     * является ли InventoryView классом или интерфейсом в текущей версии API.
     */
    public static void setCursor(HumanEntity human, ItemStack item) {
        if (human == null) return;
        try {
            resolveMethods();
            if (getOpenInventoryMethod == null || setCursorMethod == null) return;

            Object view = getOpenInventoryMethod.invoke(human);
            if (view == null) return;

            setCursorMethod.invoke(view, item);
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to execute setCursor via compat layer", t);
        }
    }

    private static void resolveMethods() {
        if (resolved) return;
        synchronized (InventoryViewCompat.class) {
            if (resolved) return;
            try {
                getOpenInventoryMethod = HumanEntity.class.getMethod("getOpenInventory");
                Class<?> viewClass = Class.forName("org.bukkit.inventory.InventoryView");
                getTopInventoryMethod = viewClass.getMethod("getTopInventory");
                setCursorMethod = viewClass.getMethod("setCursor", ItemStack.class);
            } catch (Throwable ignored) {
                getOpenInventoryMethod = null;
                getTopInventoryMethod = null;
                setCursorMethod = null;
            } finally {
                resolved = true;
            }
        }
    }
}