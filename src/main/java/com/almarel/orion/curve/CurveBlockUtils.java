package com.almarel.orion.curve;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class CurveBlockUtils {

    /**
     * Проверяет, является ли паттерн полублоком (Slab).
     */
    public static boolean isSlab(Pattern pattern) {
        if (pattern instanceof BlockStateHolder) {
            BlockType type = ((BlockStateHolder<?>) pattern).getBlockType();
            String id = type.getId().toLowerCase();
            return id.contains("slab") || id.contains("step");
        }
        return false;
    }

    @Nullable
    public static BlockStateHolder<?> getSlabVariant(Pattern pattern, String variant) {
        if (pattern instanceof BlockStateHolder) {
            try {
                BlockStateHolder<?> state = (BlockStateHolder<?>) pattern;
                // Проверяем, поддерживает ли блок свойство TYPE, прежде чем менять его
                if (state.getBlockType().hasProperty(PropertyKey.TYPE)) {
                    return state.with(PropertyKey.TYPE, variant);
                }
            } catch (Exception ignored) {
                // Фоллбэк при любой ошибке рефлексии или API FAWE
            }
        }
        return null;
    }

    // --- Восстановленные методы для RopeRenderer ---

    public static class SlabStates {
        private final boolean slab;
        private final BlockStateHolder<?> top;
        private final BlockStateHolder<?> bottom;
        private final BlockStateHolder<?> doubleSlab;

        public SlabStates(boolean isSlab, BlockStateHolder<?> top, BlockStateHolder<?> bottom, BlockStateHolder<?> doubleSlab) {
            this.slab = isSlab;
            this.top = top;
            this.bottom = bottom;
            this.doubleSlab = doubleSlab;
        }

        public boolean isSlab() { return slab; }
        public BlockStateHolder<?> top() { return top; }
        public BlockStateHolder<?> bottom() { return bottom; }
        public BlockStateHolder<?> doubleSlab() { return doubleSlab; }
    }

    public static SlabStates resolveSlabStates(Pattern pattern) {
        if (isSlab(pattern)) {
            return new SlabStates(true, getSlabVariant(pattern, "top"), getSlabVariant(pattern, "bottom"), getSlabVariant(pattern, "double"));
        }
        return new SlabStates(false, null, null, null);
    }

    public static String toCardinal(double x, double z) {
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? "east" : "west";
        } else {
            return z > 0 ? "south" : "north";
        }
    }

    private static final ConcurrentHashMap<String, BlockStateHolder<?>> STATE_CACHE = new ConcurrentHashMap<>();

    public static int setStairWithFacing(EditSession session, BlockVector3 pos, Pattern pattern, String facing, String half) {
        if (pattern instanceof BlockStateHolder) {
            try {
                BlockType type = ((BlockStateHolder<?>) pattern).getBlockType();
                String cacheKey = type.getId() + "_" + facing + "_" + half;

                // Парсим строку только один раз для каждой комбинации (всего 8 вариантов на блок).
                // Это обходит баг FAWE 2.13.1 с невозможностью применить свойства через API.
                BlockStateHolder<?> state = STATE_CACHE.computeIfAbsent(cacheKey, key -> {
                    String blockString = type.getId() + "[facing=" + facing + ",half=" + half + "]";
                    try {
                        ParserContext context = new ParserContext();
                        context.setPreferringWildcard(false);
                        context.setRestricted(false);
                        return WorldEdit.getInstance().getBlockFactory().parseFromInput(blockString, context);
                    } catch (Exception e) {
                        return null;
                    }
                });

                if (state != null) {
                    return session.setBlock(pos, state) ? 1 : 0;
                }
            } catch (Exception e) {
                // Фоллбэк при любой ошибке
            }
        }
        return session.setBlock(pos, pattern) ? 1 : 0;
    }

    /**
     * Безопасно применяет свойство к BlockState.
     * Если свойство не существует, возвращает исходное состояние без краша.
     */
    public static BlockState applyPropertySafely(BlockState state, PropertyKey key, String value) {
        try {
            return (BlockState) state.with(key, value);
        } catch (Exception e) {
            return state;
        }
    }
}