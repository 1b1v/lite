package com.almarel.orion.math;

public class ProceduralTransform {
    public final double ox, oy, oz;
    public final double sx, sy, sz;
    public final double rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22;
    public final boolean hasRotation;
    public final FastNoise noise;

    // [ORION FIX]: Увеличена точность проверки на единичную матрицу
    public ProceduralTransform(double x, double y, double z, double sx, double sy, double sz, double[] rm, FastNoise noise) {
        this.ox = x; this.oy = y; this.oz = z;
        this.sx = sx; this.sy = sy; this.sz = sz;
        this.noise = noise;

        // Проверка на единичную матрицу (Identity) с высокой точностью
        boolean isIdentity = Math.abs(rm[0] - 1.0) < 1e-5 && Math.abs(rm[1]) < 1e-5 && Math.abs(rm[2]) < 1e-5 &&
                Math.abs(rm[3]) < 1e-5 && Math.abs(rm[4] - 1.0) < 1e-5 && Math.abs(rm[5]) < 1e-5 &&
                Math.abs(rm[6]) < 1e-5 && Math.abs(rm[7]) < 1e-5 && Math.abs(rm[8] - 1.0) < 1e-5;

        this.hasRotation = !isIdentity;

        this.rm00 = rm[0]; this.rm01 = rm[1]; this.rm02 = rm[2];
        this.rm10 = rm[3]; this.rm11 = rm[4]; this.rm12 = rm[5];
        this.rm20 = rm[6]; this.rm21 = rm[7]; this.rm22 = rm[8];
    }
}