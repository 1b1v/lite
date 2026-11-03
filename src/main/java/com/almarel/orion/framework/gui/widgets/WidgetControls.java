package com.almarel.orion.framework.gui.widgets;

import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Глобальная фабрика интерактивных элементов управления (Inputs).
 * Предоставляет готовые виджеты для изменения чисел, строк и переключателей.
 * Защищена от фантомных кликов (DROP, NUMBER_KEY, DOUBLE_CLICK).
 */
public class WidgetControls {

    private static final DecimalFormat DF = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.ROOT));

    // --- Interfaces ---
    public interface DoubleCallback { void onChange(double newValue); }
    public interface IntCallback { void onChange(int newValue); }
    public interface ActionCallback { void onAction(InventoryClickEvent event); }
    public interface ToggleCallback { void onToggle(); }
    public interface NameGenerator { String generate(String valueString); }

    // --- Control Builders ---

    public static void drawDoubleControl(OrionMenuHolder holder, int slot, Material icon, double value, double min, double max, double def, CommonGuiConfig config, NameGenerator nameGen, DoubleCallback callback) {
        drawDoubleControl(holder, slot, icon, value, min, max, def, 1.0, 5.0, false, config, nameGen, callback);
    }

    public static void drawDoubleControl(OrionMenuHolder holder, int slot, Material icon, double value, double min, double max, double def, double stepSmall, double stepLarge, CommonGuiConfig config, NameGenerator nameGen, DoubleCallback callback) {
        drawDoubleControl(holder, slot, icon, value, min, max, def, stepSmall, stepLarge, false, config, nameGen, callback);
    }

    public static void drawIntControl(OrionMenuHolder holder, int slot, Material icon, int value, int min, int max, int def, int stepSmall, int stepLarge, CommonGuiConfig config, NameGenerator nameGen, IntCallback callback) {
        String finalName = nameGen.generate(Palette.VAL_ON + value);
        var builder = new ItemBuilder(icon).name(finalName).lore(
                "  " + config.getText("controls.lkm-pkm", "+" + stepSmall, "-" + stepSmall),
                "  " + config.getText("controls.shift", "+" + stepLarge, "-" + stepLarge),
                "  " + config.getText("controls.skm", config.getText("actions.reset")));

        if (value != def) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            ClickType click = event.getClick();
            if (click == ClickType.MIDDLE) {
                callback.onChange(def);
            } else if (click.isLeftClick() || click.isRightClick()) {
                int delta = event.isShiftClick() ? (event.isRightClick() ? -stepLarge : stepLarge) : (event.isRightClick() ? -stepSmall : stepSmall);
                callback.onChange(Math.max(min, Math.min(value + delta, max)));
            }
        }));
    }

    public static void drawTextControl(OrionMenuHolder holder, int slot, Material icon, String value, String defaultVal, CommonGuiConfig config, NameGenerator nameGen, ActionCallback callback) {
        boolean isSet = value != null && !value.isEmpty() && !value.equals(defaultVal);
        String fallback = defaultVal == null ? config.getText("values.none") : defaultVal;
        String finalName = nameGen.generate(isSet ? Palette.VAL_ON + value : Palette.VAL_OFF + fallback);

        var builder = new ItemBuilder(icon).name(finalName).lore(
                "  " + config.getText("controls.set-item"),
                "  " + config.getText("controls.edit-skm"),
                "  " + config.getText("controls.reset-pkm"));

        if (isSet) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            ClickType click = event.getClick();
            if (click.isLeftClick() || click.isRightClick() || click == ClickType.MIDDLE) {
                callback.onAction(event);
            }
        }));
    }

    public static void drawToggle(OrionMenuHolder holder, int slot, Material icon, CommonGuiConfig config, boolean state, NameGenerator nameGen, ToggleCallback callback) {
        String status = state ? Palette.VAL_ON + config.getText("values.on") : Palette.VAL_OFF + config.getText("values.off");
        var builder = new ItemBuilder(icon).name(nameGen.generate(status)).lore(
                "  " + config.getText("controls.toggle-lkm"));

        if (state) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            ClickType click = event.getClick();
            if (click.isLeftClick() || click.isRightClick() || click == ClickType.MIDDLE) {
                callback.onToggle();
            }
        }));
    }

    private static void drawDoubleControl(OrionMenuHolder holder, int slot, Material icon, double value, double min, double max, double def, double stepSmall, double stepLarge, boolean showLimit, CommonGuiConfig config, NameGenerator nameGen, DoubleCallback callback) {
        String valStr = Palette.VAL_ON + String.format(Locale.ROOT, (stepSmall < 1.0 ? "%.1f" : "%.0f"), value);
        var builder = new ItemBuilder(icon).name(nameGen.generate(valStr)).lore(
                "  " + config.getText("controls.lkm-pkm", "+" + DF.format(stepSmall), "-" + DF.format(stepSmall)),
                "  " + config.getText("controls.shift", "+" + DF.format(stepLarge), "-" + DF.format(stepLarge)),
                "  " + config.getText("controls.skm", config.getText("actions.reset")));

        if (showLimit && max < 10000) builder.lore(" ", "  " + config.getText("pro.radius-limit", String.valueOf(max)));
        if (Math.abs(value - def) > 0.001) builder.glow();

        holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
            ClickType click = event.getClick();
            if (click == ClickType.MIDDLE) {
                callback.onChange(def);
            } else if (click.isLeftClick() || click.isRightClick()) {
                double delta = event.isShiftClick() ? (event.isRightClick() ? -stepLarge : stepLarge) : (event.isRightClick() ? -stepSmall : stepSmall);
                callback.onChange(Math.max(min, Math.min(value + delta, max)));
            }
        }));
    }
}