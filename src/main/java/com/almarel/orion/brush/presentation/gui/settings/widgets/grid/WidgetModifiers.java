package com.almarel.orion.brush.presentation.gui.settings.widgets.grid;

import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.widgets.WidgetControls;
import org.bukkit.Material;

public class WidgetModifiers {

    public static void drawHollowWidget(OrionMenuHolder holder, int slot, Hollowable state, CommonGuiConfig config, WidgetControls.ToggleCallback callback) {
        WidgetControls.drawToggle(holder, slot, Material.BUCKET, config, state.isHollow(),
                (st) -> config.getName("pro.hollow", st), callback);
    }

    public static void drawGravityWidget(OrionMenuHolder holder, int slot, Gravityable state, CommonGuiConfig config, WidgetControls.ToggleCallback callback) {
        WidgetControls.drawToggle(holder, slot, Material.ANVIL, config, state.hasGravity(),
                (st) -> "§eGravity: " + st, callback);
    }

    public static void drawSeedWidget(OrionMenuHolder holder, int slot, Seedable state, CommonGuiConfig config, WidgetControls.ToggleCallback callback) {
        WidgetControls.drawToggle(holder, slot, Material.ENDER_EYE, config, state.isRandomSeed(),
                (st) -> config.getName("params.seed", st), callback);
    }

    public static void drawSmearWidget(OrionMenuHolder holder, int slot, Smearable state, CommonGuiConfig config, WidgetControls.IntCallback callback) {
        int depth = state.getSmearDepth();
        String status = (depth > 0) ? Palette.VAL_ON + depth + " down" : Palette.VAL_OFF + config.getText("values.off");
        var builder = new ItemBuilder(Material.FEATHER).name(config.getName("params.smear", status)).lore(
                "  " + config.getText("controls.lkm-pkm", "+1", "-1"),
                "  " + config.getText("controls.shift", "+5", "-5"));

        if (depth > 0) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            int delta = event.isShiftClick() ? (event.isRightClick() ? -5 : 5) : (event.isRightClick() ? -1 : 1);
            callback.onChange(Math.max(0, Math.min(depth + delta, 256)));
        }));
    }

    public static void drawSmoothWidget(OrionMenuHolder holder, int slot, Smoothable state, CommonGuiConfig config, WidgetControls.IntCallback callback) {
        int smoothVal = state.getSmoothIterations();
        String smoothStatus = (smoothVal > 0) ? Palette.VAL_ON + smoothVal + "x" : Palette.VAL_OFF + config.getText("values.off");
        var smoothBuilder = new ItemBuilder(Material.SNOWBALL)
                .name(config.getName("cp.smooth", smoothStatus))
                .lore("  " + config.getText("controls.lkm-pkm", "+1", "-1"),
                        "  " + config.getText("controls.shift", "+5", "-5"));
        if (smoothVal > 0) smoothBuilder.glow();

        holder.addButton(slot, new GuiMenuButton(smoothBuilder.build(), event -> {
            int delta = event.isShiftClick() ? (event.isRightClick() ? -5 : 5) : (event.isRightClick() ? -1 : 1);
            int newVal = Math.max(0, smoothVal + delta);
            callback.onChange(newVal);
        }));
    }
}