package com.almarel.orion.recolor.presentation.gui.widgets;

import com.almarel.orion.framework.gui.common.GuiItemService;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.mapping.visual.RgbColor;
import com.almarel.orion.recolor.palette.QuickPalettePreviewOption;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public final class QuickPaletteWidget {

    // Центрированы в ряду из 9 слотов (0..8), по 2 filler-клетки HEADER_FILLER с каждого края.
    // Расширено с 3 до 5 — после фикса выбора анкора (Проблема 3) вариантов стало больше
    // осмысленных/различимых, 3 иконки были слишком тесным лимитом.
    private static final int[] SLOTS = {2, 3, 4, 5, 6};

    private QuickPaletteWidget() {
    }

    public static void draw(OrionMenuHolder holder,
                            List<QuickPalettePreviewOption> options,
                            Consumer<QuickPalettePreviewOption> onClick) {
        for (int i = 0; i < SLOTS.length && i < options.size(); i++) {
            int slot = SLOTS[i];
            QuickPalettePreviewOption option = options.get(i);
            RgbColor previewColor = option.previewColor();

            ItemStack icon = new GuiItemService.ItemBuilder(Material.LEATHER_HORSE_ARMOR)
                    .name(option.preset().displayName())
                    .leatherColor(previewColor.r(), previewColor.g(), previewColor.b())
                    .build();

            holder.addButton(slot, new GuiMenuButton(icon, event -> onClick.accept(option)));
        }
    }
}