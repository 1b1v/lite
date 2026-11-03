package com.almarel.orion.framework.integration.fawe;

import com.fastasyncworldedit.core.queue.IChunkGet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Единая точка побитового сканирования региона через {@link ChunkAccessCache}.
 * Порядок обхода (Z -> X -> получение чанка -> Y) намеренно фиксирован:
 * чанк запрашивается один раз на всю Y-колонку, что исключает повторные
 * обращения к кэшу чанков на каждый вокссель.
 * Используется DensityMatrix (pattern.smooth) и SlopeSmoothingField (pattern.flora).
 */
public final class RegionChunkScanner {

    private RegionChunkScanner() {
    }

    @FunctionalInterface
    public interface BlockStateVisitor {
        void visit(int x, int y, int z, BlockState state);
    }

    public static void forEachBlock(World world, Region region, BlockStateVisitor visitor) {
        if (region instanceof CuboidRegion cuboid) {
            scanCuboid(world, cuboid, visitor);
            return;
        }
        scanGeneric(world, region, visitor);
    }

    private static void scanCuboid(World world, CuboidRegion cuboid, BlockStateVisitor visitor) {
        BlockVector3 min = cuboid.getMinimumPoint();
        BlockVector3 max = cuboid.getMaximumPoint();

        int minX = min.x(), minY = min.y(), minZ = min.z();
        int maxX = max.x(), maxY = max.y(), maxZ = max.z();

        ChunkAccessCache cache = new ChunkAccessCache(world);

        for (int z = minZ; z <= maxZ; z++) {
            int cz = z >> 4;
            int lz = z & 15;

            for (int x = minX; x <= maxX; x++) {
                IChunkGet chunk = cache.getChunk(x >> 4, cz);
                int lx = x & 15;

                for (int y = minY; y <= maxY; y++) {
                    visitor.visit(x, y, z, chunk.getBlock(lx, y, lz));
                }
            }
        }
    }

    private static void scanGeneric(World world, Region region, BlockStateVisitor visitor) {
        ChunkAccessCache cache = new ChunkAccessCache(world);

        for (BlockVector3 pos : region) {
            IChunkGet chunk = cache.getChunk(pos.x() >> 4, pos.z() >> 4);
            visitor.visit(pos.x(), pos.y(), pos.z(), chunk.getBlock(pos.x() & 15, pos.y(), pos.z() & 15));
        }
    }
}