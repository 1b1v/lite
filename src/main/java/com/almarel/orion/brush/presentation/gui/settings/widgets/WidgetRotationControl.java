package com.almarel.orion.brush.presentation.gui.settings.widgets;

import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class WidgetRotationControl {

    public interface RotationCallback {
        void onRotate(String action, int deltaAngle);
    }

    public static void draw(OrionMenuHolder holder, CommonGuiConfig config, RotationCallback callback) {
        String tUp = "§cUp";
        String tDown = "§cDown";
        String tLeanLeft = "§cLean Left";   // Крен влево
        String tLeanRight = "§cLean Right"; // Крен вправо
        String tTurnLeft = "§cTurn Left";   // Поворот вокруг оси Y
        String tTurnRight = "§cTurn Right"; // Поворот вокруг оси Y

        // КРЕСТ (Динамические оси X и Z относительно камеры)
        drawBtn(holder, config, 11, tUp, "PITCH", true, callback);
        drawBtn(holder, config, 29, tDown, "PITCH", false, callback);

        drawBtn(holder, config, 19, tLeanLeft, "ROLL", true, callback);
        drawBtn(holder, config, 21, tLeanRight, "ROLL", false, callback);

        // ДИАГОНАЛИ (Статичная ось Y - Вращение как юла)
        drawBtn(holder, config, 28, tTurnLeft, "YAW", false, callback);
        drawBtn(holder, config, 12, tTurnRight, "YAW", true, callback);
    }

    private static void drawBtn(OrionMenuHolder holder, CommonGuiConfig config, int slot,
                                String name, String action, boolean invertDir,
                                RotationCallback callback) {

        var builder = new ItemBuilder(Material.PHANTOM_MEMBRANE)
                .name(name)
                .lore("  " + config.getText("controls.lkm-pkm", "15°", "15°"),
                        "  " + config.getText("controls.shift", "45°", "45°"),
                        // Добавляем подсказку про сброс на колесико
                        "  " + config.getText("controls.skm", "Reset"));

        holder.addButton(slot, new GuiMenuButton(builder.build(), e -> {
            ClickType click = e.getClick();

            // [NEW] Обработка клика на колесико (Сброс матрицы)
            if (click == ClickType.MIDDLE || click.isCreativeAction()) {
                callback.onRotate("RESET", 0);
                return; // Прерываем выполнение
            }

            // Стандартное вращение на ЛКМ/ПКМ
            if (click.isLeftClick() || click.isRightClick()) {
                int delta = calculateDelta(e);
                if (invertDir) delta = -delta;
                callback.onRotate(action, delta);
            }
        }));
    }

    private static int calculateDelta(InventoryClickEvent event) {
        if (event.isRightClick()) return event.isShiftClick() ? 45 : 15;
        if (event.isLeftClick()) return event.isShiftClick() ? -45 : -15;
        return 0;
    }
}