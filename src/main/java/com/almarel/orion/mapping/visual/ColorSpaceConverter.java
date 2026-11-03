package com.almarel.orion.mapping.visual;

/**
 * Pure, stateless color space math: sRGB -> CIE L*a*b* and sRGB -> HSL.
 * No DI — deterministic function with no dependencies, consistent with
 * java.lang.Math-style utility classes. Used only by BlockVisualProfile's
 * static factory to precompute derived color representations once.
 */
public final class ColorSpaceConverter {

    // sRGB (D65) linear RGB -> XYZ matrix, CIE standard.
    private static final double[][] SRGB_TO_XYZ_MATRIX = {
            {0.4124564, 0.3575761, 0.1804375},
            {0.2126729, 0.7151522, 0.0721750},
            {0.0193339, 0.1191920, 0.9503041}
    };

    // CIE D65 reference white, 0-100 scale.
    private static final double REFERENCE_WHITE_X = 95.047;
    private static final double REFERENCE_WHITE_Y = 100.0;
    private static final double REFERENCE_WHITE_Z = 108.883;

    private static final double LAB_EPSILON = Math.pow(6.0 / 29.0, 3);
    private static final double LAB_KAPPA = (1.0 / 3.0) * Math.pow(29.0 / 6.0, 2);

    private ColorSpaceConverter() {
        // Static utility, no instances.
    }

    /**
     * Converts sRGB to CIE L*a*b*.
     * Sanity check: RgbColor(255,0,0) -> approx Lab(53.24, 80.09, 67.20).
     * Sanity check: RgbColor(255,255,255) -> approx Lab(100.0, 0.0, 0.0).
     */
    public static LabColor rgbToLab(RgbColor rgb) {
        double linearR = linearizeSrgbChannel(rgb.r() / 255.0);
        double linearG = linearizeSrgbChannel(rgb.g() / 255.0);
        double linearB = linearizeSrgbChannel(rgb.b() / 255.0);

        double x = (linearR * SRGB_TO_XYZ_MATRIX[0][0]
                + linearG * SRGB_TO_XYZ_MATRIX[0][1]
                + linearB * SRGB_TO_XYZ_MATRIX[0][2]) * 100.0;
        double y = (linearR * SRGB_TO_XYZ_MATRIX[1][0]
                + linearG * SRGB_TO_XYZ_MATRIX[1][1]
                + linearB * SRGB_TO_XYZ_MATRIX[1][2]) * 100.0;
        double z = (linearR * SRGB_TO_XYZ_MATRIX[2][0]
                + linearG * SRGB_TO_XYZ_MATRIX[2][1]
                + linearB * SRGB_TO_XYZ_MATRIX[2][2]) * 100.0;

        double fx = labPivot(x / REFERENCE_WHITE_X);
        double fy = labPivot(y / REFERENCE_WHITE_Y);
        double fz = labPivot(z / REFERENCE_WHITE_Z);

        double l = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double b = 200.0 * (fy - fz);

        return new LabColor(l, a, b);
    }

    /**
     * Converts sRGB to HSL (hue in degrees [0, 360), saturation/lightness in [0, 1]).
     * Sanity check: RgbColor(255,0,0) -> Hsl(0.0, 1.0, 0.5).
     * Sanity check: RgbColor(255,255,255) -> Hsl(0.0, 0.0, 1.0).
     */
    public static HslColor rgbToHsl(RgbColor rgb) {
        double r = rgb.r() / 255.0;
        double g = rgb.g() / 255.0;
        double b = rgb.b() / 255.0;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double lightness = (max + min) / 2.0;

        if (max == min) {
            return new HslColor(0.0, 0.0, lightness);
        }

        double delta = max - min;
        double saturation = lightness > 0.5
                ? delta / (2.0 - max - min)
                : delta / (max + min);

        double hue;
        if (max == r) {
            hue = ((g - b) / delta) + (g < b ? 6.0 : 0.0);
        } else if (max == g) {
            hue = ((b - r) / delta) + 2.0;
        } else {
            hue = ((r - g) / delta) + 4.0;
        }
        hue *= 60.0;
        if (hue < 0.0) {
            hue += 360.0;
        }

        return new HslColor(hue, saturation, lightness);
    }

    private static double linearizeSrgbChannel(double channel) {
        return channel <= 0.04045
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static double labPivot(double t) {
        return t > LAB_EPSILON
                ? Math.cbrt(t)
                : (LAB_KAPPA * t) + (4.0 / 29.0);
    }

    /**
     * Converts HSL back to sRGB. Standard inverse of rgbToHsl.
     * Needed by the palette-harmony track: hue-rotation math happens in
     * HSL space, but NearestBlockByColorFinder operates on Lab — this is
     * the bridge between the two.
     */
    public static RgbColor hslToRgb(HslColor hsl) {
        double h = hsl.hue() / 360.0;
        double s = hsl.saturation();
        double l = hsl.lightness();

        double r, g, b;
        if (s == 0.0) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgbChannel(p, q, h + 1.0 / 3.0);
            g = hueToRgbChannel(p, q, h);
            b = hueToRgbChannel(p, q, h - 1.0 / 3.0);
        }

        return new RgbColor(clampToByte(r * 255.0), clampToByte(g * 255.0), clampToByte(b * 255.0));
    }

    /**
     * Convenience shortcut for the palette track: HSL -> RGB -> Lab in one
     * call, avoiding duplicated plumbing at every call site.
     */
    public static LabColor hslToLab(HslColor hsl) {
        return rgbToLab(hslToRgb(hsl));
    }

    private static double hueToRgbChannel(double p, double q, double t) {
        double tt = t;
        if (tt < 0) tt += 1;
        if (tt > 1) tt -= 1;
        if (tt < 1.0 / 6.0) return p + (q - p) * 6 * tt;
        if (tt < 1.0 / 2.0) return q;
        if (tt < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - tt) * 6;
        return p;
    }

    private static int clampToByte(double value) {
        return (int) Math.round(Math.max(0, Math.min(255, value)));
    }
}