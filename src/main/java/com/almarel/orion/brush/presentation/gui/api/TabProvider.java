package com.almarel.orion.brush.presentation.gui.api;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.framework.gui.data.enums.BrushTab;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.entity.Player;

/**
 * Контракт для динамического создания и внедрения вкладок (экранов) в меню кисти.
 * Позволяет выносить логику рендера (например, Group List или Point Settings)
 * в независимые классы-провайдеры, следуя принципу открытости-закрытости (OCP).
 */
public interface TabProvider {

    /**
     * Возвращает уникальный идентификатор вкладки.
     */
    BrushTab getTab();

    /**
     * Определяет слот в навигационной панели (GuiLayout), куда будет помещена иконка вкладки.
     */
    int getSlot();

    /**
     * Динамическое название меню при открытии этой вкладки.
     *
     * @param config Конфигурация для получения базовых строк.
     * @param state  Стейт кисти для получения специфичного названия (например, для конкретной кисти).
     */
    String getTitle(ConfigurationGuiStore config, BrushState state);

    /**
     * Проверяет, должна ли эта вкладка отображаться для данного стейта.
     * Например, вкладка "Point Settings" нужна только для Construct-кистей.
     */
    boolean isVisible(BrushState state);

    /**
     * Рендерит содержимое вкладки (кнопки, виджеты) в инвентарь.
     *
     * @param holder          Контейнер меню для установки кнопок.
     * @param state           Текущее состояние кисти.
     * @param player          Игрок, открывший меню.
     * @param refreshCallback Колбэк для полной перерисовки меню при изменениях.
     */
    void draw(OrionMenuHolder holder, BrushState state, Player player, Runnable refreshCallback);
}