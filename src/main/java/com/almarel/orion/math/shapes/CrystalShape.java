package com.almarel.orion.math.shapes;

import com.almarel.orion.math.FastNoise;
import com.almarel.orion.math.SDF;

/**
 * SDF реализация формы "Кристалл".
 * Оптимизирована за счет математических границ, Early Out и прекомпиляции множителей (FPU Multiply vs Divide).
 */
public class CrystalShape implements SDF {

    private static final double MAX_POLY_MULTIPLIER = 1.1716; // Высчитанный предел для term2_poly (1 / cos^2(pi/8))

    // Прекомпилированные множители
    private final double invRadiusY;
    private final double invRadiusXZ;

    public CrystalShape(double radius) {
        // Заменяем деление на умножение заранее
        double invRadius = 1.0 / radius;
        this.invRadiusY = invRadius * 1.5;
        this.invRadiusXZ = invRadius * 4.0;
    }

    @Override
    public boolean isInside(double x, double y, double z, FastNoise noise) {
        double fY = y * invRadiusY;
        double absY = Math.abs(fY);

        // ОПТИМИЗАЦИЯ 1: Моментальное отсечение воздуха сверху и снизу
        if (absY >= 1.0) return false;

        double fX = x * invRadiusXZ;
        double fZ = z * invRadiusXZ;

        double term1 = (fX * fX + fZ * fZ) * 1.1;

        // ОПТИМИЗАЦИЯ 2: Избавляемся от Math.pow для затухания высоты
        final double h = 0.625;
        double term2_decay;
        if (absY <= h) {
            term2_decay = 1.0;
        } else {
            double decay = (1.0 - absY) / 0.375; // (1.0 - 0.625) предрасчитано
            term2_decay = decay * decay;
        }

        // ОПТИМИЗАЦИЯ 3: Early Out для граней.
        if (term1 > term2_decay * MAX_POLY_MULTIPLIER) return false;
        if (term1 < term2_decay * 1.0) return true;

        // Считаем тяжелую математику (тригонометрию) только для тонкой "корки" кристалла
        double atan = Math.atan2(fZ, fX);
        double sinPart = Math.sin(4.0 * atan); // (s / 2.0) -> 8 / 2 = 4.0
        double asinPart = Math.asin(sinPart);
        double cosPart = Math.cos(0.25 * asinPart); // (2.0 / s) -> 2 / 8 = 0.25

        double term2_poly = 1.0 / (cosPart * cosPart);

        return term1 < (term2_decay * term2_poly);
    }
}