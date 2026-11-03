package com.almarel.orion.pattern.flora;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.almarel.orion.framework.concurrent.AsyncExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrassPattern extends AbstractPattern {

    private final BlockState baseBlock;
    private final BlockState edgeBlock;

    private final Region region;
    private final AsyncExecutor asyncExecutor;

    private SlopeSmoothingField field;
    private final AtomicBoolean isBuilding = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);

    public GrassPattern(BlockState baseBlock, BlockState edgeBlock,
                        Region region, AsyncExecutor asyncExecutor) {
        this.baseBlock = baseBlock;
        this.edgeBlock = edgeBlock;
        this.region = region;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return baseBlock.toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        ensureFieldInitialized(extent);

        if (field == null || !field.isValid()) {
            return false;
        }

        int x = get.getBlockX();
        int y = get.getBlockY();
        int z = get.getBlockZ();

        if (!field.shouldPaint(x, y, z)) {
            return false;
        }

        BlockState chosenState = resolveBlockState(x, y, z);
        return extent.setBlock(set.getX(), set.getY(), set.getZ(), chosenState);
    }

    /**
     * Потокобезопасная ленивая инициализация поля сглаживания.
     * Паттерн идентичен SmoothPattern.ensureMatrixInitialized —
     * единый подход к async-постройке тяжёлых воксельных структур в Orion.
     */
    private void ensureFieldInitialized(Extent extent) {
        if (region == null || field != null) {
            return;
        }

        if (isBuilding.compareAndSet(false, true)) {
            buildField(extent);
        } else {
            awaitFieldBuild();
        }
    }

    private void buildField(Extent extent) {
        try {
            World world = resolveWorld(extent);
            if (world != null) {
                this.field = new SlopeSmoothingField(world, region, asyncExecutor);
            }
        } finally {
            latch.countDown();
        }
    }

    private void awaitFieldBuild() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private World resolveWorld(Extent extent) {
        World world = region.getWorld();
        if (world == null && extent instanceof World worldExtent) {
            return worldExtent;
        }
        return world;
    }

    /**
     * Определяет финальный материал блока на основе открытости граней
     * (работает по НЕразмытому снапшоту внутри SlopeSmoothingField).
     */
    private BlockState resolveBlockState(int x, int y, int z) {
        return field.countAirContacts(x, y, z) >= 2 ? edgeBlock : baseBlock;
    }
}