package com.almarel.orion.brush.presentation.gui.settings.widgets.grid;

import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.widgets.WidgetControls;
import org.bukkit.Material;

public class WidgetMaterial {

    public static void drawPatternWidget(OrionMenuHolder holder, int slot, Patternable state, CommonGuiConfig config, WidgetControls.ActionCallback callback) {
        WidgetControls.drawTextControl(holder, slot, Material.GRASS_BLOCK, state.getPattern(), "stone", config,
                (val) -> config.getName("params.pattern", val), callback);
    }

    public static void drawMaskWidget(OrionMenuHolder holder, int slot, Maskable state, CommonGuiConfig config, WidgetControls.ActionCallback callback) {
        WidgetControls.drawTextControl(holder, slot, Material.PAPER, state.getMask(), null, config,
                (val) -> config.getName("params.mask", val == null || val.isEmpty() ? config.getText("values.none") : val), callback);
    }

    public static void drawSourceMaskWidget(OrionMenuHolder holder, int slot, SourceMaskable state, CommonGuiConfig config, WidgetControls.ActionCallback callback) {
        WidgetControls.drawTextControl(holder, slot, Material.MAP, state.getSourceMask(), null, config,
                (val) -> config.getName("params.source-mask", val == null || val.isEmpty() ? config.getText("values.none") : val), callback);
    }
}