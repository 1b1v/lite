package com.almarel.orion.framework.gui.common;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * SmartInventoryHolder - Контейнер для меню v3.0.
 * Хранит привязку "Слот -> Кнопка".
 * Позволяет GuiManager-у находить и вызывать логику конкретной кнопки.
 */
public class OrionMenuHolder implements InventoryHolder {

    // Карта: Номер слота -> Объект кнопки
    private final Map<Integer, GuiMenuButton> buttons = new HashMap<>();

    // Ссылка на сам инвентарь Bukkit
    private Inventory inventory;

    /**
     * Регистрирует кнопку в определенном слоте.
     * Если в слоте уже была кнопка, она заменится новой.
     * @param slot Номер слота (0-53).
     * @param button Кнопка (логика + иконка).
     */
    public void addButton(int slot, GuiMenuButton button) {
        buttons.put(slot, button);
        if (inventory != null) {
            inventory.setItem(slot, button.getIcon());
        }
    }

    /**
     * Удаляет кнопку из слота и очищает иконку в инвентаре.
     * Используется при динамическом обновлении меню (например, пагинация).
     * @param slot Номер слота.
     */
    public void removeButton(int slot) {
        buttons.remove(slot);
        if (inventory != null) {
            inventory.setItem(slot, null);
        }
    }

    /**
     * Получает кнопку по слоту.
     * @param slot Номер слота.
     * @return GuiButton или null, если слот пуст/декоративен.
     */
    @Nullable
    public GuiMenuButton getButton(int slot) {
        return buttons.get(slot);
    }

    /**
     * Устанавливает инвентарь (вызывается при создании меню).
     * @param inventory Созданный инвентарь Bukkit.
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}