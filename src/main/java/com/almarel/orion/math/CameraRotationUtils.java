package com.almarel.orion.math;

import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Утилита для вычисления вращений в пространстве камеры (Camera Space).
 * Решает проблему Gimbal Lock путем матричного перемножения (Matrix Concatenation).
 */
public class CameraRotationUtils {

    /**
     * Применяет инкрементальный поворот к матрице с учетом направления взгляда игрока.
     *
     * @param currentMatrix Текущая матрица 3x3
     * @param playerYaw     Yaw игрока
     * @param action        Действие (YAW, PITCH, ROLL)
     * @param delta         Градусы смещения (+ / -)
     * @return Новая матрица 3x3
     */
    public static double[] calculateCameraSpaceRotation(double[] currentMatrix, float playerYaw, String action, int delta) {
        AffineTransform current = new AffineTransform();
        if (currentMatrix != null && currentMatrix.length == 9) {
            current = new AffineTransform(currentMatrix);
        }

        AffineTransform deltaRot = new AffineTransform();

        // Нормализуем Yaw в диапазон от 0 до 360
        float yaw = (playerYaw % 360 + 360) % 360;

        if (action.equalsIgnoreCase("YAW")) {
            // Yaw (Влево/Вправо) всегда вращает вертикальную ось Y
            deltaRot = deltaRot.rotateY(delta);
        } else {
            // Определяем сторону света (Quadrant)
            boolean isLookingSouth = (yaw >= 315 || yaw < 45);   // +Z
            boolean isLookingWest  = (yaw >= 45 && yaw < 135);   // -X
            boolean isLookingNorth = (yaw >= 135 && yaw < 225);  // -Z
            boolean isLookingEast  = (yaw >= 225 && yaw < 315);  // +X

            if (action.equalsIgnoreCase("PITCH")) {
                // Pitch (Вверх/Вниз) вращает ось, перпендикулярную взгляду
                if (isLookingSouth || isLookingNorth) {
                    int sign = isLookingNorth ? -1 : 1;
                    deltaRot = deltaRot.rotateX(delta * sign);
                } else { // West или East
                    int sign = isLookingEast ? -1 : 1;
                    deltaRot = deltaRot.rotateZ(delta * sign);
                }
            }
            else if (action.equalsIgnoreCase("ROLL")) {
                // Roll (Наклон) вращает ось, параллельную взгляду
                if (isLookingSouth || isLookingNorth) {
                    int sign = isLookingNorth ? -1 : 1;
                    deltaRot = deltaRot.rotateZ(delta * sign);
                } else { // West или East
                    int sign = isLookingWest ? -1 : 1;
                    deltaRot = deltaRot.rotateX(delta * sign);
                }
            }
        }

        // [CRITICAL] ГЛОБАЛЬНОЕ ВРАЩЕНИЕ.
        // Мы применяем deltaRot к текущей матрице снаружи (pre-concatenate),
        // чтобы ось вращения всегда была привязана к миру, а не к уже наклоненному объекту.
        AffineTransform result = deltaRot.combine(current);

        // Возвращаем новую матрицу 3x3 (извлекаем из 12-элементного массива коэффициентов)
        double[] c = result.coefficients();
        return new double[]{ c[0], c[1], c[2], c[4], c[5], c[6], c[8], c[9], c[10] };
    }
}