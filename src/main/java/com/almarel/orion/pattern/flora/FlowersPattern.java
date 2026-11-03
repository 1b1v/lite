package com.almarel.orion.pattern.flora;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.almarel.orion.pattern.util.BlockPlacementUtil;
import com.almarel.orion.pattern.util.BlockTagsUtil;
import com.almarel.orion.pattern.util.WeightedPalette;
import com.almarel.orion.pattern.util.WeightedPalette.WeightedBlock;

import java.util.concurrent.ThreadLocalRandom;

public class FlowersPattern extends AbstractPattern {

    private final PerlinNoise perlinBase;
    private final PerlinNoise perlinFine;
    private final WeightedPalette grassPalette;
    private final WeightedPalette flowerPalette;
    private final WeightedPalette tallGrassPalette;

    public FlowersPattern(double frequency, WeightedPalette grassPalette, WeightedPalette flowerPalette, WeightedPalette tallGrassPalette) {
        this.perlinBase = new PerlinNoise();
        this.perlinBase.setFrequency(frequency);

        this.perlinFine = new PerlinNoise();
        this.perlinFine.setFrequency(frequency * 2.5);

        this.grassPalette = grassPalette;
        this.flowerPalette = flowerPalette;
        this.tallGrassPalette = tallGrassPalette;
    }

    @Override
    public com.sk89q.worldedit.world.block.BaseBlock applyBlock(BlockVector3 position) {
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        int sx = set.getX();
        int sy = set.getY();
        int sz = set.getZ();

        boolean isCurrentAir = extent.getBlock(sx, sy, sz).isAir();
        int placeY = isCurrentAir ? sy : sy + 1;
        int checkY = isCurrentAir ? sy - 1 : sy;

        if (!canPlaceFlora(extent, sx, placeY, checkY, sz)) {
            return false;
        }

        Vector2 pos2D = Vector2.at(get.getX(), get.getZ());
        WeightedBlock chosenWB = selectStateByNoise(pos2D);

        if (chosenWB == null) {
            return false;
        }

        return placePlant(extent, sx, placeY, sz, chosenWB);
    }

    private boolean canPlaceFlora(Extent extent, int x, int placeY, int checkY, int z) {
        if (!extent.getBlock(x, placeY, z).isAir()) {
            return false;
        }
        return isSurfaceValid(extent, x, checkY, z);
    }

    private boolean placePlant(Extent extent, int x, int y, int z, WeightedBlock weightedBlock) throws WorldEditException {
        BlockState state = weightedBlock.state();
        if (isDoublePlant(state, weightedBlock)) {
            return placeDoublePlant(extent, x, y, z, state);
        }
        return placeSingleBlock(extent, x, y, z, state);
    }

    private boolean isDoublePlant(BlockState state, WeightedBlock weightedBlock) {
        return state.getBlockType().hasProperty(PropertyKey.HALF) && !weightedBlock.forceSingleBlock();
    }

    private boolean placeDoublePlant(Extent extent, int x, int y, int z, BlockState state) throws WorldEditException {
        if (!extent.getBlock(x, y + 1, z).isAir()) {
            return false; // Нет места для верхней половины
        }

        if (!BlockPlacementUtil.placeDoublePlant(extent, x, y, z, state)) {
            return placeSingleBlock(extent, x, y, z, state);
        }
        return true;
    }

    private WeightedBlock selectStateByNoise(Vector2 pos2D) {
        float noiseBase = (float) perlinBase.noise(pos2D);
        float noiseFine = (float) perlinFine.noise(pos2D);

        WeightedBlock deadBush = rollRareDeadBush();
        if (deadBush != null) {
            return deadBush;
        }

        if (isFlowerZone(noiseBase, noiseFine)) {
            return rollFlowerCluster();
        }

        return rollGrassCluster(noiseBase);
    }

    private WeightedBlock rollRareDeadBush() {
        if (flowerPalette.isEmpty() || ThreadLocalRandom.current().nextFloat() >= 0.015f) {
            return null;
        }
        WeightedBlock testBlock = flowerPalette.rollRandom();
        return (testBlock.state().getBlockType() == BlockTypes.DEAD_BUSH) ? testBlock : null;
    }

    private boolean isFlowerZone(float noiseBase, float noiseFine) {
        return !flowerPalette.isEmpty() && (noiseBase > 0.91f || noiseFine < 0.11f);
    }

    private WeightedBlock rollFlowerCluster() {
        WeightedBlock chosen = flowerPalette.rollRandom();
        if (chosen.state().getBlockType() == BlockTypes.DEAD_BUSH) {
            return null;
        }
        return ThreadLocalRandom.current().nextFloat() < 0.5f ? chosen : null;
    }

    private WeightedBlock rollGrassCluster(float noiseBase) {
        if (grassPalette.isEmpty() || noiseBase <= 0.61f) {
            return null;
        }

        if (shouldSampleTallGrass(noiseBase)) {
            return tallGrassPalette.rollRandom();
        }
        return grassPalette.rollRandom();
    }

    private boolean shouldSampleTallGrass(float noiseBase) {
        return noiseBase > 0.82f && !tallGrassPalette.isEmpty() && ThreadLocalRandom.current().nextFloat() < 0.30f;
    }

    private boolean isSurfaceValid(Extent extent, int cx, int cy, int cz) {
        return BlockTagsUtil.isFloraSurface(extent.getBlock(cx, cy, cz).getBlockType());
    }

    private boolean placeSingleBlock(Extent extent, int px, int py, int pz, BlockState state) throws WorldEditException {
        return extent.setBlock(px, py, pz, state);
    }
}