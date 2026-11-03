package com.almarel.orion.math;

/**
 * Единая точка истины для параметров гауссова блюра, используемого
 * воксельными "движками" паттернов (DensityMatrix для #smooth,
 * SlopeSmoothingField для #grass).
 */
public final class GaussianBlurKernel {

    private GaussianBlurKernel() {
    }

    /**
     * Переводит пользовательский параметр "сила" (1-5) в радиус блюра
     * в блоках. Единая формула для всех паттернов, использующих блюр.
     */
    public static int resolveRadius(int power) {
        return Math.max(3, Math.min(8, power + 2));
    }

    /**
     * Генерирует нормализованное 1D гауссово ядро заданного радиуса.
     */
    public static float[] generate1DKernel(int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sigma = Math.max(2.0f, radius / 1.2f);
        float sum = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float weight = (float) Math.exp(-(i * i) / (2 * sigma * sigma));
            kernel[i + radius] = weight;
            sum += weight;
        }

        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }
}