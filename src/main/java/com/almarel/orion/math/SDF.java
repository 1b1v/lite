package com.almarel.orion.math;

/**
 * Signed Distance Field (Function).
 * Универсальный контракт для любой геометрической формы.
 * Работает СТРОГО в Локальных Координатах (относительно 0,0,0).
 */
@FunctionalInterface
public interface SDF {
    /**
     * @param x Локальная координата X
     * @param y Локальная координата Y
     * @param z Локальная координата Z
     * @param noise Генератор шума (может быть null, если форма простая)
     * @return true, если точка внутри
     */
    boolean isInside(double x, double y, double z, FastNoise noise);
}