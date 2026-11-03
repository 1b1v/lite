package com.almarel.orion.brush.presentation.gui.settings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.NavBarRenderer;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.widgets.WidgetPageNavigation;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

@Singleton
public class GuiMaterialListMenu {

    private final NavBarRenderer navigationService;
    private final CommonGuiConfig commonGuiConfig;
    private final MessageManager messageManager;

    private static final int PAGE_SIZE = 27;
    private static final ItemStack GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

    @Inject
    public GuiMaterialListMenu(NavBarRenderer navigationService, CommonGuiConfig commonGuiConfig, MessageManager messageManager) {
        this.navigationService = navigationService;
        this.commonGuiConfig = commonGuiConfig;
        this.messageManager = messageManager;
    }

    public void open(Player player, String title, Set<Material> materials, int limit, Map<Integer, GuiMenuButton> extraButtons, Consumer<Set<Material>> onSave, Runnable onBack) {
        List<String> stringList = new ArrayList<>();
        if (materials != null) {
            for (Material m : materials) {
                if (m != null && m != Material.AIR) {
                    stringList.add(m.name().toLowerCase());
                }
            }
        }
        open(player, title, stringList, limit, 0, extraButtons, (csvString) -> {
            Set<Material> newSet = new HashSet<>();
            if (csvString != null && !csvString.isEmpty()) {
                String[] parts = csvString.split(",");
                for (String part : parts) {
                    Material m = Material.matchMaterial(part);
                    if (m != null) newSet.add(m);
                }
            }
            if (newSet.isEmpty()) newSet.add(Material.AIR);
            onSave.accept(newSet);
        }, onBack);
    }

    public void open(Player player, String title, String currentList, int limit, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        List<String> elements = new ArrayList<>();
        if (currentList != null && !currentList.isEmpty()) {
            elements.addAll(Arrays.asList(currentList.split(",")));
        }
        open(player, title, elements, limit, 0, extraButtons, onSave, onBack);
    }

    private void open(Player player, String title, List<String> elements, int limit, int page, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        // [FIX] Убрали счетчик (0/limit) из заголовка
        String fullTitle = title;

        // Оставляем защиту от краша Bukkit (ограничение инвентаря в 32 символа)
        if (fullTitle.length() > 32) fullTitle = fullTitle.substring(0, 32);

        OrionMenuHolder holder = new OrionMenuHolder();
        Inventory menu = Bukkit.createInventory(holder, 54, fullTitle);
        holder.setInventory(menu);

        draw(holder, player, title, elements, limit, page, extraButtons, onSave, onBack);
        player.openInventory(menu);
    }

    private void draw(OrionMenuHolder holder, Player player, String title, List<String> elements, int limit, int rawPage, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        Inventory menu = holder.getInventory();

        drawBorders(menu);
        navigationService.draw(menu, player, MenuType.SETTINGS);

        int totalItems = elements.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        final int page = normalizePage(rawPage, totalPages);

        drawList(holder, elements, page, limit, title, extraButtons, onSave, onBack);
        drawActionButtons(holder, player, elements, limit, page, title, extraButtons, onSave, onBack);

        if (extraButtons != null) {
            extraButtons.forEach(holder::addButton);
        }

        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;
        WidgetPageNavigation.draw(holder, commonGuiConfig, page, hasPrev, hasNext, newPage -> {
            open(player, title, elements, limit, newPage, extraButtons, onSave, onBack);
        });
    }

    private void drawList(OrionMenuHolder holder, List<String> elements, int page, int limit, String title, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, elements.size());
        int baseSlot = 9;

        for (int i = startIndex; i < endIndex; i++) {
            int slot = baseSlot + (i - startIndex);
            String matName = elements.get(i);
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.BEDROCK;

            ItemBuilder builder = new ItemBuilder(mat)
                    .name(Palette.LORE_VAL + matName)
                    .lore(Palette.LORE_TEXT + commonGuiConfig.getText("controls.delete-pkm"));

            final int currentIndex = i;
            holder.addButton(slot, new GuiMenuButton(builder.build(), event -> {
                if (event.isRightClick()) {
                    elements.remove(currentIndex);
                    save(elements, onSave);
                    open((Player)event.getWhoClicked(), title, elements, limit, page, extraButtons, onSave, onBack);
                }
            }));
        }
    }

    private void save(List<String> elements, Consumer<String> onSave) {
        String result = String.join(",", elements);
        if (result.isEmpty()) result = null;
        onSave.accept(result);
    }

    private int normalizePage(int rawPage, int totalPages) {
        if (totalPages > 0 && rawPage >= totalPages) return totalPages - 1;
        if (rawPage < 0) return 0;
        return rawPage;
    }

    private void drawBorders(Inventory menu) {
        for (int i = 0; i < 9; i++) menu.setItem(i, GLASS_PANE);
        for (int i = 45; i < 54; i++) menu.setItem(i, GLASS_PANE);
    }

    private void drawActionButtons(OrionMenuHolder holder, Player player, List<String> elements, int limit, int page, String title, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        holder.addButton(42, new GuiMenuButton(new ItemBuilder(Material.HOPPER)
                .name(Palette.BTN_ACT + commonGuiConfig.getText("actions.add-block"))
                .lore(Palette.LORE_TEXT + commonGuiConfig.getText("controls.add-block-hint")).build(), event -> {
            handleAddItem(player, event.getCursor(), elements, limit, page, title, extraButtons, onSave, onBack);
        }));

        holder.addButton(43, new GuiMenuButton(new ItemBuilder(Material.LAVA_BUCKET)
                .name(Palette.BTN_CLOSE + commonGuiConfig.getText("actions.clear-list"))
                .lore(Palette.LORE_TEXT + commonGuiConfig.getText("controls.shift-lkm-confirm")).build(), event -> {
            if (event.isShiftClick()) handleClearList(player, elements, title, limit, extraButtons, onSave, onBack);
        }));

        holder.addButton(44, new GuiMenuButton(new ItemBuilder(Material.BARRIER)
                .name(Palette.BTN_CLOSE + commonGuiConfig.getText("actions.done-back")).build(), event -> onBack.run()));
    }

    private void handleAddItem(Player player, ItemStack cursor, List<String> elements, int limit, int page, String title, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        if (cursor != null && cursor.getType() != Material.AIR && cursor.getType().isBlock()) {
            String matName = cursor.getType().name().toLowerCase();
            if (!elements.contains(matName)) {
                if (elements.size() >= limit) {
                    messageManager.send(player, OrionMessage.GUI_LIMIT_REACHED);
                    return;
                }
                elements.add(matName);
                save(elements, onSave);
                open(player, title, elements, limit, page, extraButtons, onSave, onBack);
            } else {
                messageManager.send(player, OrionMessage.GUI_MAT_EXISTS);
            }
        } else {
            messageManager.send(player, OrionMessage.GUI_MAT_PROMPT);
        }
    }

    private void handleClearList(Player player, List<String> elements, String title, int limit, Map<Integer, GuiMenuButton> extraButtons, Consumer<String> onSave, Runnable onBack) {
        elements.clear();
        save(elements, onSave);
        open(player, title, elements, limit, 0, extraButtons, onSave, onBack);
    }
}