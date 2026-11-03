package com.almarel.orion.brush.data;

import com.almarel.orion.brush.data.enums.OrionBrushType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Базовый интерфейс для всех состояний кистей (DTO).
 */
public interface BrushState {

    Material getTool();

    // --- Persistence Logic ---
    boolean isPersistent();
    void setPersistent(boolean persistent);

    // [NEW] Identity API
    String getId();
    void setId(String id);

    String getStorageKey();

    OrionBrushType getBrushType();

    // === Polymorphic API for BrushSyncService (Optimization v2.0) ===

    default void cachePlayerOrientation(Player player) {
        // Пустая реализация по умолчанию.
    }

    // --- Sorting API ---
    int getOrderIndex();
    void setOrderIndex(int orderIndex);

    default boolean needsRecreationOnDesync() {
        return false;
    }

    // === Polymorphic API for GUI (Feature Cutting Safe) ===

    default String getGuiIdentifier() {
        return "Unknown";
    }

    default boolean hasActiveBuffer() {
        return false;
    }

    /**
     * Приоритет сортировки в селекторе GUI: 1 = Temp, 2 = Perm.
     * Переопределяется кистями с дополнительными промежуточными состояниями
     * (см. CopyPasteBrushState — там есть уровень 3 "Buffered").
     */
    default int getSortPriority() {
        return isPersistent() ? 2 : 1;
    }
}