package com.almarel.orion.pattern.flora;

import com.almarel.orion.math.VolumetricBlur;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.almarel.orion.framework.integration.fawe.RegionChunkScanner;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.math.GaussianBlurKernel;

import java.util.*;

/**
 * Воксельный "движок" для #grass. Строит два скалярных поля плотности
 * разного масштаба размытия вокруг выделения (1.0 = твёрдый блок, 0.0 = воздух):
 * <p>
 * - fineDensity (слабый блюр) — для точного детектирования РЕАЛЬНОГО обрыва,
 *   без искажения соседним пологим склоном.
 * - coarseDensity (сильный блюр) — для macro-формы рельефа: определения
 *   "заведомо плоских" стартовых точек заливки и сравнения плавности перехода
 *   между соседними вокселями.
 * <p>
 * Покрас определяется не локальным углом отдельного вокселя (как в первой
 * версии паттерна), а Multi-Source BFS Region Growing: заливка стартует от
 * плоских мест и растекается по поверхности, пока переход между соседними
 * нормалями остаётся плавным (coarse) И абсолютный угол не превышает жёсткий
 * потолок (fine). Это позволяет красить гладко изогнутые формы (купол, волна)
 * целиком, даже если локально они вертикальны, но останавливает заливку на
 * резких изломах (утёс, скол) — независимо от их локального угла.
 */
public class SlopeSmoothingField {

    public static final long MAX_VOLUME = 40_000_000L;

    private static final int FINE_RADIUS = 2;
    private static final int COARSE_RADIUS = 5;

    private static final double SEED_MAX_ANGLE_DEG = 25.0;
    private static final double PROPAGATION_MAX_STEP_ANGLE_DEG = 18.0;
    private static final double FINE_PROPAGATION_MAX_STEP_ANGLE_DEG = 30.0;
    private static final double HARD_CEILING_ANGLE_DEG = 86.0;

    private static final double GRADIENT_EPSILON = 1e-3;
    private static final double GRADIENT_EPSILON_SQ = GRADIENT_EPSILON * GRADIENT_EPSILON;

    public static long computeVolume(Region region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        long width = (long) (max.getBlockX() - min.getBlockX() + 1) + COARSE_RADIUS * 2L;
        long height = (long) (max.getBlockY() - min.getBlockY() + 1) + COARSE_RADIUS * 2L;
        long length = (long) (max.getBlockZ() - min.getBlockZ() + 1) + COARSE_RADIUS * 2L;

        return width * height * length;
    }

    private final AsyncExecutor asyncExecutor;

    private final int startX, startY, startZ;
    private final int width, height, length;
    private final int sliceSize;

    private final int coreStartX, coreStartY, coreStartZ;
    private final int coreEndX, coreEndY, coreEndZ;

    private final double seedCosSq;
    private final double hardCeilingCosSq;
    private final double propagationCosThreshold;
    private final double finePropagationCosThreshold;

    private BitSet originalAir;
    private float[] fineDensity;
    private float[] coarseDensity;
    private BitSet paintMask;

    private final boolean isValid;

