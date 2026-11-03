package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Переносит совместимые state-свойства со старого блока на дефолтный стейт нового типа.
 *
 * Оптимизация: кэш "плана переноса" по паре (oldType, newType), чтобы не делать
 * string-lookups и classify на каждый блок при массовом apply.
 */
@Singleton
public class ShapeAwareStateTransferor {

    private final BlockShapeClassifier classifier;

    private static final Map<BlockShapeCategory, Set<String>> SKIPPED_PROPERTIES = new EnumMap<>(BlockShapeCategory.class);
    static {
        SKIPPED_PROPERTIES.put(BlockShapeCategory.LEAVES, Set.of("distance"));
    }

    private final ConcurrentHashMap<Long, TransferPlan> planCache = new ConcurrentHashMap<>();

    @Inject
    public ShapeAwareStateTransferor(BlockShapeClassifier classifier) {
        this.classifier = classifier;
    }

    public BlockState transfer(BlockStateHolder<?> oldState, BlockType newType) {
        BlockType oldType = oldState.getBlockType();
        TransferPlan plan = planCache.computeIfAbsent(key(oldType, newType), k -> buildPlan(oldType, newType));

        BlockState result = newType.getDefaultState();
        Map<Property<?>, Object> oldStates = oldState.getStates();

        for (int i = 0; i < plan.oldProps.length; i++) {
            Object rawValue = oldStates.get(plan.oldProps[i]);
            if (rawValue == null) continue;
            result = applyIfValid(result, plan.newProps[i], rawValue);
        }

        return result;
    }

    private TransferPlan buildPlan(BlockType oldType, BlockType newType) {
        BlockShapeCategory newCategory = classifier.classify(newType);
        Set<String> skipped = SKIPPED_PROPERTIES.get(newCategory);

        // Берём список свойств с default-state старого типа — они же будут в oldState.getStates()
        Map<Property<?>, Object> oldDefaultStates = oldType.getDefaultState().getStates();

        List<Property<?>> oldProps = new ArrayList<>(oldDefaultStates.size());
        List<Property<?>> newProps = new ArrayList<>(oldDefaultStates.size());

        for (Property<?> oldProp : oldDefaultStates.keySet()) {
            String name = oldProp.getName();
            if (skipped != null && skipped.contains(name)) continue;

            Property<?> newProp = newType.getProperty(name);
            if (newProp == null) continue;

            oldProps.add(oldProp);
            newProps.add(newProp);
        }

        return new TransferPlan(
                oldProps.toArray(Property[]::new),
                newProps.toArray(Property[]::new)
        );
    }

    private static long key(BlockType oldType, BlockType newType) {
        return (((long) oldType.getInternalId()) << 32) ^ (newType.getInternalId() & 0xFFFFFFFFL);
    }

    private record TransferPlan(Property<?>[] oldProps, Property<?>[] newProps) {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyIfValid(BlockState state, Property property, Object rawValue) {
        if (!property.getValues().contains(rawValue)) {
            return state;
        }
        return state.with(property, rawValue);
    }
}