package com.almarel.orion.recolor.core;

import com.almarel.orion.recolor.shape.ShapeAwareVariantResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class RecolorMapBuilder {

    private final ShapeAwareVariantResolver resolver;
    private final RecolorExclusionRegistry exclusionRegistry;

    @Inject
    public RecolorMapBuilder(ShapeAwareVariantResolver resolver, RecolorExclusionRegistry exclusionRegistry) {
        this.resolver = resolver;
        this.exclusionRegistry = exclusionRegistry;
    }

    public Map<BlockType, BlockType> build(Map<BlockType, BlockType> desiredTargets) {
        if (desiredTargets.isEmpty()) {
            return Map.of();
        }

        Map<BlockType, BlockType> resolved = new HashMap<>(desiredTargets.size() * 2);

        for (Map.Entry<BlockType, BlockType> entry : desiredTargets.entrySet()) {
            BlockType originalType = entry.getKey();
            BlockType targetMaterial = entry.getValue();

            resolveMapping(originalType, targetMaterial)
                    .ifPresent(resolvedType -> resolved.put(originalType, resolvedType));
        }

        return resolved.isEmpty() ? Map.of() : Map.copyOf(resolved);
    }

    private Optional<BlockType> resolveMapping(BlockType originalType, BlockType targetMaterial) {
        if (!isEligible(originalType, targetMaterial)) {
            return Optional.empty();
        }

        return resolver.resolve(originalType.getDefaultState(), targetMaterial)
                .filter(resolvedType -> resolvedType != originalType);
    }

    private boolean isEligible(BlockType originalType, BlockType targetMaterial) {
        if (originalType == null || targetMaterial == null) {
            return false;
        }
        return !exclusionRegistry.isExcluded(originalType) && !exclusionRegistry.isExcluded(targetMaterial);
    }
}