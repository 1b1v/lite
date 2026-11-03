package com.almarel.orion.pattern.util;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Set;

public final class BlockTagsUtil {

    private BlockTagsUtil() {
        // Утилитный класс не инстанцируется
    }

    /**
     * Поверхности, подходящие для вспахивания грядок (FieldPattern).
     * Включает натуральную траву и строительные "зеленые" блоки для органики.
     */
    public static final Set<BlockType> FIELD_SURFACES = Set.of(
            BlockTypes.GRASS_BLOCK,
            BlockTypes.MOSS_BLOCK,
            BlockTypes.GREEN_WOOL,
            BlockTypes.GREEN_TERRACOTTA,
            BlockTypes.LIME_TERRACOTTA,
            BlockTypes.GREEN_CONCRETE_POWDER
    );

    /**
     * Поверхности, подходящие для спавна любой флоры (FlowersPattern).
     * Включает зеленые блоки, землю (для травы) и пустынные блоки (для мертвых кустов).
     */
    public static final Set<BlockType> FLORA_SURFACES = Set.of(
            // Зеленые блоки
            BlockTypes.GRASS_BLOCK, BlockTypes.MOSS_BLOCK,
            BlockTypes.GREEN_WOOL, BlockTypes.GREEN_TERRACOTTA,
            BlockTypes.LIME_TERRACOTTA, BlockTypes.GREEN_CONCRETE_POWDER,

            // Земляные блоки
            BlockTypes.DIRT, BlockTypes.COARSE_DIRT, BlockTypes.PODZOL,

            // Пустынные блоки (Wasteland)
            BlockTypes.SAND, BlockTypes.RED_SAND,
            BlockTypes.TERRACOTTA, BlockTypes.BROWN_TERRACOTTA
    );

    public static boolean isFieldSurface(BlockType type) {
        return FIELD_SURFACES.contains(type);
    }

    public static boolean isFloraSurface(BlockType type) {
        return FLORA_SURFACES.contains(type);
    }
}