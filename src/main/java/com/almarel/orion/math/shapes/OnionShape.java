package com.almarel.orion.math.shapes;

import com.almarel.orion.math.FastNoise;
import com.almarel.orion.math.SDF;

/**
 * SDF реализация Луковицы.
 * Оптимизирована: Math.pow заменен на прямое умножение.
 */
public class OnionShape implements SDF {

    // Прекомпилированные множители
    private final double invRadiusX;
    private final double invRadiusZ;
    private final double invHeight2; // 2.0 / height

    public OnionShape(double width, double height, double length) {
        double rX = Math.max(0.5, width / 2.0);
        double rZ = Math.max(0.5, length / 2.0);
        double h = Math.max(1.0, height - 1.0);

        this.invRadiusX = 1.0 / rX;
        this.invRadiusZ = 1.0 / rZ;
        this.invHeight2 = 2.0 / h;
    }

    @Override
    public boolean isInside(double localX, double localY, double localZ, FastNoise noise) {
        // ОПТИМИЗАЦИЯ 1: Early Out. При localY < 0 правая часть формулы всегда отрицательна,
        // а сумма квадратов (leftSide) >= 0. Блок 100% снаружи.
        if (localY < 0.0) return false;

        double nx = localX * invRadiusX;
        double nz = localZ * invRadiusZ;

        // Математическое упрощение: (localY / height) * 2.0 - 1.0 + 1.0 -> localY * (2.0 / height)
        double tempY = localY * invHeight2;

        double leftSide = (nx * nx) + (nz * nz);
        double tempY15 = 1.5 * tempY;
        double rightSide = 4.0 * tempY * Math.exp(-(tempY15 * tempY15));

        return leftSide <= rightSide;
    }
}