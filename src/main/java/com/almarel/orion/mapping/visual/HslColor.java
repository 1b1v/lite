package com.almarel.orion.mapping.visual;

/**
 * Hue/Saturation/Lightness color. Hue in degrees [0, 360), saturation and
 * lightness normalized to [0, 1]. Used for color-wheel operations
 * (harmonic palette generation — complementary/triadic/etc.).
 */
public record HslColor(double hue, double saturation, double lightness) {

    public HslColor {
        if (hue < 0 || hue >= 360) {
            throw new IllegalArgumentException("Hue must be in range [0, 360), got: " + hue);
        }
        if (saturation < 0 || saturation > 1) {
            throw new IllegalArgumentException("Saturation must be in range [0, 1], got: " + saturation);
        }
        if (lightness < 0 || lightness > 1) {
            throw new IllegalArgumentException("Lightness must be in range [0, 1], got: " + lightness);
        }
    }
}