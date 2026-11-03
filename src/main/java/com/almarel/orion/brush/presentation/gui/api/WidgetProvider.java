package com.almarel.orion.brush.presentation.gui.api;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.brush.presentation.gui.settings.UniversalBrushController;
import com.almarel.orion.brush.presentation.gui.settings.widgets.grid.DynamicGridBuilder;
import org.bukkit.entity.Player;

/**
 * Контракт для динамического внедрения виджетов в UniversalBrushRenderer.
 * Позволяет выносить логику отрисовки конкретных фич (например, паттернов или вращения)
 * в отдельные, независимые классы.
 */
public interface WidgetProvider {

    /**
     * Вызывается при рендере GUI для попытки внедрения виджета.
     *
     * @param holder          Контейнер инвентаря для установки кнопок.
     * @param state           Текущее состояние кисти (DTO).
     * @param player          Игрок, открывший меню.
     * @param controller      Контроллер для биндинга действий (кликов).
     * @param grid            Помощник для безопасного размещения виджетов по сетке.
     * @param refreshCallback Колбэк для перерисовки меню (вызывается при изменении стейта).
     */
    void inject(OrionMenuHolder holder,
                BrushState state,
                Player player,
                UniversalBrushController controller,
                DynamicGridBuilder grid,
                Runnable refreshCallback);
}