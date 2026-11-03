package com.almarel.orion.brush.presentation.gui.settings.widgets.grid;

import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.BrushRotationMode;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.widgets.WidgetControls;
import org.bukkit.Material;

public class WidgetTransform {

    public static void drawGlobalScaleWidget(OrionMenuHolder holder, int slot, Scalable state, CommonGuiConfig config, WidgetControls.DoubleCallback onChange) {
        WidgetControls.drawDoubleControl(holder, slot, Material.SLIME_BALL, state.getScale(), 0.1, 10.0, 1.0, 0.1, 0.5, config,
                (valStr) -> config.getName("params.scale", valStr), onChange);
    }

    public static void drawSizeWidget(OrionMenuHolder holder, int slot, Sizeable state, double maxSize, CommonGuiConfig config, WidgetControls.DoubleCallback onChange) {
        WidgetControls.drawDoubleControl(holder, slot, Material.MAGMA_CREAM, state.getSize(), 1.0, maxSize, 5.0, config,
                (valStr) -> config.getName("params.size", valStr), onChange);
    }

    public static void drawRangeWidget(OrionMenuHolder holder, int slot, Rangeable state, CommonGuiConfig config, WidgetControls.IntCallback onChange) {
        WidgetControls.drawIntControl(holder, slot, Material.BOW, state.getRange(), -1, 10000, -1, 5, 15, config,
                (valStr) -> config.getName("params.range", valStr.contains("-1") ? "MAX" : valStr), onChange);
    }

    public static void drawAngleToggleWidget(OrionMenuHolder holder, int slot, AngleModeable state, CommonGuiConfig config, WidgetControls.ToggleCallback callback) {
        WidgetControls.drawToggle(holder, slot, Material.MOJANG_BANNER_PATTERN, config, state.isAngleMode(),
                (st) -> config.getName("pro.angle", st), callback);
    }

    public static void drawRotationModeWidget(OrionMenuHolder holder, int slot, RotationModeable state, CommonGuiConfig config, WidgetControls.ActionCallback callback) {
        BrushRotationMode currentMode = state.getRotationMode();
        boolean isActive = currentMode != null && currentMode != BrushRotationMode.OFF;

        String modeName = currentMode != null ? currentMode.name() : "OFF";
        String modeStatus = isActive ? Palette.VAL_ON + modeName : Palette.VAL_OFF + config.getText("values.off");
        String title = config.getName("params.rotation", modeStatus);

        var rotBuilder = new ItemBuilder(Material.MOJANG_BANNER_PATTERN)
                .name(title)
                .lore("  " + config.getText("controls.lkm-pkm", config.getText("actions.next-mode"), config.getText("actions.prev-mode")));

        if (isActive) rotBuilder.glow();

        holder.addButton(slot, new GuiMenuButton(rotBuilder.build(), callback::onAction));
    }
}