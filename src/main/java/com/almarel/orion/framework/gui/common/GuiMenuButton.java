package com.almarel.orion.framework.gui.common;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * GuiMenuButton - Умная кнопка меню.
 * Объединяет визуальную часть (Иконку) и логику (Действие).
 */
public class GuiMenuButton {

    private final ItemStack icon;
    private final Consumer<InventoryClickEvent> action;

    /**
     * Создает кнопку.
     * @param icon Предмет, который будет отображаться в слоте.
     * @param action Лямбда-выражение, которое сработает при клике.
     */
    public GuiMenuButton(ItemStack icon, Consumer<InventoryClickEvent> action) {
        this.icon = icon;
        this.action = action;
    }

    /**
     * Создает кнопку без действия (декорация).
     * @param icon Предмет для отображения.
     */
    public GuiMenuButton(ItemStack icon) {
        this(icon, null);
    }

    public ItemStack getIcon() {
        return icon;
    }

    /**
     * Выполняет действие кнопки.
     * @param event Событие клика Bukkit.
     */
    public void onClick(InventoryClickEvent event) {
        if (action != null) {
            action.accept(event);
        }
    }
}