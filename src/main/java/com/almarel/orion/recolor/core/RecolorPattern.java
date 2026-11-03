package com.almarel.orion.recolor.core;

import com.almarel.orion.recolor.shape.ShapeAwareStateTransferor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Map;

/**
 * Pattern, применяющий маппинг oldType -> newType к региону.
 * State переносится через ShapeAwareStateTransferor.
 *
 * Оптимизация:
 * - lookup replacement по internalId (массив), вместо Map#get на каждый блок.
 */
public final class RecolorPattern implements Pattern {

    private final Extent source;
    private final ShapeAwareStateTransferor transferor;

    /** replacementByTypeId[oldTypeInternalId] -> newType */
    private final BlockType[] replacementByTypeId;

    public RecolorPattern(Extent source,
                          Map<BlockType, BlockType> mapping,
                          ShapeAwareStateTransferor transferor) {
        this.source = source;
        this.transferor = transferor;
        this.replacementByTypeId = buildReplacementIndex(mapping);
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        BlockState original = source.getBlock(position);

        BlockType replacement = findReplacement(original.getBlockType());
        if (replacement == null) {
            return original.toBaseBlock();
        }

        BlockState transferred = transferor.transfer(original, replacement);
        return transferred.toBaseBlock();
    }

    private BlockType findReplacement(BlockType oldType) {
        int id = oldType.getInternalId();
        return (id >= 0 && id < replacementByTypeId.length) ? replacementByTypeId[id] : null;
    }

    private static BlockType[] buildReplacementIndex(Map<BlockType, BlockType> mapping) {
        int maxId = -1;
        for (BlockType k : mapping.keySet()) {
            maxId = Math.max(maxId, k.getInternalId());
        }

        if (maxId < 0) {
            return new BlockType[0];
        }

        BlockType[] index = new BlockType[maxId + 1];
        for (var e : mapping.entrySet()) {
            int id = e.getKey().getInternalId();
            if (id >= 0 && id < index.length) {
                index[id] = e.getValue();
            }
        }
        return index;
    }
}