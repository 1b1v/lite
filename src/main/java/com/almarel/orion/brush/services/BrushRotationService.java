package com.almarel.orion.brush.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.capabilities.ConstructTransformable;
import com.almarel.orion.brush.data.capabilities.Rotatable;
import com.almarel.orion.brush.data.capabilities.RotationModeable;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;

/**
 * Сервис для управления состоянием вращения активного буфера кисти.
 */
@Singleton
public class BrushRotationService {

    private final MessageManager messageManager;

    @Inject
    public BrushRotationService(
            MessageManager messageManager) {
        this.messageManager = messageManager;
    }


    /**
     * Единая точка входа для вращения Кистей (Brush). Обновляет матрицу и визуал.
     */
    public void applyBrushRotation(org.bukkit.entity.Player player, Rotatable state, String action, int delta) {
        if ("RESET".equalsIgnoreCase(action)) {
            state.setRotationMatrix(new double[]{1, 0, 0,  0, 1, 0,  0, 0, 1});
            return;
        }

        // [FIX] Локальное vs Глобальное пространство
        float yaw = player.getLocation().getYaw();
        if (state instanceof RotationModeable rotModeable) {
            if (rotModeable.getRotationMode() != com.almarel.orion.brush.data.enums.BrushRotationMode.OFF) {
                // Если кисть привязана к камере, мы фиксируем базис (Yaw = 0), чтобы матрица стала Локальной
                yaw = 0f;
            }
        }

        double[] newMatrix = com.almarel.orion.math.CameraRotationUtils.calculateCameraSpaceRotation(
                state.getRotationMatrix(), yaw, action, delta
        );
        state.setRotationMatrix(newMatrix);
    }

    /**
     * Единая точка входа для вращения Конструктов (Construct). Обновляет матрицу и визуал.
     */
    public void applyConstructRotation(org.bukkit.entity.Player player, ConstructTransformable state, String action, int delta) {
        if ("RESET".equalsIgnoreCase(action)) {
            state.setConstructRotationMatrix(new double[]{1, 0, 0,  0, 1, 0,  0, 0, 1});
            return;
        }

        // [FIX] Локальное vs Глобальное пространство
        float yaw = player.getLocation().getYaw();
        if (state instanceof RotationModeable rotModeable) {
            if (rotModeable.getRotationMode() != com.almarel.orion.brush.data.enums.BrushRotationMode.OFF) {
                yaw = 0f;
            }
        }

        double[] newMatrix = com.almarel.orion.math.CameraRotationUtils.calculateCameraSpaceRotation(
                state.getConstructRotationMatrix(), yaw, action, delta
        );
        state.setConstructRotationMatrix(newMatrix);
    }
}