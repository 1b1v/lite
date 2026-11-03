package com.almarel.orion.framework.gui;

import com.almarel.orion.brush.presentation.gui.selector.BrushSelectorSessionStore;
import com.almarel.orion.framework.concurrent.TaskRunner;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.events.OrionGuiNavigationEvent;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import com.almarel.orion.framework.message.MessageManager;
// [ORION_CUT: RecolorModule]
import com.almarel.orion.recolor.core.RecolorSelection;
import com.almarel.orion.recolor.core.RecolorSelectionResolver;
import com.almarel.orion.recolor.presentation.gui.RecolorMenuView;
// [ORION_CUT_END]
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class GuiWindowManager implements Listener {

    private final JavaPlugin plugin;
    private final BrushStateManager brushStateManager;
    private final GuiStateManager sessionManager;
    private final MessageManager messageManager;
    private final NamespacedKey actionKey;
    private final TaskRunner taskRunner;
    // [ORION_CUT: RecolorModule]
    private final Provider<RecolorMenuView> recolorMenuViewProvider;
    private final RecolorSelectionResolver recolorSelectionResolver;
    // [ORION_CUT_END]
    private final BrushSelectorSessionStore brushSelectorSessionStore;


    @Inject
    public GuiWindowManager(JavaPlugin plugin,
                            BrushStateManager brushStateManager,
                            MessageManager messageManager,
                            // [ORION_CUT: RecolorModule]
                            Provider<RecolorMenuView> recolorMenuViewProvider,
                            RecolorSelectionResolver recolorSelectionResolver,
                            // [ORION_CUT_END]
                            GuiStateManager sessionManager,
                            TaskRunner taskRunner,
                            BrushSelectorSessionStore brushSelectorSessionStore) {

        this.plugin = plugin;
        this.brushStateManager = brushStateManager;
        this.messageManager = messageManager;
        this.actionKey = new NamespacedKey(plugin, "brush_action");
        // [ORION_CUT: RecolorModule]
        this.recolorMenuViewProvider = recolorMenuViewProvider;
        this.recolorSelectionResolver = recolorSelectionResolver;
        // [ORION_CUT_END]
        this.sessionManager = sessionManager;
        this.taskRunner = taskRunner;
        this.brushSelectorSessionStore = brushSelectorSessionStore;
    }

    public JavaPlugin getPlugin() { return plugin; }

    public void openBrushSettings(Player player) {
        GuiSession session = sessionManager.getSession(player);
        session.setLastMenuType(MenuType.SETTINGS);
        session.setLastToolsMenuType(MenuType.SETTINGS);
        fireNavigationEvent(player, MenuType.SETTINGS, null);
    }

    public void openBrushSelector(Player player) {
        GuiSession session = sessionManager.getSession(player);
        brushSelectorSessionStore.getOrCreate(player).clearSelectorSnapshot();
        session.setLastMenuType(MenuType.LIST);
        session.setLastToolsMenuType(MenuType.LIST);
        fireNavigationEvent(player, MenuType.LIST, null);
    }

    public void openCommandsMenu(Player player) {
        openCommandsMenuInternal(player, true);
    }

    private void openCommandsMenuInternal(Player player, boolean notifyOnDenial) {
        sessionManager.getSession(player).setLastMenuType(MenuType.COMMANDS);
        fireNavigationEvent(player, MenuType.COMMANDS, null);
    }

    public void openClipboardMenu(Player player) {
        sessionManager.getSession(player).setLastMenuType(MenuType.CLIPBOARD);
        handleClipboardOpen(player);
    }

    public void openMainMenu(Player player) {
        openClipboardMenu(player);
    }

    // [ORION_CUT: RecolorModule]
    public void openRecolorMenu(Player player) {
        openRecolorMenuInternal(player, true);
    }

    private void openRecolorMenuInternal(Player player, boolean notifyOnDenial) {
        if (!player.hasPermission(OrionPermissions.RECOLOR_USE)) {
            if (notifyOnDenial) {
                messageManager.sendMessage(player, "command.no-permission");
            }
            openBrushSelector(player);
            return;
        }

        Optional<RecolorSelection> selectionOpt = recolorSelectionResolver.resolve(player);
        if (selectionOpt.isEmpty()) {
            if (notifyOnDenial) {
                player.sendMessage("§cMake a region selection first (//pos1 and //pos2, or a wand).");
            }
            recolorMenuViewProvider.get().openSelectionRequired(player);
            return;
        }

        RecolorSelection selection = selectionOpt.get();
        sessionManager.getSession(player).setLastMenuType(MenuType.RECOLOR);
        recolorMenuViewProvider.get().open(player, selection.world(), selection.region());
    }
    // [ORION_CUT_END]

    public void switchMenu(Player player, Consumer<Player> opener, ItemStack cursorItem) {
        boolean hasCursor = cursorItem != null && cursorItem.getType() != Material.AIR;
        if (hasCursor) {
            player.setItemOnCursor(null);
        }

        opener.accept(player);

        if (hasCursor) {
            final ItemStack c = cursorItem.clone();
            taskRunner.runSync(() -> player.setItemOnCursor(c));
        }
    }

    public void handleClipboardOpen(Player player) {
        handleClipboardOpenInternal(player, true);
    }

    private void handleClipboardOpenInternal(Player player, boolean notifyOnDenial) {
        GuiSession session = sessionManager.getSession(player);
        session.setLastMenuType(MenuType.CLIPBOARD);
        session.setViewingBrushId(null);
        session.setViewingBrushIndex(0);
        session.setViewingBrushMaterial(null);

        syncClipboardContext(player, session);

        fireNavigationEvent(player, MenuType.CLIPBOARD, session);
    }

    private void syncClipboardContext(Player player, GuiSession session) {
    }

    private void fireNavigationEvent(Player player, MenuType type, Object context) {
        OrionGuiNavigationEvent event = new OrionGuiNavigationEvent(player, type, context);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void openLastMenu(Player player) {
        MenuType type = sessionManager.getSession(player).getLastMenuType();
        switch (type) {
            case SETTINGS, BRUSH -> openBrushSettings(player);
            // [ORION_CUT: RecolorModule]
            case RECOLOR -> openRecolorMenuInternal(player, false);
            // [ORION_CUT_END]
            default -> openBrushSelector(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof OrionMenuHolder menuHolder)) return;

        if (event.getClickedInventory() != event.getInventory()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        GuiMenuButton button = menuHolder.getButton(event.getSlot());

        if (button != null) {
            button.onClick(event);
            return;
        }

        processActionItem(player, event.getCurrentItem(), event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof OrionMenuHolder)) return;

        int topSize = event.getInventory().getSize();
        boolean touchesMenu = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);

        if (touchesMenu) {
            event.setCancelled(true);
        }
    }

    private void processActionItem(Player player, ItemStack item, InventoryClickEvent event) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleGlobalNavigation(player, action, event);
        }
    }

    private void handleGlobalNavigation(Player player, String action, InventoryClickEvent event) {
        switch (action) {
            case "OPEN_BRUSH_LIST" -> openBrushSelector(player);
            case "OPEN_BRUSH_SETTINGS" -> openBrushSettings(player);
        }
    }
}