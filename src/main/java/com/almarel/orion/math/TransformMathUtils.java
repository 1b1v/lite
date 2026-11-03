package com.almarel.orion.math;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.almarel.orion.brush.data.enums.BrushRotationMode;

import java.util.List;

/**
 * Утилитарный класс для математических операций с трансформациями.
 * Содержит чистые функции, не зависящие от игрового состояния.
 */
public final class TransformMathUtils {

    /*
     * ============================================================================
     * [ARCHITECTURAL NOTE: PROCEDURAL VS CLIPBOARD ROTATIONS]
     * ============================================================================
     * Внимание: Математика поворотов для процедурных кистей (SDF) и буфера обмена
     * (Copypaste) преднамеренно различается.
     *
     * 1. Procedural (calculateRotation): Математические фигуры (Кристалл, Капля)
     * генерируются из вакуума в абсолютных координатах. Для вращения по взгляду им
     * нужна чистая FPS-матрица проекции камеры (Pitch * Yaw).
     *
     * 2. Copypaste (calculateAngleTransform): Скопированные объекты FAWE имеют
     * "врожденную" ориентацию (origin игрока при //copy). Чтобы вставить их как
     * "наклейку" по направлению взгляда, применяется жесткий хак `faweCompatRotation`
     * (rotateZ(180) + rotateX(90)). Если применить этот хак к SDF — они сплющатся.
     *
     * [ТЕХНИЧЕСКИЙ ДОЛГ / DOMAIN LEAK]:
     * Утилитный класс чистой математики не должен зависеть от DTO конкретных
     * кистей (CopyPasteBrushState) и хаков FAWE. В будущем логику вычисления
     * матриц следует вынести в паттерн Strategy: `ProceduralRotationStrategy`
     * и `ClipboardRotationStrategy`, оставив здесь только базовые операции матриц.
     * ============================================================================
     */

    private TransformMathUtils() {} // Private constructor for utility class

    /**
     * Создает матрицу поворота вокруг произвольной оси.
     */
    public static AffineTransform createRotationTransform(Vector3 axis, double angle) {
        double rad = Math.toRadians(angle);
        double c = Math.cos(rad);
        double s = Math.sin(rad);
        double c1 = 1 - c;
        double x = axis.x();
        double y = axis.y();
        double z = axis.z();
        return new AffineTransform(
                c + x * x * c1, x * y * c1 - z * s, x * z * c1 + y * s, 0,
                y * x * c1 + z * s, c + y * y * c1, y * z * c1 - x * s, 0,
                z * x * c1 - y * s, z * y * c1 + x * s, c + z * z * c1, 0
        );
    }

    /**
     * Округляет угол до ближайшего значения с заданным шагом и порогом.
     */
    public static float snapAngle(float angle, float step, float threshold) {
        float norm = angle % 360;
        if (norm < 0) norm += 360;
        float closest = Math.round(norm / step) * step;
        if (Math.abs(norm - closest) < threshold) { return closest; }
        return angle;
    }

    /**
     * Возвращает угол, округленный до 16 направлений (шаг 22.5 градуса).
     */
    public static float get16PointYaw(float yaw) {
        if (yaw < 0) yaw += 360f;
        int sector = Math.round(yaw / 22.5f);
        return (sector * 22.5f) % 360f;
    }

