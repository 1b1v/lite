package com.almarel.orion.brush.presentation.gui.settings.widgets;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.SourceMaskable;
import com.almarel.orion.brush.fawe.data.ArceonConstructBrushState;
import com.almarel.orion.brush.fawe.data.FaweConstructBrushState;
import com.almarel.orion.framework.gui.data.GuiLayout;
import com.almarel.orion.brush.presentation.gui.selector.BrushDescriptionService;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WidgetInfoIcon {

    private static final String PADDING = "  ";

    // --- Builders (Legacy Runnable) ---

    public static InfoBuilder builder(OrionMenuHolder holder, ItemStack iconItem, String name, Runnable action) {
        return new InfoBuilder(holder, iconItem, name, (e) -> action.run());
    }

    public static InfoBuilder builder(OrionMenuHolder holder, Material material, String name, Runnable action) {
        return new InfoBuilder(holder, new ItemStack(material), name, (e) -> action.run());
    }

    // --- Builders (Event Consumer) ---

    public static InfoBuilder builder(OrionMenuHolder holder, ItemStack iconItem, String name, Consumer<InventoryClickEvent> action) {
        return new InfoBuilder(holder, iconItem, name, action);
    }

    public static InfoBuilder builder(OrionMenuHolder holder, Material material, String name, Consumer<InventoryClickEvent> action) {
        return new InfoBuilder(holder, new ItemStack(material), name, action);
    }

    /**
     * Отрисовка иконки информации о кисти.
     * Использует BrushDescriptionService для генерации унифицированного списка параметров.
     */
    public static void drawBrushDetails(OrionMenuHolder holder,
                                        Player player,
                                        BrushState state,
                                        BrushDescriptionService descriptionService,
                                        CommonGuiConfig config,
                                        boolean isExpanded,
                                        Consumer<InventoryClickEvent> action) {

        Material iconMat = determineIconMaterial(state);
        String title = determineBrushTitle(state);
        InfoBuilder builder = builder(holder, iconMat, title, action);

        List<String> params = descriptionService.getParameters(state, player, isExpanded);
        for (String line : params) {
            builder.addRawLine(line);
        }

        appendSourceMask(builder, state);
        appendFooterNote(builder, state, config);

        builder.draw();
    }

    // =========================================
    // PRIVATE HELPERS
    // =========================================

    private static Material determineIconMaterial(BrushState state) {
        return (state.getTool() != null && state.getTool() != Material.AIR)
                ? state.getTool()
                : Material.BRUSH;
    }

    private static String determineBrushTitle(BrushState state) {
        String typeId = state.getBrushType().name().toLowerCase(java.util.Locale.ROOT);

        // [ORION_CUT: FaweConstructBrushState]
        if (state instanceof FaweConstructBrushState fawe && isValidName(fawe.getFaweBrushName())) {
            typeId = fawe.getFaweBrushName();
        } else if (state instanceof ArceonConstructBrushState arceon && isValidName(arceon.getArceonBrushName())) {
            typeId = arceon.getArceonBrushName();
        }
        // [ORION_CUT_END]

        return Palette.VAL_ON + capitalize(typeId) + " Brush";
    }

    private static boolean isValidName(String name) {
        return name != null && !name.equals("unknown");
    }

    private static void appendSourceMask(InfoBuilder builder, BrushState state) {
        if (state instanceof SourceMaskable sourceMaskable) {
            String srcMask = sourceMaskable.getSourceMask();
            if (srcMask != null) {
                builder.addProperty("Source Mask", srcMask);
            }
        }
    }

    private static void appendFooterNote(InfoBuilder builder, BrushState state, CommonGuiConfig config) {
            builder.addActionNote(config.getText("info.refresh"));
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static class InfoBuilder {
        private final OrionMenuHolder holder;
        private final ItemStack iconItem;
        private final String name;
        private final Consumer<InventoryClickEvent> action;
        private final List<String> lore = new ArrayList<>();

        public InfoBuilder(OrionMenuHolder holder, ItemStack iconItem, String name, Consumer<InventoryClickEvent> action) {
            this.holder = holder;
            this.iconItem = iconItem;
            this.name = name;
            this.action = action;
        }

        public InfoBuilder addRawLine(String text) {
            if (text != null && !text.isEmpty()) this.lore.add(text);
            return this;
        }

        public InfoBuilder addProperty(String key, Object value) {
            this.lore.add(PADDING + Palette.LORE_TEXT + key + ": " + Palette.LORE_VAL + value);
            return this;
        }

        public InfoBuilder addSeparator() { // почему-то no usage
            this.lore.add("");
            return this;
        }

        public InfoBuilder addActionNote(String note) {
            if (note != null && !note.isEmpty()) this.lore.add(PADDING + note);
            return this;
        }

        public void draw() {
            ItemBuilder builder = new ItemBuilder(iconItem)
                    .name(name) // Убрали §a, так как цвет уже задан при генерации title
                    .lore(lore)
                    .glow();

            holder.addButton(GuiLayout.SLOT_INFO_MAIN, new GuiMenuButton(builder.build(), event -> {
                if (action != null) action.accept(event);
            }));
        }
    }
}