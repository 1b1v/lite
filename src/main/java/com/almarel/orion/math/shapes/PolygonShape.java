package com.almarel.orion.math.shapes;

import com.almarel.orion.math.FastNoise;
import com.almarel.orion.math.SDF;

/**
 * SDF реализация правильного многоугольника (Призмы).
 * Оптимизирована с использованием Incircle/Circumcircle Early Out.
 */
public class PolygonShape implements SDF {

    private final int vertices;
    private final boolean hollow;
    private final double normalizedThickness;

    private final double limit;
    private final double limitSq;

    // Прекомпилированные множители
    private final double invRadiusX;
    private final double invRadiusZ;
    private final double segmentAngle;
    private final double invSegmentAngle;
    private final double piOverV;

    public PolygonShape(double width, double length, int vertices, boolean hollow) {
        double rX = width / 2.0;
        double rZ = length / 2.0;
        this.vertices = vertices;
        this.hollow = hollow;

        this.invRadiusX = (rX == 0) ? 0 : 1.0 / rX;
        this.invRadiusZ = (rZ == 0) ? 0 : 1.0 / rZ;

        this.segmentAngle = 2 * Math.PI / vertices;
        this.invSegmentAngle = 1.0 / this.segmentAngle;
        this.piOverV = Math.PI / vertices;

        this.limit = Math.cos(this.piOverV);
        this.limitSq = this.limit * this.limit;

        double minR = Math.min(rX, rZ);
        double rawThickness = (minR > 0) ? (1.0 / minR) : 1.0;
        this.normalizedThickness = rawThickness * 1.01;
    }

    @Override
    public boolean isInside(double x, double y, double z, FastNoise noise) {
        double nx = x * invRadiusX;
        double nz = z * invRadiusZ;

        double distSq = (nx * nx) + (nz * nz);

        // ОПТИМИЗАЦИЯ 1 (Circumcircle): Блок находится за пределами описанной окружности
        if (distSq > 1.0001) return false;

        // ОПТИМИЗАЦИЯ 2 (Incircle): Блок глубоко внутри вписанной окружности.
        if (!hollow && distSq <= limitSq) return true;

        double r = Math.sqrt(distSq);
        double angle = Math.atan2(nz, nx) + piOverV;

        // ОПТИМИЗАЦИЯ 3 (Fast Modulo): Заменяем медленный double % на умножение и Math.floor
        double sum = angle + Math.PI;
        double localAngle = (sum - Math.floor(sum * invSegmentAngle) * segmentAngle) - piOverV;

        double distanceToEdge = r * Math.cos(localAngle);

        if (distanceToEdge > limit) return false;

        if (hollow) {
            return distanceToEdge >= limit - normalizedThickness;
        }

        return true;
    }
}