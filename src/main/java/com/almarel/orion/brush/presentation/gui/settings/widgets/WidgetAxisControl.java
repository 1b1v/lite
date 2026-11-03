package com.almarel.orion.brush.presentation.gui.settings.widgets;

import com.almarel.orion.brush.data.capabilities.AxisScalable;
import com.almarel.orion.brush.data.capabilities.AxisSizeable;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.widgets.WidgetControls;
import org.bukkit.Material;

public class WidgetAxisControl {

    // --- CALLBACKS ---
    public interface ScaleCallback {
        void onScale(String axis, double delta);
    }

    public interface SizeCallback {
        void onSize(String axis, double delta);
    }

    // =========================================
    // 1. SCALE AXIS (Дробный масштаб, шаг 0.1)
    // =========================================
    public static void drawScaleAxisWidget(OrionMenuHolder holder, AxisScalable state, CommonGuiConfig config, ScaleCallback callback) {
        drawScale(holder, config, state.getScaleX(), state.getScaleY(), state.getScaleZ(), callback);
    }

    public static void drawScale(OrionMenuHolder holder, CommonGuiConfig config, double sX, double sY, double sZ, ScaleCallback callback) {
        drawScaleBtn(holder, config, 9, "X", sX, callback);
        drawScaleBtn(holder, config, 18, "Y", sY, callback);
        drawScaleBtn(holder, config, 27, "Z", sZ, callback);
    }

    private static void drawScaleBtn(OrionMenuHolder holder, CommonGuiConfig config, int slot, String axis, double currentVal, ScaleCallback callback) {
        WidgetControls.drawDoubleControl(holder, slot, Material.STRING, currentVal, 0.1, 100.0, 1.0, 0.1, 0.5, config,
                (valStr) -> config.getName("scale-axis.name", axis, valStr),
                (newVal) -> callback.onScale(axis, newVal - currentVal)
        );
    }

    // =========================================
    // 2. SIZE AXIS (Абсолютный размер, шаг 1.0)
    // =========================================
    public static void drawSizeAxisWidget(OrionMenuHolder holder, AxisSizeable state, CommonGuiConfig config, SizeCallback callback) {
        drawSizeBtn(holder, config, 9, "X", state.getSizeX(), callback);
        drawSizeBtn(holder, config, 18, "Y", state.getSizeY(), callback);
        drawSizeBtn(holder, config, 27, "Z", state.getSizeZ(), callback);
    }

    private static void drawSizeBtn(OrionMenuHolder holder, CommonGuiConfig config, int slot, String axis, double currentVal, SizeCallback callback) {
        WidgetControls.drawDoubleControl(holder, slot, Material.STRING, currentVal, 1.0, 100.0, 1.0, 1.0, 5.0, config,
                (valStr) -> "§bSize " + axis + " §8>> " + valStr,
                (newVal) -> callback.onSize(axis, newVal - currentVal)
        );
    }
}