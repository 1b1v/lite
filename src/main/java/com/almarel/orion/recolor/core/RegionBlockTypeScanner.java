package com.almarel.orion.recolor.core;

import com.almarel.orion.framework.integration.fawe.ChunkAccessCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Собирает подсчёт блоков по типу (BlockType -> объём) в регионе.
 * Используется для построения исходного списка "было" в GUI recolor
 * и для палитрового трека (DominantColorExtractor) — там "доминирующий"
 * цвет постройки определяется объёмом, а не числом уникальных BlockType.
 *
 * Оптимизация:
 * - если extent == World -> читаем через FAWE IChunkGet + ChunkAccessCache
 * - для CuboidRegion используем тройной цикл без region-итератора
 */
@Singleton
public class RegionBlockTypeScanner {

    private final RecolorExclusionRegistry exclusionRegistry;
    private final Map<BlockType, Boolean> itemableCache = new ConcurrentHashMap<>();

    @Inject
    public RegionBlockTypeScanner(RecolorExclusionRegistry exclusionRegistry) {
        this.exclusionRegistry = exclusionRegistry;
    }

    public Map<BlockType, Long> scanTypeCounts(World world, Region region) {
        Map<BlockType, Long> result = new HashMap<>();
        traverseWorld(world, region, type -> result.merge(type, 1L, Long::sum));
        return result;
    }

    @FunctionalInterface
    private interface BlockTypeVisitor {
        void visit(BlockType type);
    }

    private void traverseWorld(World world, Region region, BlockTypeVisitor visitor) {
        if (region instanceof CuboidRegion cuboid) {
            traverseCuboidFromWorld(world, cuboid, visitor);
        } else {
            traverseAnyRegionFromWorld(world, region, visitor);
        }
    }

    private void traverseCuboidFromWorld(World world, CuboidRegion cuboid, BlockTypeVisitor visitor) {
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
                    BlockType type = chunk.getBlock(lx, y, lz).getBlockType();
                    if (shouldInclude(type)) visitor.visit(type);
                }
            }
        }
    }

    private void traverseAnyRegionFromWorld(World world, Region region, BlockTypeVisitor visitor) {
        ChunkAccessCache cache = new ChunkAccessCache(world);

        for (BlockVector3 pos : region) {
            IChunkGet chunk = cache.getChunk(pos.x() >> 4, pos.z() >> 4);
            BlockType type = chunk.getBlock(pos.x() & 15, pos.y(), pos.z() & 15).getBlockType();
            if (shouldInclude(type)) visitor.visit(type);
        }
    }

    private boolean shouldInclude(BlockType type) {
        if (type == BlockTypes.AIR || type == BlockTypes.CAVE_AIR || type == BlockTypes.VOID_AIR) {
            return false;
        }
        if (exclusionRegistry.isExcluded(type)) {
            return false;
        }
        return isPlaceableItem(type);
    }

    /**
     * Отсекает BlockType, у которых нет соответствующего предмета
     * (вода, лава, огонь, порталы, bubble column и т.п.) — такие блоки
     * не могут быть отображены как иконка "было" в GUI и не могут быть
     * выбраны как цель "станет". Результат кэшируется per-BlockType.
     */
    private boolean isPlaceableItem(BlockType type) {
        return itemableCache.computeIfAbsent(type, t -> {
            try {
                return BukkitAdapter.adapt(t).isItem();
            } catch (Exception e) {
                return false;
            }
        });
    }
}