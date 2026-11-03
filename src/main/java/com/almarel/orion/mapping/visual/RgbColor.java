package com.almarel.orion.mapping.visual;

/**
 * Raw sRGB color in [0, 255] per channel. Source-of-truth representation
 * coming straight from the JSON dataset (one average color per BlockType).
 */
public record RgbColor(int r, int g, int b) {

    public RgbColor {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException(
                    "RGB components must be in range [0, 255], got: r=" + r + " g=" + g + " b=" + b);
        }
    }
}