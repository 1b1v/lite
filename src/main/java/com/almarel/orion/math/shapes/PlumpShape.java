package com.almarel.orion.math.shapes;

import com.almarel.orion.math.FastNoise;
import com.almarel.orion.math.SDF;

/**
 * SDF реализация формы "Капля" (Plump).
 * Оптимизирована с использованием Narrow Band Evaluation (отсечение вычислений шума).
 */
public class PlumpShape implements SDF {

    private final double frequency; // Коэффициент сжатия координат (f)
    private final double grain;     // Зернистость шума
    private final boolean hollow;   // Пустотелость

    // Максимальное влияние шума на формулу (1.0 / 6.0 ≈ 0.1666). Берем с запасом 0.18
    private static final double MAX_NOISE_IMPACT = 0.18;
    private static final double HOLLOW_THICKNESS = 0.025;

    public PlumpShape(double maxRadius, double grain, boolean hollow) {
        this.frequency = (maxRadius > 0) ? (0.5 / maxRadius) : 1.0;
        this.grain = grain;
        this.hollow = hollow;
    }

    @Override
    public boolean isInside(double x, double y, double z, FastNoise noise) {
        if (noise == null) return false;

        double X = x * frequency;
        double Y = y * frequency;
        double Z = z * frequency;

        // [ORION FIX]: Ось капли перенесена с Z (Глубина) на Y (Высота).
        // 0.31 * Y делает верхнюю часть (Y > 0) жирной, а нижнюю (Y < 0) острой.
        double baseLeft = 0.2 + (0.31 * Y);

        double absX = Math.abs(X);
        double absY = Math.abs(Y);
        double absZ = Math.abs(Z);
        double maxAbs = Math.max(absX, Math.max(absY, absZ));
        double distSq = (X * X) + (Y * Y) + (Z * Z);

        double right = (maxAbs * 0.5) + distSq;
        double diff = baseLeft - right;

        if (hollow) {
            if (diff > HOLLOW_THICKNESS + MAX_NOISE_IMPACT || diff < -MAX_NOISE_IMPACT) {
                return false;
            }
        } else {
            if (diff > MAX_NOISE_IMPACT) {
                return true;
            }
            if (diff < -MAX_NOISE_IMPACT) {
                return false;
            }
        }

        double noiseVal = noise.eval(X * grain, Y * grain, Z * grain);
        double finalLeft = baseLeft + (noiseVal * 0.16666666666666666);

        if (hollow) {
            return finalLeft > right && finalLeft < (right + HOLLOW_THICKNESS);
        }
        return finalLeft > right;
    }
}