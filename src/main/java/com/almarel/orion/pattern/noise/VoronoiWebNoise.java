package com.almarel.orion.pattern.noise;

import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.math.noise.VoronoiNoise;

public class VoronoiWebNoise implements NoiseGenerator {

    private final VoronoiNoise delegate;

    // Допуск разницы значений между ячейками для определения границы
    private static final float CELL_DIFFERENCE_TOLERANCE = 0.01f;

    public VoronoiWebNoise(VoronoiNoise delegate) {
        this.delegate = delegate;
    }

    @Override
    public float noise(Vector2 position) {
        float centerValue = delegate.noise(position);
        double px = position.getX();
        double pz = position.getZ();

        if (isBoundaryWithNeighbor(centerValue, px + 1, pz)
                || isBoundaryWithNeighbor(centerValue, px - 1, pz)
                || isBoundaryWithNeighbor(centerValue, px, pz + 1)
                || isBoundaryWithNeighbor(centerValue, px, pz - 1)) {
            return 1.0f;
        }

        return 0.0f;
    }

    @Override
    public float noise(Vector3 position) {
        return noise(Vector2.at(position.getX(), position.getZ()));
    }

    /**
     * Ленивая проверка перепада высот с соседом для детекции ребра диаграммы Вороного.
     */
    private boolean isBoundaryWithNeighbor(float centerValue, double neighborX, double neighborZ) {
        float neighborValue = delegate.noise(Vector2.at(neighborX, neighborZ));
        return (centerValue - neighborValue) > CELL_DIFFERENCE_TOLERANCE;
    }
}