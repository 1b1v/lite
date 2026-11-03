package com.almarel.orion.mapping.visual;

import com.sk89q.worldedit.world.block.BlockType;

/**
 * Visual dataset entry for a single BlockType: its representative color
 * (raw + derived representations) and automation-suitability flags.
 * Plain data object — created via the static factory below, never injected.
 */
public record BlockVisualProfile(
        BlockType blockType,
        RgbColor rgbColor,
        LabColor labColor,
        HslColor hslColor,
        double noiseScore,
        boolean excluded,
        VisualExclusionReason exclusionReason
) {

    public BlockVisualProfile {
        if (excluded && exclusionReason == null) {
            throw new IllegalArgumentException(
                    "excluded=true requires a non-null exclusionReason for blockType=" + blockType);
        }
        if (!excluded && exclusionReason != null) {
            throw new IllegalArgumentException(
                    "excluded=false requires exclusionReason=null for blockType=" + blockType);
        }
    }

    /**
     * Builds a profile from raw dataset values, computing Lab/HSL
     * representations once via ColorSpaceConverter.
     */
    public static BlockVisualProfile of(BlockType blockType, RgbColor rgbColor, double noiseScore,
                                        boolean excluded, VisualExclusionReason exclusionReason) {
        LabColor labColor = ColorSpaceConverter.rgbToLab(rgbColor);
        HslColor hslColor = ColorSpaceConverter.rgbToHsl(rgbColor);
        return new BlockVisualProfile(blockType, rgbColor, labColor, hslColor, noiseScore, excluded, exclusionReason);
    }
}