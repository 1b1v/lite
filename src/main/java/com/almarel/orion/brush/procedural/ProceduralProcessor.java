package com.almarel.orion.brush.procedural;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.math.ProceduralTransform;
import com.almarel.orion.math.SDF;
import com.almarel.orion.math.TransformMathUtils;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Изолированный Data-Oriented движок рендеринга процедурных кистей.
 * Не является Singleton, создается строго через new для каждого рендера.
 */
public class ProceduralProcessor {

    private final List<ProceduralTransform> elements;
    private final TransformMathUtils.TransformContext ctx;
    private final SDF shapeStrategy;
    private final boolean angleMode;
    private final boolean swapDefaultYZ;
    private final AsyncExecutor asyncExecutor;

    private final int bMinX, bMinY, bMinZ;
    private final int width, height, length;
    private final int sliceSize, volume;

    private BitSet solidMask;
    private int blocksSet = 0;

    public ProceduralProcessor(CuboidRegion region, List<ProceduralTransform> elements,
                               TransformMathUtils.TransformContext ctx, SDF shapeStrategy,
                               boolean angleMode, boolean swapDefaultYZ, AsyncExecutor asyncExecutor) {
        this.elements = elements;
        this.ctx = ctx;
        this.shapeStrategy = shapeStrategy;
        this.angleMode = angleMode;
        this.swapDefaultYZ = swapDefaultYZ;
        this.asyncExecutor = asyncExecutor;

        this.bMinX = region.getMinimumPoint().getX();
        this.bMinY = region.getMinimumPoint().getY();
        this.bMinZ = region.getMinimumPoint().getZ();

        this.width = region.getMaximumPoint().getX() - bMinX + 1;
        this.height = region.getMaximumPoint().getY() - bMinY + 1;
        this.length = region.getMaximumPoint().getZ() - bMinZ + 1;

        this.sliceSize = width * length;
        this.volume = width * height * length;
        this.solidMask = new BitSet(volume);
    }

    public static CuboidRegion calculateBounds(List<ProceduralTransform> elements, TransformMathUtils.TransformContext ctx,
                                               double radius, boolean angleMode, boolean swapDefaultYZ) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        double rSdf = radius * 1.5 + 2.0;

        double[] corner = new double[3]; // Реиспользуем буфер для исключения аллокаций в цикле

        for (int eIdx = 0; eIdx < elements.size(); eIdx++) {
            ProceduralTransform e = elements.get(eIdx);
            for (int i = 0; i < 8; i++) {
                computeCornerWorldPosition(e, i, rSdf, ctx, angleMode, swapDefaultYZ, corner);

                double wx = corner[0];
                double wy = corner[1];
                double wz = corner[2];

                if (wx < minX) minX = wx; if (wx > maxX) maxX = wx;
                if (wy < minY) minY = wy; if (wy > maxY) maxY = wy;
                if (wz < minZ) minZ = wz; if (wz > maxZ) maxZ = wz;
            }
        }

        return createClampedBounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private static void computeCornerWorldPosition(ProceduralTransform e, int cornerIndex, double rSdf,
                                                   TransformMathUtils.TransformContext ctx,
                                                   boolean angleMode, boolean swapDefaultYZ, double[] out) {
        double cx = ((cornerIndex & 1) == 0 ? rSdf : -rSdf) * e.sx;
        double cy = ((cornerIndex & 2) == 0 ? rSdf : -rSdf) * e.sy;
        double cz = ((cornerIndex & 4) == 0 ? rSdf : -rSdf) * e.sz;

        double rx = cx, ry = cy, rz = cz;
        if (e.hasRotation) {
            rx = cx * e.rm00 + cy * e.rm10 + cz * e.rm20;
            ry = cx * e.rm01 + cy * e.rm11 + cz * e.rm21;
            rz = cx * e.rm02 + cy * e.rm12 + cz * e.rm22;
        }

        double tx = rx + e.ox;
        double mathY = ry + e.oy;
        double mathZ = rz + e.oz;

        double ty = (swapDefaultYZ && angleMode) ? mathY : (angleMode ? mathZ : mathY);
        double tz = (swapDefaultYZ && angleMode) ? mathZ : (angleMode ? mathY : mathZ);

        if (ctx.doRotate) {
            out[0] = tx * ctx.m00 + ty * ctx.m10 + tz * ctx.m20;
            out[1] = tx * ctx.m01 + ty * ctx.m11 + tz * ctx.m21;
            out[2] = tx * ctx.m02 + ty * ctx.m12 + tz * ctx.m22;
        } else {
            out[0] = tx;
            out[1] = swapDefaultYZ ? tz : ty;
            out[2] = swapDefaultYZ ? ty : tz;
        }
    }

