package com.almarel.orion.mapping.visual;

/**
 * CIE L*a*b* color. Perceptually uniform — Euclidean distance between two
 * LabColor values approximates how different they look to a human eye
 * (CIE76 metric). Used for "find nearest block by color" lookups.
 */
public record LabColor(double l, double a, double b) {

    /**
     * CIE76 color difference (simplest standard ΔE formula).
     * Smaller value = more visually similar colors.
     */
    public double deltaE76(LabColor other) {
        double dl = this.l - other.l;
        double da = this.a - other.a;
        double db = this.b - other.b;
        return Math.sqrt(dl * dl + da * da + db * db);
    }
}