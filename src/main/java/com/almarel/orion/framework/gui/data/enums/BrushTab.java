package com.almarel.orion.framework.gui.data.enums;

import lombok.Value;

/**
 * Value Object, заменяющий старый enum BrushTab.
 * Позволяет динамически регистрировать новые вкладки (Open-Closed Principle),
 * сохраняя при этом потокобезопасность и обратную совместимость.
 */
@Value
public class BrushTab {

    // --- Обратная совместимость (Built-in Tabs) ---
    public static final BrushTab SETTINGS = new BrushTab("SETTINGS");
    public static final BrushTab POINT_SETTINGS = new BrushTab("POINT_SETTINGS");
    public static final BrushTab GROUP_LIST = new BrushTab("GROUP_LIST");

    String id;

    public BrushTab(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("BrushTab ID cannot be null or empty.");
        }
        this.id = id.toUpperCase();
    }
}