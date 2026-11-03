package com.almarel.orion.brush.data.capabilities;

public interface Rotatable {
    /**
     * Возвращает матрицу вращения 3x3 в виде одномерного массива (9 элементов).
     */
    double[] getRotationMatrix();

    void setRotationMatrix(double[] matrix);
}