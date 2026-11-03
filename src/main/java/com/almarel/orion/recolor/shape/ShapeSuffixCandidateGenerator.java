package com.almarel.orion.recolor.shape;

import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ShapeSuffixCandidateGenerator {

    private static final Map<BlockShapeCategory, String> SUFFIX_BY_CATEGORY = new EnumMap<>(BlockShapeCategory.class);
    private static final List<String> ALL_SUFFIXES;

    static {
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.STAIRS, "_stairs");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.SLAB, "_slab");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.WALL, "_wall");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.FENCE, "_fence");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.FENCE_GATE, "_fence_gate");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.DOOR, "_door");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.TRAPDOOR, "_trapdoor");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.PRESSURE_PLATE, "_pressure_plate");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.BUTTON, "_button");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.CARPET, "_carpet");
        SUFFIX_BY_CATEGORY.put(BlockShapeCategory.LEAVES, "_leaves");
        // LOG, CHAIN, LANTERN: без суффикса.
        // GLASS_PANE: особый случай ниже.

        ALL_SUFFIXES = List.copyOf(SUFFIX_BY_CATEGORY.values());
    }

    public List<String> generateCandidates(BlockShapeCategory category, String namespacePrefix, String baseName) {
        if (category == BlockShapeCategory.GLASS_PANE) {
            if ("glass".equals(baseName)) {
                return List.of(namespacePrefix + "glass_pane");
            }
            return List.of(namespacePrefix + baseName + "_stained_glass_pane");
        }

        String suffix = SUFFIX_BY_CATEGORY.get(category);
        if (suffix == null) {
            return List.of();
        }

        return List.of(namespacePrefix + baseName + suffix);
    }

    /**
     * Все известные суффиксы форм (кроме GLASS_PANE — у него нерегулярный паттерн).
     */
    public List<String> getAllSuffixes() {
        return ALL_SUFFIXES;
    }
}