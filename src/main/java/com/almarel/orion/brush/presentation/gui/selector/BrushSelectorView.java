package com.almarel.orion.brush.presentation.gui.selector;

import com.almarel.orion.framework.compat.InventoryViewCompat;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.presentation.gui.selector.BrushSelectorPresenter.BrushViewItem;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.gui.NavBarRenderer;
import com.almarel.orion.framework.gui.common.GuiItemService;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.BrushListGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.events.OrionGuiNavigationEvent;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import com.almarel.orion.framework.gui.widgets.WidgetContextMenu;
import com.almarel.orion.framework.gui.widgets.WidgetFilter;
import com.almarel.orion.framework.gui.widgets.WidgetPageNavigation;
import com.almarel.orion.player.inventory.PlayerHotbarSlotAllocator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Только рендер + тонкое делегирование кликов в BrushSelectorPresenter.
 * Не содержит permission-гейтов, regex-валидации и ChatPrompt-оркестрации —
 * вся эта логика в BrushPresetBindingService, вызываемом через Presenter.
 */
@Singleton
public class BrushSelectorView implements Listener {

    private static final int PAGE_SIZE = 36;

    private final JavaPlugin plugin;
    private final GuiStateManager sessionManager;
    private final BrushSelectorSessionStore sessionStore;
    private final BrushListGuiConfig config;
    private final CommonGuiConfig commonGuiConfig;
    private final BrushSelectorPresenter presenter;
    private final NavBarRenderer navigationService;
    private final PlayerHotbarSlotAllocator hotbarSlotAllocator;

    @Inject
    public BrushSelectorView(JavaPlugin plugin,
                             GuiStateManager sessionManager,
                             BrushSelectorSessionStore sessionStore,
                             NavBarRenderer navigationService,
                             BrushListGuiConfig config,
                             CommonGuiConfig commonGuiConfig,
                             BrushSelectorPresenter presenter,
                             PlayerHotbarSlotAllocator hotbarSlotAllocator) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.sessionStore = sessionStore;
        this.navigationService = navigationService;
        this.config = config;
        this.commonGuiConfig = commonGuiConfig;
        this.presenter = presenter;
        this.hotbarSlotAllocator = hotbarSlotAllocator;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNavigationRequest(OrionGuiNavigationEvent event) {
        if (event.getTargetMenu() == MenuType.LIST) {
            openMenu(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        GuiSession session = sessionManager.getSession(player);
        if (session.isSwapping()) {
            presenter.cancelSwap(player, session);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GuiSession session = sessionManager.getSession(player);
        if (!session.isSwapping()) {
            return;
        }

        handleSwappingClick(player, session, event);
    }

    private void handleSwappingClick(Player player, GuiSession session, InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot >= 54) {
            event.setCancelled(true);
            equipSwappedBrush(player, session, event.getSlot());
            return;
        }

        if (rawSlot == -999 || isClickOnEmptyMenuSlot(event, rawSlot)) {
            event.setCancelled(true);
            presenter.cancelSwap(player, session);
        }
    }

    private boolean isClickOnEmptyMenuSlot(InventoryClickEvent event, int rawSlot) {
        return rawSlot >= 0 && rawSlot < 54
                && event.getInventory().getHolder() instanceof OrionMenuHolder holder
                && holder.getButton(event.getSlot()) == null;
    }

    private void equipSwappedBrush(Player player, GuiSession session, int targetSlot) {
        String sourceKey = session.getSwapSourceKey();
        if (sourceKey == null) {
            presenter.cancelSwap(player, session);
            return;
        }

        BrushSelectorSession selectorSession = sessionStore.getOrCreate(player);
        List<BrushViewItem> allSorted = presenter.getSortedBrushes(player, selectorSession);
        BrushViewItem matchedItem = allSorted.stream()
                .filter(item -> item.getUniqueKey().equals(sourceKey))
                .findFirst()
                .orElse(null);

        presenter.cancelSwap(player, session);

        if (matchedItem != null) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    hotbarSlotAllocator.placeInSlot(player, matchedItem.state().getTool(), targetSlot));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GuiSession session = sessionManager.getSession(player);
        if (!session.isSwapping()) {
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 54) {
                event.setCancelled(true);
                presenter.cancelSwap(player, session);
                return;
            }

            if (rawSlot >= 9 && rawSlot < 45 && handleGridDragSlot(player, session, rawSlot, event)) {
                return;
            }
        }
    }

    private boolean handleGridDragSlot(Player player, GuiSession session, int rawSlot, InventoryDragEvent event) {
        BrushSelectorSession selectorSession = sessionStore.getOrCreate(player);
        List<BrushViewItem> allSorted = presenter.getSortedBrushes(player, selectorSession);
        int page = selectorSession.getBrushListPage();
        int targetIndex = (page * PAGE_SIZE) + (rawSlot - 9);

        if (targetIndex < 0 || targetIndex >= allSorted.size()) {
            return false;
        }

        BrushViewItem targetItem = allSorted.get(targetIndex);
        event.setCancelled(true);

        String sourceKey = session.getSwapSourceKey();
        if (sourceKey != null && sourceKey.equals(targetItem.getUniqueKey())) {
            return true;
        }

        presenter.cancelSwap(player, session);
        boolean swapped = presenter.handleDragSwap(player, selectorSession, targetItem.getUniqueKey(), sourceKey);

        if (swapped) {
            Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
        }
        return true;
    }