    public SlopeSmoothingField(World world, Region region, AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;

        double seedCos = Math.cos(Math.toRadians(SEED_MAX_ANGLE_DEG));
        this.seedCosSq = seedCos * seedCos;

        double ceilingCos = Math.cos(Math.toRadians(HARD_CEILING_ANGLE_DEG));
        this.hardCeilingCosSq = ceilingCos * ceilingCos;

        this.propagationCosThreshold = Math.cos(Math.toRadians(PROPAGATION_MAX_STEP_ANGLE_DEG));
        this.finePropagationCosThreshold = Math.cos(Math.toRadians(FINE_PROPAGATION_MAX_STEP_ANGLE_DEG));

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        this.coreStartX = min.getBlockX();
        this.coreStartY = min.getBlockY();
        this.coreStartZ = min.getBlockZ();
        this.coreEndX = max.getBlockX();
        this.coreEndY = max.getBlockY();
        this.coreEndZ = max.getBlockZ();

        this.startX = coreStartX - COARSE_RADIUS;
        this.startY = coreStartY - COARSE_RADIUS;
        this.startZ = coreStartZ - COARSE_RADIUS;

        this.width = (coreEndX - coreStartX + 1) + COARSE_RADIUS * 2;
        this.height = (coreEndY - coreStartY + 1) + COARSE_RADIUS * 2;
        this.length = (coreEndZ - coreStartZ + 1) + COARSE_RADIUS * 2;
        this.sliceSize = this.width * this.length;

        long volume = (long) width * height * length;

        if (volume > MAX_VOLUME || volume <= 0) {
            this.isValid = false;
            this.originalAir = new BitSet(0);
            this.fineDensity = new float[0];
            this.coarseDensity = new float[0];
            this.paintMask = new BitSet(0);
            return;
        }

        this.originalAir = new BitSet((int) volume);
        float[] solidRaw = new float[(int) volume];

        boolean foundSolid = populateGrid(world, solidRaw);

        if (!foundSolid) {
            this.isValid = false;
            this.fineDensity = new float[0];
            this.coarseDensity = new float[0];
            this.paintMask = new BitSet(0);
            return;
        }

        this.fineDensity = VolumetricBlur.blur(solidRaw, width, height, length,
                FINE_RADIUS, GaussianBlurKernel.generate1DKernel(FINE_RADIUS), asyncExecutor);
        this.coarseDensity = VolumetricBlur.blur(solidRaw, width, height, length,
                COARSE_RADIUS, GaussianBlurKernel.generate1DKernel(COARSE_RADIUS), asyncExecutor);
        solidRaw = null;

        this.paintMask = buildPaintMask();
        this.isValid = true;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean contains(int x, int y, int z) {
        return x >= coreStartX && x <= coreEndX &&
                y >= coreStartY && y <= coreEndY &&
                z >= coreStartZ && z <= coreEndZ;
    }

    public boolean shouldPaint(int x, int y, int z) {
        if (!contains(x, y, z)) {
            return false;
        }
        int index = getIndex(x - startX, y - startY, z - startZ);
        return paintMask.get(index);
    }

    public boolean canGrowAt(int x, int y, int z) {
        return !isAir(x, y, z) && isAir(x, y + 1, z);
    }

    public int countAirContacts(int x, int y, int z) {
        int contacts = 0;
        if (isAir(x + 1, y, z)) contacts++;
        if (isAir(x - 1, y, z)) contacts++;
        if (isAir(x, y + 1, z)) contacts++;
        if (isAir(x, y - 1, z)) contacts++;
        if (isAir(x, y, z + 1)) contacts++;
        if (isAir(x, y, z - 1)) contacts++;
        return contacts;
    }

    // --- Построение полей ---

    private boolean populateGrid(World world, float[] solidRaw) {
        boolean[] foundSolid = {false};

        CuboidRegion scanRegion = new CuboidRegion(
                BlockVector3.at(startX, startY, startZ),
                BlockVector3.at(startX + width - 1, startY + height - 1, startZ + length - 1)
        );

        RegionChunkScanner.forEachBlock(world, scanRegion, (wx, wy, wz, block) -> {
            int index = getIndex(wx - startX, wy - startY, wz - startZ);
            if (block.isAir()) {
                originalAir.set(index);
            } else {
                solidRaw[index] = 1.0f;
                foundSolid[0] = true;
            }
        });

        return foundSolid[0];
    }

    // --- Region Growing (Multi-Source BFS) ---

    private BitSet buildPaintMask() {
        int volume = width * height * length;
        BitSet mask = new BitSet(volume);
        FloodQueue queue = new FloodQueue(4096);

        seedQueue(mask, queue);
        growFromSeeds(mask, queue);

        return mask;
    }

    private void seedQueue(BitSet mask, FloodQueue queue) {
        for (int y = coreStartY; y <= coreEndY; y++) {
            for (int z = coreStartZ; z <= coreEndZ; z++) {
                for (int x = coreStartX; x <= coreEndX; x++) {
                    if (!canGrowAt(x, y, z)) continue;
                    if (!passesSeed(x, y, z) || !passesCeiling(x, y, z)) continue;

                    double cgx = gradX(coarseDensity, x, y, z);
                    double cgy = gradY(coarseDensity, x, y, z);
                    double cgz = gradZ(coarseDensity, x, y, z);
                    double cMag = Math.sqrt(cgx * cgx + cgy * cgy + cgz * cgz);
                    if (cMag < GRADIENT_EPSILON) continue;

                    double fgx = gradX(fineDensity, x, y, z);
                    double fgy = gradY(fineDensity, x, y, z);
                    double fgz = gradZ(fineDensity, x, y, z);
                    double fMag = Math.sqrt(fgx * fgx + fgy * fgy + fgz * fgz);
                    if (fMag < GRADIENT_EPSILON) continue;

                    int index = getIndex(x - startX, y - startY, z - startZ);
                    mask.set(index);
                    queue.add(index,
                            (float) cgx, (float) cgy, (float) cgz,
                            (float) fgx, (float) fgy, (float) fgz);
                }
            }
        }
    }

    private void growFromSeeds(BitSet mask, FloodQueue queue) {
        while (!queue.isEmpty()) {
            int index = queue.pollIndex();

            int ly = index / sliceSize;
            int rem = index % sliceSize;
            int lz = rem / width;
            int lx = rem % width;

            expandToNeighbors(mask, queue,
                    lx + startX, ly + startY, lz + startZ,
                    queue.lastCoarseX, queue.lastCoarseY, queue.lastCoarseZ,
                    queue.lastFineX, queue.lastFineY, queue.lastFineZ);
        }
    }

    /**
     * Та же логика "referenced-нормали" (см. предыдущий рефакторинг), но без
     * единого выделения объектов FloodNode/Grad — все векторы передаются как
     * примитивы через параметры и хранятся в FloodQueue как parallel float[].
     * Это устраняет GC-нагрузку на поверхностном слое объёмных регионов,
     * не меняя математику и результат заливки.
     */
    private void expandToNeighbors(BitSet mask, FloodQueue queue,
                                   int cx, int cy, int cz,
                                   double refCoarseX, double refCoarseY, double refCoarseZ,
                                   double refFineX, double refFineY, double refFineZ) {
        double refCoarseMag = Math.sqrt(refCoarseX * refCoarseX + refCoarseY * refCoarseY + refCoarseZ * refCoarseZ);
        double refFineMag = Math.sqrt(refFineX * refFineX + refFineY * refFineY + refFineZ * refFineZ);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    int nx = cx + dx;
                    int ny = cy + dy;
                    int nz = cz + dz;

                    if (!contains(nx, ny, nz)) continue;

                    int nIndex = getIndex(nx - startX, ny - startY, nz - startZ);
                    if (mask.get(nIndex)) continue;
                    if (!canGrowAt(nx, ny, nz)) continue;
                    if (!passesCeiling(nx, ny, nz)) continue;

                    double candCoarseX = gradX(coarseDensity, nx, ny, nz);
                    double candCoarseY = gradY(coarseDensity, nx, ny, nz);
                    double candCoarseZ = gradZ(coarseDensity, nx, ny, nz);
                    double candCoarseMag = Math.sqrt(candCoarseX * candCoarseX + candCoarseY * candCoarseY + candCoarseZ * candCoarseZ);
                    if (candCoarseMag < GRADIENT_EPSILON) continue;

                    double coarseCos = (refCoarseX * candCoarseX + refCoarseY * candCoarseY + refCoarseZ * candCoarseZ)
                            / (refCoarseMag * candCoarseMag);
                    if (coarseCos < propagationCosThreshold) continue;

                    double candFineX = gradX(fineDensity, nx, ny, nz);
                    double candFineY = gradY(fineDensity, nx, ny, nz);
                    double candFineZ = gradZ(fineDensity, nx, ny, nz);
                    double candFineMag = Math.sqrt(candFineX * candFineX + candFineY * candFineY + candFineZ * candFineZ);
                    if (candFineMag < GRADIENT_EPSILON) continue;

                    double fineCos = (refFineX * candFineX + refFineY * candFineY + refFineZ * candFineZ)
                            / (refFineMag * candFineMag);
                    if (fineCos < finePropagationCosThreshold) continue;

                    mask.set(nIndex);

                    boolean isClean = countAirContacts(nx, ny, nz) < 2;
                    queue.add(nIndex,
                            (float) (isClean ? candCoarseX : refCoarseX),
                            (float) (isClean ? candCoarseY : refCoarseY),
                            (float) (isClean ? candCoarseZ : refCoarseZ),
                            (float) (isClean ? candFineX : refFineX),
                            (float) (isClean ? candFineY : refFineY),
                            (float) (isClean ? candFineZ : refFineZ));
                }
            }
        }
    }

    private boolean passesSeed(int x, int y, int z) {
        double gx = gradX(coarseDensity, x, y, z);
        double gy = gradY(coarseDensity, x, y, z);
        double gz = gradZ(coarseDensity, x, y, z);

        if (gy >= 0) return false;

        double sqLength = gx * gx + gy * gy + gz * gz;
        if (sqLength < GRADIENT_EPSILON_SQ) return false;

        double cosSq = (gy * gy) / sqLength;
        return cosSq >= seedCosSq;
    }

    private boolean passesCeiling(int x, int y, int z) {
        double gx = gradX(fineDensity, x, y, z);
        double gy = gradY(fineDensity, x, y, z);
        double gz = gradZ(fineDensity, x, y, z);

        if (gy >= 0) return false;

        double sqLength = gx * gx + gy * gy + gz * gz;
        if (sqLength < GRADIENT_EPSILON_SQ) return false;

        double cosSq = (gy * gy) / sqLength;
        return cosSq >= hardCeilingCosSq;
    }

    // --- Градиент и доступ к данным ---

    private double gradX(float[] field, int x, int y, int z) {
        return getFieldValue(field, x + 1, y, z) - getFieldValue(field, x - 1, y, z);
    }

    private double gradY(float[] field, int x, int y, int z) {
        return getFieldValue(field, x, y + 1, z) - getFieldValue(field, x, y - 1, z);
    }

    private double gradZ(float[] field, int x, int y, int z) {
        return getFieldValue(field, x, y, z + 1) - getFieldValue(field, x, y, z - 1);
    }

    private float getFieldValue(float[] field, int x, int y, int z) {
        int lx = clamp(x - startX, 0, width - 1);
        int ly = clamp(y - startY, 0, height - 1);
        int lz = clamp(z - startZ, 0, length - 1);
        return field[getIndex(lx, ly, lz)];
    }

    private boolean isAir(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return false;
        }
        int index = getIndex(x - startX, y - startY, z - startZ);
        return originalAir.get(index);
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= startX && y >= startY && z >= startZ &&
                x < startX + width && y < startY + height && z < startZ + length;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int getIndex(int x, int y, int z) {
        return x + z * width + y * sliceSize;
    }

    /**
     * Примитивная кольцевая очередь для BFS: хранит индекс вокселя и 6 float
     * компонент (референсные нормали coarse/fine) в параллельных массивах —
     * без аллокации Java-объектов на каждый узел обхода.
     */
    private static final class FloodQueue {
        private int[] index;
        private float[] coarseX, coarseY, coarseZ, fineX, fineY, fineZ;
        private int head, tail, size;

        float lastCoarseX, lastCoarseY, lastCoarseZ, lastFineX, lastFineY, lastFineZ;

        FloodQueue(int initialCapacity) {
            int cap = Math.max(16, initialCapacity);
            index = new int[cap];
            coarseX = new float[cap];
            coarseY = new float[cap];
            coarseZ = new float[cap];
            fineX = new float[cap];
            fineY = new float[cap];
            fineZ = new float[cap];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void add(int idx, float cx, float cy, float cz, float fx, float fy, float fz) {
            if (size == index.length) {
                grow();
            }
            index[tail] = idx;
            coarseX[tail] = cx;
            coarseY[tail] = cy;
            coarseZ[tail] = cz;
            fineX[tail] = fx;
            fineY[tail] = fy;
            fineZ[tail] = fz;
            tail = (tail + 1) % index.length;
            size++;
        }

        int pollIndex() {
            int idx = index[head];
            lastCoarseX = coarseX[head];
            lastCoarseY = coarseY[head];
            lastCoarseZ = coarseZ[head];
            lastFineX = fineX[head];
            lastFineY = fineY[head];
            lastFineZ = fineZ[head];
            head = (head + 1) % index.length;
            size--;
            return idx;
        }

        private void grow() {
            int oldCap = index.length;
            int newCap = oldCap * 2;

            index = copyRing(index, oldCap, newCap);
            coarseX = copyRing(coarseX, oldCap, newCap);
            coarseY = copyRing(coarseY, oldCap, newCap);
            coarseZ = copyRing(coarseZ, oldCap, newCap);
            fineX = copyRing(fineX, oldCap, newCap);
            fineY = copyRing(fineY, oldCap, newCap);
            fineZ = copyRing(fineZ, oldCap, newCap);

            head = 0;
            tail = oldCap;
        }

        private int[] copyRing(int[] src, int oldCap, int newCap) {
            int[] dst = new int[newCap];
            for (int i = 0; i < oldCap; i++) {
                dst[i] = src[(head + i) % oldCap];
            }
            return dst;
        }

        private float[] copyRing(float[] src, int oldCap, int newCap) {
            float[] dst = new float[newCap];
            for (int i = 0; i < oldCap; i++) {
                dst[i] = src[(head + i) % oldCap];
            }
            return dst;
        }
    }
}