    /**
     * Возвращает кардинальный угол (0, 90, 180, 270).
     */
    public static float getCardinalYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        return Math.round(yaw / 90f) * 90f;
    }

    public static boolean isFlipped(Transform t) {
        if (t == null) return false;

        // Для проверки нам достаточно знать, как трансформируются базисные векторы
        Vector3 origin = Vector3.at(0, 0, 0);
        Vector3 unitX = Vector3.at(1, 0, 0);
        Vector3 unitY = Vector3.at(0, 1, 0);
        Vector3 unitZ = Vector3.at(0, 0, 1);

        Vector3 tOrigin = t.apply(origin);
        Vector3 tX = t.apply(unitX).subtract(tOrigin);
        Vector3 tY = t.apply(unitY).subtract(tOrigin);
        Vector3 tZ = t.apply(unitZ).subtract(tOrigin);

        // Считаем смешанное произведение (Triple Product) = Determinant
        double crossX = tX.getY() * tY.getZ() - tX.getZ() * tY.getY();
        double crossY = tX.getZ() * tY.getX() - tX.getX() * tY.getZ();
        double crossZ = tX.getX() * tY.getY() - tX.getY() * tY.getX();

        double det = crossX * tZ.getX() + crossY * tZ.getY() + crossZ * tZ.getZ();

        // Если определитель отрицательный -> было отражение
        return det < -0.0001;
    }

    // [ORION FIX]: Санитайзер для значений с плавающей запятой
    private static double clean(double value) {
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) < 1e-9) {
            return (double) rounded; // Жестко привязываем к целому числу (0.0, 1.0, -1.0)
        }
        return value;
    }

    // [ORION FIX]: Извлекает из любого Transform идеальную матрицу AffineTransform
    public static AffineTransform sanitizeTransform(Transform t) {
        if (t == null) return new AffineTransform();

        Vector3 origin = t.apply(Vector3.at(0, 0, 0));
        Vector3 vX = t.apply(Vector3.at(1, 0, 0)).subtract(origin);
        Vector3 vY = t.apply(Vector3.at(0, 1, 0)).subtract(origin);
        Vector3 vZ = t.apply(Vector3.at(0, 0, 1)).subtract(origin);

        double m00 = clean(vX.getX()), m01 = clean(vY.getX()), m02 = clean(vZ.getX()), m03 = clean(origin.getX());
        double m10 = clean(vX.getY()), m11 = clean(vY.getY()), m12 = clean(vZ.getY()), m13 = clean(origin.getY());
        double m20 = clean(vX.getZ()), m21 = clean(vY.getZ()), m22 = clean(vZ.getZ()), m23 = clean(origin.getZ());

        // Используем стандартный FAWE конструктор для 4x4 (или 3x4) матрицы смещений
        return new AffineTransform(
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23
        );
    }


    public static TransformContext calculateRotation(float playerYaw, float playerPitch, boolean angleMode, double[] rotMatrix) {
        AffineTransform viewTransform = new AffineTransform();

        if (angleMode) {
            // [ORION FIX]: Идеальная проекция World -> Camera Local.
            AffineTransform yawRot = new AffineTransform().rotateY(playerYaw);
            AffineTransform pitchRot = new AffineTransform().rotateX(-playerPitch);
            viewTransform = pitchRot.combine(yawRot);
        }

        AffineTransform manualTransform = new AffineTransform();
        boolean hasManualRotation = false;

        if (rotMatrix != null && rotMatrix.length == 9) {
            manualTransform = new AffineTransform(rotMatrix);
            hasManualRotation = !isIdentityMatrix(rotMatrix);
        }

        Transform combined = angleMode ? viewTransform.combine(manualTransform) : manualTransform;
        AffineTransform finalTransform = sanitizeTransform(combined);
        boolean doRotate = angleMode || hasManualRotation;

        return new TransformContext(finalTransform, doRotate);
    }

    public static boolean isIdentityMatrix(double[] matrix) {
        if (matrix == null || matrix.length != 9) return false;
        return Math.abs(matrix[0] - 1.0) < 0.01 && Math.abs(matrix[1]) < 0.01 && Math.abs(matrix[2]) < 0.01 &&
                Math.abs(matrix[3]) < 0.01 && Math.abs(matrix[4] - 1.0) < 0.01 && Math.abs(matrix[5]) < 0.01 &&
                Math.abs(matrix[6]) < 0.01 && Math.abs(matrix[7]) < 0.01 && Math.abs(matrix[8] - 1.0) < 0.01;
    }

    /**
     * Рассчитывает ограничивающую рамку (Bounding Box) региона после применения трансформации.
     * Чистая функция, необходима для расчета выделения после вставки и трансформации регионов.
     */
    public static BlockVector3[] calculateTransformedBounds(Region region, Transform transform) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        List<Vector3> corners = List.of(
                Vector3.at(min.getX(), min.getY(), min.getZ()), Vector3.at(max.getX(), min.getY(), min.getZ()),
                Vector3.at(min.getX(), max.getY(), min.getZ()), Vector3.at(min.getX(), min.getY(), max.getZ()),
                Vector3.at(max.getX(), max.getY(), min.getZ()), Vector3.at(max.getX(), min.getY(), max.getZ()),
                Vector3.at(min.getX(), max.getY(), max.getZ()), Vector3.at(max.getX(), max.getY(), max.getZ())
        );

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

        for (var corner : corners) {
            var tCorner = transform.apply(corner);
            minX = Math.min(minX, tCorner.getX()); minY = Math.min(minY, tCorner.getY()); minZ = Math.min(minZ, tCorner.getZ());
            maxX = Math.max(maxX, tCorner.getX()); maxY = Math.max(maxY, tCorner.getY()); maxZ = Math.max(maxZ, tCorner.getZ());
        }

        // [ORION FIX]: Добавляем эпсилон (1e-5) для защиты Bounding Box.
        BlockVector3 tMin = BlockVector3.at((int) Math.floor(minX + 1e-5), (int) Math.floor(minY + 1e-5), (int) Math.floor(minZ + 1e-5));
        BlockVector3 tMax = BlockVector3.at((int) Math.ceil(maxX - 1e-5), (int) Math.ceil(maxY - 1e-5), (int) Math.ceil(maxZ - 1e-5));

        return new BlockVector3[]{tMin, tMax};
    }

    public static class TransformContext {
        public final double m00, m10, m20;
        public final double m01, m11, m21;
        public final double m02, m12, m22;
        public final boolean doRotate;
        public final AffineTransform affine;

        public TransformContext(AffineTransform t, boolean doRotate) {
            this.affine = t;
            this.doRotate = doRotate;
            Vector3 vX = t.apply(Vector3.at(1, 0, 0));
            Vector3 vY = t.apply(Vector3.at(0, 1, 0));
            Vector3 vZ = t.apply(Vector3.at(0, 0, 1));
            this.m00 = vX.getX(); this.m10 = vX.getY(); this.m20 = vX.getZ();
            this.m01 = vY.getX(); this.m11 = vY.getY(); this.m21 = vY.getZ();
            this.m02 = vZ.getX(); this.m12 = vZ.getY(); this.m22 = vZ.getZ();
        }
    }

    /**
     * Определяет, требуется ли сброс матрицы трансформации при смене режима ротации.
     */
    public static boolean requiresMatrixReset(BrushRotationMode oldMode, BrushRotationMode newMode) {
        if (oldMode == null) oldMode = BrushRotationMode.OFF;
        if (newMode == null) newMode = BrushRotationMode.OFF;

        boolean wasRotationMode = (oldMode == BrushRotationMode.VIEW || oldMode == BrushRotationMode.SIXTEEN);
        boolean isNowGridMode = (newMode == BrushRotationMode.VIEW || newMode == BrushRotationMode.SIXTEEN || newMode == BrushRotationMode.RANDOM);
        boolean isNowRotationAny = isNowGridMode || (newMode == BrushRotationMode.ANGLE);

        return wasRotationMode != isNowRotationAny;
    }
}