    public void openMenu(Player player) {
        GuiSession session = sessionManager.getSession(player);
        BrushSelectorSession selectorSession = sessionStore.getOrCreate(player);
        int page = selectorSession.getBrushListPage();
        String title = resolveMenuTitle(selectorSession, page);

        OrionMenuHolder holder = new OrionMenuHolder();
        Inventory menu = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(menu);

        drawBorders(menu, player);

        List<BrushViewItem> allSortedBrushes = presenter.getSortedBrushes(player, selectorSession);
        drawBrushList(holder, player, session, selectorSession, allSortedBrushes, page);
        drawContextMenuIfSelected(holder, player, selectorSession, allSortedBrushes);
        drawPaginationControls(holder, player, selectorSession, allSortedBrushes, page);

        player.openInventory(menu);
    }

    private void drawBorders(Inventory menu, Player player) {
        ItemStack glass = new GuiItemService.ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, glass);
        }
        navigationService.draw(menu, player, MenuType.LIST);
    }

    private String resolveMenuTitle(BrushSelectorSession selectorSession, int page) {

        return config.getTitle() + (page > 0 ? " (" + (page + 1) + ")" : "");
    }


    private void drawBrushList(OrionMenuHolder holder, Player player, GuiSession session, BrushSelectorSession selectorSession,
                               List<BrushViewItem> allSortedBrushes, int page) {
        List<BrushViewItem> pageBrushes = getPageItems(allSortedBrushes, page);
        int slot = 9;

        for (BrushViewItem viewItem : pageBrushes) {
            if (slot >= 45) {
                break;
            }

            ItemStack icon = presenter.getRenderer().render(viewItem.state(), player, session, selectorSession).build();
            holder.addButton(slot, new GuiMenuButton(icon, event ->
                    handleBrushClick(player, session, selectorSession, viewItem, event, icon)
            ));
            slot++;
        }
    }

    private List<BrushViewItem> getPageItems(List<BrushViewItem> items, int page) {
        int start = page * PAGE_SIZE;
        if (start >= items.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + PAGE_SIZE, items.size());
        return items.subList(start, end);
    }

    private boolean isGlobalAdmin(Player player) {
        boolean result = false;
        return result;
    }

    /**
     * Единственная точка входа для клика по любой кнопке кисти/пресета.
     * Вся бизнес-логика — в presenter.handleClick(); View только решает,
     * нужно ли (и как) перерисовать меню.
     */
    private void handleBrushClick(Player player, GuiSession session, BrushSelectorSession selectorSession,
                                  BrushViewItem viewItem, InventoryClickEvent event, ItemStack icon) {
        boolean needRefresh = presenter.handleClick(
                player, session, selectorSession, viewItem, event, icon,
                () -> isMenuStillOpen(player) && !session.isSwapping(),
                () -> openMenu(player)
        );

        if (needRefresh) {
            Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
        }
    }

    private boolean isMenuStillOpen(Player player) {
        Inventory top = InventoryViewCompat.getTopInventory(player);
        return top != null && top.getHolder() instanceof OrionMenuHolder;
    }

    private void drawContextMenuIfSelected(OrionMenuHolder holder, Player player, BrushSelectorSession selectorSession,
                                           List<BrushViewItem> allSortedBrushes) {
        if (selectorSession.getSelectedBrushKey() == null) {
            return;
        }

        BrushViewItem targetItem = findSelectedState(selectorSession, allSortedBrushes);
        if (targetItem == null) {
            return;
        }

        final boolean isAdmin = isGlobalAdmin(player);
        Runnable onPublishFork = null;

        WidgetContextMenu.draw(holder, targetItem.isPreset(), isAdmin, onPublishFork, null);
        drawFormulaToggle(holder, player, selectorSession, targetItem.state());
    }

    private BrushViewItem findSelectedState(BrushSelectorSession selectorSession, List<BrushViewItem> allSortedBrushes) {
        for (BrushViewItem item : allSortedBrushes) {
            if (item.state().getStorageKey().equals(selectorSession.getSelectedBrushKey())) {
                return item;
            }
        }
        return null;
    }

    private void drawFormulaToggle(OrionMenuHolder holder, Player player, BrushSelectorSession selectorSession, BrushState targetState) {
    }

    private void drawPaginationControls(OrionMenuHolder holder, Player player, BrushSelectorSession selectorSession,
                                        List<BrushViewItem> brushes, int page) {
        boolean hasPrev = page > 0;
        boolean hasNext = brushes.size() > (page + 1) * PAGE_SIZE;

        WidgetPageNavigation.draw(holder, commonGuiConfig, page, hasPrev, hasNext, newPage -> {
            selectorSession.setBrushListPage(newPage);
            selectorSession.clearSelectedBrushKey();
            openMenu(player);
        });
    }
}