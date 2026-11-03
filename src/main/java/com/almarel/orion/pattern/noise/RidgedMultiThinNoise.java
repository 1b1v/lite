package com.almarel.orion.pattern.noise;

import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.math.noise.RidgedMultiFractalNoise;

public class RidgedMultiThinNoise implements NoiseGenerator {

    private final RidgedMultiFractalNoise delegate;

    // Пороги для формирования линии
    private static final float CUTOFF_MIN = 0.2f;           // Отсекаем глубокие равнины
    private static final float RIDGE_THRESHOLD = 0.4f;       // Порог для основного гребня
    private static final float STAIR_FILL_THRESHOLD = 0.2f; // Порог для заполнения стыков

    public RidgedMultiThinNoise(RidgedMultiFractalNoise delegate) {
        this.delegate = delegate;
    }

    @Override
    public float noise(Vector2 position) {
        float c = delegate.noise(position);
        if (c < CUTOFF_MIN) {
            return 0.0f;
        }

        double px = position.getX();
        double pz = position.getZ();

        // Ближние соседи
        float e = delegate.noise(Vector2.at(px + 1, pz));
        float w = delegate.noise(Vector2.at(px - 1, pz));
        float n = delegate.noise(Vector2.at(px, pz - 1));
        float s = delegate.noise(Vector2.at(px, pz + 1));

        if (isPrimaryRidge(c, e, w, n, s)) {
            return 1.0f;
        }

        if (isStairFill(c, px, pz, e, w, n, s)) {
            return 1.0f;
        }

        return 0.0f;
    }

    @Override
    public float noise(Vector3 position) {
        return noise(Vector2.at(position.getX(), position.getZ()));
    }

    /**
     * Стандартный NMS (Non-Maximum Suppression) толщиной в 1 блок для прямых линий.
     */
    private boolean isPrimaryRidge(float c, float e, float w, float n, float s) {
        if (c <= RIDGE_THRESHOLD) {
            return false;
        }
        boolean isRidgeX = (c >= e && c >= w);
        boolean isRidgeZ = (c >= n && c >= s);
        return isRidgeX || isRidgeZ;
    }

    /**
     * Stair Filler (Анти-лесенка): заполняет диагональные разрывы, если блок
     * находится на ортогональном стыке двух сформированных линий.
     */
    private boolean isStairFill(float c, double px, double pz, float e, float w, float n, float s) {
        if (c <= STAIR_FILL_THRESHOLD) {
            return false;
        }

        boolean eIsLine = isLineNeighbor(e, c, px + 2, pz);
        boolean wIsLine = isLineNeighbor(w, c, px - 2, pz);
        boolean nIsLine = isLineNeighbor(n, c, px, pz - 2);
        boolean sIsLine = isLineNeighbor(s, c, px, pz + 2);

        boolean hasHorizontalLineNeighbor = eIsLine || wIsLine;
        boolean hasVerticalLineNeighbor = nIsLine || sIsLine;

        return hasHorizontalLineNeighbor && hasVerticalLineNeighbor;
    }

    /**
     * Ленивая проверка соседа: дальний шум вычисляется только если ближний кандидат выше центра.
     */
    private boolean isLineNeighbor(float neighborVal, float centerVal, double farX, double farZ) {
        return neighborVal >= centerVal && neighborVal >= delegate.noise(Vector2.at(farX, farZ));
    }
}