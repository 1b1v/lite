package com.almarel.orion.pattern.util;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

public final class BlockPlacementUtil {

    private BlockPlacementUtil() {
        // Утилитный класс не должен инстанцироваться
    }

    /**
     * Устанавливает двойное растение (tall grass, tall flower, tall seagrass).
     * Вызывающий код ОБЯЗАН предварительно проверить, что блоки на (y) и (y + 1)
     * подходят для установки (воздух/вода).
     *
     * @return true, если блок поддерживает половинки и был успешно установлен.
     */
    public static boolean placeDoublePlant(Extent extent, int x, int y, int z, BlockState baseState) throws WorldEditException {
        BlockType type = baseState.getBlockType();

        if (!type.hasProperty(PropertyKey.HALF)) {
            return false;
        }

        Object lowerProp = type.getProperty(PropertyKey.HALF).getValueFor("lower");
        Object upperProp = type.getProperty(PropertyKey.HALF).getValueFor("upper");

        if (lowerProp == null || upperProp == null) {
            return false;
        }

        BlockState finalBottom = baseState.with(PropertyKey.HALF, lowerProp);
        BlockState finalTop = baseState.with(PropertyKey.HALF, upperProp);

        extent.setBlock(x, y, z, finalBottom);
        extent.setBlock(x, y + 1, z, finalTop);

        return true;
    }
}