package com.almarel.orion.brush.state;

import com.almarel.orion.brush.data.BrushState;
import org.bukkit.Material;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Компонент сессии, отвечающий за хранение состояний кистей.
 * 1 Инструмент = 1 Кисть.
 */
public class SessionBrushData {

    // Key: Material (Тип инструмента) -> Value: Настройки кисти
    @Getter
    private final Map<Material, BrushState> brushesData = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, BrushState> unboundBrushes = new ConcurrentHashMap<>();

    /**
     * Получить настройки по типу материала.
     * O(1) Lookup.
     */
    public BrushState getBrushSettings(Material tool) {
        if (tool == null) return null;
        return brushesData.get(tool);
    }

    /**
     * Сохранить настройки для материала.
     * Автоматически заменяет любую предыдущую кисть на этом инструменте.
     */
    public void putBrushSettings(Material tool, BrushState settings) {
        if (tool == null || settings == null) return;
        brushesData.put(tool, settings);
    }

    public void removeBrushSettings(Material tool) {
        if (tool == null) return;
        brushesData.remove(tool);
    }

    public void putUnboundBrush(BrushState settings) {
        if (settings == null || settings.getId() == null) return;
        unboundBrushes.put(settings.getId(), settings);
    }

    public BrushState getUnboundBrush(String id) {
        if (id == null) return null;
        return unboundBrushes.get(id);
    }

    public void removeUnboundBrush(String id) {
        if (id == null) return;
        unboundBrushes.remove(id);
    }
}