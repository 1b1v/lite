package com.almarel.orion.brush.api;

import com.almarel.orion.brush.data.BaseBrushState;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a modular brush feature within the Orion system.
 * Each implementation of this interface corresponds to a specific brush type (e.g., Crystal, Copy, Construct).
 *
 * @param <S> The type of settings class used by this brush.
 */
public interface BrushFeature<S extends BaseBrushState> {

    /**
     * @return The unique identifier for this brush (e.g., "crystal", "cp").
     * Should be lowercase and unique.
     */
    @NotNull
    String getId();

    /**
     * @return The icon used in GUI to represent this brush.
     */
    @NotNull
    Material getIcon();

    /**
     * @return The class type of the settings object (useful for serialization/reflection).
     */
    @NotNull
    Class<S> getSettingsClass();

    /**
     * Factory method to create a new instance of default settings for this brush.
     * @return A new settings instance with default values.
     */
    @NotNull
    S createDefaultSettings();

    /**
     * @return The sorting priority/order in the GUI. Lower numbers appear first.
     */
    int getOrder();

    /**
     * Проверяет, есть ли у игрока права на использование этой кисти.
     * По умолчанию кисть доступна всем.
     *
     * @param player Игрок для проверки.
     * @return true, если игрок может использовать кисть.
     */
    default boolean hasPermission(org.bukkit.entity.Player player) {
        return true;
    }
}