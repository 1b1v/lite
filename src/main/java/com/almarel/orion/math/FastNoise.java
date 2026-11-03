package com.almarel.orion.math;

import java.util.Random;

/**
 * CPU-Оптимизированная математическая реализация 3D-шума Перлина.
 * Использует Fast Floor и бессвитчевые (branchless) градиенты для ускорения конвейера.
 */
public class FastNoise {

    private final int[] p = new int[512];

    // Предвычисленные градиентные векторы для 16 возможных направлений.
    // Избавляет процессор от тяжелых тернарных операторов ветвления в методе grad().
    private static final double[] GRAD_X = {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0};
    private static final double[] GRAD_Y = {1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1};
    private static final double[] GRAD_Z = {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1};

    public FastNoise(int seed) {
        Random rand = new Random(seed);
        for (int i = 0; i < 256; i++) p[i] = i;
        for (int i = 0; i < 256; i++) {
            int j = rand.nextInt(256);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }
        for (int i = 0; i < 256; i++) p[256 + i] = p[i];
    }

    public double eval(double x, double y, double z) {
        // Fast Floor: Кастование в (int) в 4 раза быстрее вызова Math.floor()
        int x0 = x > 0.0 ? (int) x : (int) x - 1;
        int y0 = y > 0.0 ? (int) y : (int) y - 1;
        int z0 = z > 0.0 ? (int) z : (int) z - 1;

        int X = x0 & 255;
        int Y = y0 & 255;
        int Z = z0 & 255;

        x -= x0;
        y -= y0;
        z -= z0;

        // Инлайн функции fade. Умножение быстрее вызова метода.
        double u = x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
        double v = y * y * y * (y * (y * 6.0 - 15.0) + 10.0);
        double w = z * z * z * (z * (z * 6.0 - 15.0) + 10.0);

        int A  = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B  = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        return lerp(w,
                lerp(v,
                        lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1.0, y, z)),
                        lerp(u, grad(p[AB], x, y - 1.0, z), grad(p[BB], x - 1.0, y - 1.0, z))
                ),
                lerp(v,
                        lerp(u, grad(p[AA + 1], x, y, z - 1.0), grad(p[BA + 1], x - 1.0, y, z - 1.0)),
                        lerp(u, grad(p[AB + 1], x, y - 1.0, z - 1.0), grad(p[BB + 1], x - 1.0, y - 1.0, z - 1.0))
                )
        );
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    // Branchless (без ветвлений) вычисление градиента по хэшу
    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        return GRAD_X[h] * x + GRAD_Y[h] * y + GRAD_Z[h] * z;
    }
}