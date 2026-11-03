package com.almarel.orion.pattern.util;

import com.sk89q.worldedit.world.block.BlockState;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedPalette {

    private final List<WeightedBlock> blocks;
    private final int totalWeight;

    public WeightedPalette(List<WeightedBlock> blocks) {
        this.blocks = blocks;
        int sum = 0;
        for (WeightedBlock wb : blocks) {
            sum += wb.weight();
        }
        this.totalWeight = sum;
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public WeightedBlock rollRandom() {
        if (blocks.isEmpty()) return null;
        if (totalWeight <= 0) return blocks.get(0);

        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

        for (WeightedBlock wb : blocks) {
            randomWeight -= wb.weight();
            if (randomWeight < 0) {
                return wb;
            }
        }

        return blocks.get(blocks.size() - 1);
    }

    public record WeightedBlock(BlockState state, int weight, boolean forceSingleBlock) {

        public WeightedBlock(BlockState state, int weight) {
            this(state, weight, false);
        }
    }
}