package com.almarel.orion.brush;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.api.BrushFeature;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry for all Orion brush features.
 * Holds the definitions of brushes but not their runtime state.
 */

@Singleton
public class OrionBrushRegistry {

    // LinkedHashMap maintains insertion order, which is important for GUI rendering order.
    private final Map<String, BrushFeature<?>> features = new LinkedHashMap<>();

    @Inject
    public OrionBrushRegistry() {
        // Пустой конструктор для Guice
    }

    /**
     * Registers a new brush feature.
     *
     * @param feature The feature implementation to register.
     * @throws IllegalArgumentException if a brush with the same ID is already registered.
     */
    public void register(BrushFeature<?> feature) {
        if (features.containsKey(feature.getId())) {
            throw new IllegalArgumentException("Brush feature with ID '" + feature.getId() + "' is already registered!");
        }
        features.put(feature.getId(), feature);
    }

    /**
     * Retrieves a feature by its ID.
     *
     * @param id The unique brush ID.
     * @return The feature, or null if not found.
     */
    @Nullable
    public BrushFeature<?> get(String id) {
        return features.get(id);
    }

    /**
     * @return An unmodifiable view of all registered features.
     */
    @Unmodifiable
    public Collection<BrushFeature<?>> getAll() {
        return Collections.unmodifiableCollection(features.values());
    }

    public boolean has(String id) {
        return features.containsKey(id);
    }
}