    private static CuboidRegion createClampedBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        int bMinX = (int) Math.floor(minX); int bMaxX = (int) Math.ceil(maxX);
        int bMinY = (int) Math.floor(minY); int bMaxY = (int) Math.ceil(maxY);
        int bMinZ = (int) Math.floor(minZ); int bMaxZ = (int) Math.ceil(maxZ);

        if (bMinX > bMaxX) { bMinX = -1; bMaxX = 1; }
        if (bMinY > bMaxY) { bMinY = -1; bMaxY = 1; }
        if (bMinZ > bMaxZ) { bMinZ = -1; bMaxZ = 1; }

        return new CuboidRegion(BlockVector3.at(bMinX, bMinY, bMinZ), BlockVector3.at(bMaxX, bMaxY, bMaxZ));
    }

    public void computeGeometry() {
        int processors = Runtime.getRuntime().availableProcessors();
        int batchSize = Math.max(1, height / processors);

        List<Callable<Void>> tasks = createBatchTasks(batchSize);

        try {
            asyncExecutor.getExecutor().invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Callable<Void>> createBatchTasks(int batchSize) {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int startY = 0; startY < height; startY += batchSize) {
            final int sY = startY;
            final int eY = Math.min(startY + batchSize, height);

            tasks.add(() -> {
                BitSet localMask = new BitSet(volume);
                int localBlocksSet = voxelizeSlice(sY, eY, localMask);

                if (localBlocksSet > 0) {
                    mergeLocalMask(localMask, localBlocksSet);
                }
                return null;
            });
        }
        return tasks;
    }

    private int voxelizeSlice(int sY, int eY, BitSet localMask) {
        int count = 0;
        // ОПТИМИЗАЦИЯ L1 CACHE: Строгий порядок Y -> Z -> X
        for (int y = sY; y < eY; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    if (evaluateWorldPoint(x + bMinX, y + bMinY, z + bMinZ)) {
                        localMask.set(x + z * width + y * sliceSize);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void mergeLocalMask(BitSet localMask, int localBlocksSet) {
        synchronized (solidMask) {
            solidMask.or(localMask);
            blocksSet += localBlocksSet;
        }
    }

    private boolean evaluateWorldPoint(int worldX, int worldY, int worldZ) {
        double tx, ty, tz;
        if (ctx.doRotate) {
            tx = worldX * ctx.m00 + worldY * ctx.m01 + worldZ * ctx.m02;
            ty = worldX * ctx.m10 + worldY * ctx.m11 + worldZ * ctx.m12;
            tz = worldX * ctx.m20 + worldY * ctx.m21 + worldZ * ctx.m22;
        } else {
            tx = worldX;
            ty = swapDefaultYZ ? worldZ : worldY;
            tz = swapDefaultYZ ? worldY : worldZ;
        }

        double mathX = tx;
        double mathY = swapDefaultYZ && angleMode ? ty : (angleMode ? tz : ty);
        double mathZ = swapDefaultYZ && angleMode ? tz : (angleMode ? ty : tz);

        return isInsideElementsLocal(mathX, mathY, mathZ);
    }

    public void applyHollow() {
        if (blocksSet == 0) return;
        BitSet hollowMask = new BitSet(volume);
        int newBlocksSet = 0;

        for (int i = solidMask.nextSetBit(0); i >= 0; i = solidMask.nextSetBit(i + 1)) {
            int y = i / sliceSize;
            int z = (i / width) % length;
            int x = i % width;

            if (isBoundaryVoxel(x, y, z)) {
                hollowMask.set(i);
                newBlocksSet++;
            }
        }

        this.solidMask = hollowMask;
        this.blocksSet = newBlocksSet;
    }

    private boolean isBoundaryVoxel(int x, int y, int z) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    int nx = x + dx;
                    int ny = y + dy;
                    int nz = z + dz;

                    if (nx < 0 || nx >= width || ny < 0 || ny >= height || nz < 0 || nz >= length) {
                        return true;
                    }

                    int nIndex = nx + nz * width + ny * sliceSize;
                    if (!solidMask.get(nIndex)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public BlockVector3 calculateSurfaceAnchor(Vector direction, int offset, double radius) {
        if (blocksSet == 0) return BlockVector3.ZERO;

        double dirX = direction.getX(), dirY = direction.getY(), dirZ = direction.getZ();
        BlockVector3 bestPoint = findExtremeSurfacePoint(dirX, dirY, dirZ);

        if (bestPoint != null) {
            return applyAnchorOffset(bestPoint, dirX, dirY, dirZ, offset, radius);
        }
        return BlockVector3.ZERO;
    }

    private BlockVector3 findExtremeSurfacePoint(double dirX, double dirY, double dirZ) {
        double maxDot = -Double.MAX_VALUE;
        BlockVector3 bestPoint = null;

        for (int i = solidMask.nextSetBit(0); i >= 0; i = solidMask.nextSetBit(i + 1)) {
            int y = i / sliceSize;
            int z = (i / width) % length;
            int x = i % width;

            int worldX = bMinX + x;
            int worldY = bMinY + y;
            int worldZ = bMinZ + z;

            double dot = worldX * dirX + worldY * dirY + worldZ * dirZ;
            if (dot > maxDot) {
                maxDot = dot;
                bestPoint = BlockVector3.at(worldX, worldY, worldZ);
            }
        }
        return bestPoint;
    }

    private BlockVector3 applyAnchorOffset(BlockVector3 point, double dirX, double dirY, double dirZ, int offset, double radius) {
        int smartOffset = Math.max(1, (int)(radius * 0.08));
        int totalOffset = smartOffset + offset;

        int shiftX = (int) Math.round(-dirX * totalOffset);
        int shiftY = (int) Math.round(-dirY * totalOffset);
        int shiftZ = (int) Math.round(-dirZ * totalOffset);

        return BlockVector3.at(
                point.getX() + shiftX,
                point.getY() + shiftY,
                point.getZ() + shiftZ
        );
    }

    /**
     * Прямая вставка маски в мир с мгновенной генерацией фундамента (Smear).
     */
    public void flushToSession(EditSession session, Pattern finalPattern, BlockVector3 origin, int smearDepth) {
        if (blocksSet == 0) return;

        MutableBlockVector3 mutPos = new MutableBlockVector3();
        boolean isSimpleBlock = finalPattern instanceof BlockState;
        BlockState simpleState = isSimpleBlock ? (BlockState) finalPattern : null;

        int offsetX = origin.getX();
        int offsetY = origin.getY();
        int offsetZ = origin.getZ();

        boolean[] columnSmeared = smearDepth > 0 ? new boolean[width * length] : null;

        for (int i = solidMask.nextSetBit(0); i >= 0; i = solidMask.nextSetBit(i + 1)) {
            int y = i / sliceSize;
            int rem = i % sliceSize;
            int z = rem / width;
            int x = rem % width;

            int worldX = bMinX + x + offsetX;
            int worldY = bMinY + y + offsetY;
            int worldZ = bMinZ + z + offsetZ;

            if (smearDepth > 0) {
                int colIdx = x + z * width;
                if (!columnSmeared[colIdx]) {
                    columnSmeared[colIdx] = true;
                    applySmearColumn(session, finalPattern, simpleState, mutPos, worldX, worldY, worldZ, smearDepth);
                }
            }

            setSessionBlock(session, finalPattern, simpleState, mutPos, worldX, worldY, worldZ);
        }
    }

    private void applySmearColumn(EditSession session, Pattern pattern, BlockState simpleState,
                                  MutableBlockVector3 mutPos, int worldX, int worldY, int worldZ, int smearDepth) {
        for (int dy = 1; dy <= smearDepth; dy++) {
            setSessionBlock(session, pattern, simpleState, mutPos, worldX, worldY - dy, worldZ);
        }
    }

    private void setSessionBlock(EditSession session, Pattern pattern, BlockState simpleState,
                                 MutableBlockVector3 mutPos, int x, int y, int z) {
        mutPos.setComponents(x, y, z);
        try {
            if (simpleState != null) {
                session.setBlock(mutPos, simpleState);
            } else {
                pattern.apply(session, mutPos, mutPos);
            }
        } catch (Exception ignored) {}
    }

    private boolean isInsideElementsLocal(double mathX, double mathY, double mathZ) {
        for (int i = 0; i < elements.size(); i++) {
            ProceduralTransform e = elements.get(i);
            double lx = mathX - e.ox;
            double ly = mathY - e.oy;
            double lz = mathZ - e.oz;

            if (e.hasRotation) {
                double r_lx = lx * e.rm00 + ly * e.rm01 + lz * e.rm02;
                double r_ly = lx * e.rm10 + ly * e.rm11 + lz * e.rm12;
                double r_lz = lx * e.rm20 + ly * e.rm21 + lz * e.rm22;
                lx = r_lx; ly = r_ly; lz = r_lz;
            }

            lx /= e.sx;
            ly /= e.sy;
            lz /= e.sz;

            if (shapeStrategy.isInside(lx, ly, lz, e.noise)) {
                return true;
            }
        }
        return false;
    }
}