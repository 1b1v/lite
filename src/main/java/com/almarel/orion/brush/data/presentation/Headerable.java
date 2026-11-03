package com.almarel.orion.brush.data.presentation;

/**
 * Кисть, которая сама знает свой заголовок для карточки GUI-селектора
 * (без цвета статуса — Perm/Default/Temp добавляет Renderer).
 *
 * Применяется ТОЛЬКО для кистей, чей заголовок вычисляется из их
 * собственных полей (имя команды, тип инструмента), а не из
 * конфигурируемых GUI-лейблов (те идут через BrushListGuiConfig
 * по OrionBrushType — см. BrushItemRenderer.resolveConfiguredHeader).
 */
public interface Headerable {
    String getDisplayHeader();
}