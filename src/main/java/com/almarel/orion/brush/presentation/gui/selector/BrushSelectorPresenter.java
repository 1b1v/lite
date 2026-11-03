package com.almarel.orion.brush.presentation.gui.selector;

import com.google.inject.Inject;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.brush.presentation.gui.settings.BrushSettingsRouter;
import com.almarel.orion.brush.services.BrushQueryService;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.gui.interaction.DoubleClickDetector;
import com.almarel.orion.framework.gui.interaction.GuiSwapService;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.player.inventory.PlayerHotbarSlotAllocator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Единственная точка принятия решений по клику в GUI-селекторе кистей.
 * Не содержит Bukkit Inventory API (OrionMenuHolder/Inventory) — только
 * решения "что сделать" и делегирование служебным сервисам; "как это
 * отразится на экране" решает BrushSelectorView через переданные колбэки.
 */
public class BrushSelectorPresenter {

    private static final long EQUIP_DELAY_TICKS = 6L;       // ~300ms, синхронно с окном DoubleClickDetector
    private static final long PRESET_BIND_DELAY_TICKS = 8L; // отдельная задержка для bind+reopen пресета

    private final BrushItemRenderer brushItemRenderer;
    private final BrushSettingsRouter settingsRouter;
    private final BrushQueryService brushQueryService;
    private final DoubleClickDetector doubleClickDetector;
    private final GuiSwapService swapService;
    private final JavaPlugin plugin;

    private final BrushStateManager brushStateManager;
    private final FaweIntegrationService faweIntegrationService;
    private final PlayerHotbarSlotAllocator hotbarSlotAllocator;

    private final Map<UUID, BukkitTask> pendingActions = new ConcurrentHashMap<>();

    @Inject
    public BrushSelectorPresenter(BrushItemRenderer brushItemRenderer,
                                  BrushSettingsRouter settingsRouter,
                                  BrushQueryService brushQueryService,
                                  DoubleClickDetector doubleClickDetector,
                                  GuiSwapService swapService,
                                  JavaPlugin plugin,
                                  BrushStateManager brushStateManager,
                                  FaweIntegrationService faweIntegrationService,
                                  PlayerHotbarSlotAllocator hotbarSlotAllocator) {
        this.brushItemRenderer = brushItemRenderer;
        this.settingsRouter = settingsRouter;
        this.brushQueryService = brushQueryService;
        this.doubleClickDetector = doubleClickDetector;
        this.swapService = swapService;
        this.plugin = plugin;
        this.brushStateManager = brushStateManager;
        this.faweIntegrationService = faweIntegrationService;
        this.hotbarSlotAllocator = hotbarSlotAllocator;
    }

    public record BrushViewItem(
            BrushState state
    ) {
        public boolean isPreset() {
            boolean result = false;
            return result;
        }

        public String getUniqueKey() {
            return state.getStorageKey();
        }
    }

    // ==========================================
    // Sorting / Snapshot
    // ==========================================

    public List<BrushViewItem> getSortedBrushes(Player player, BrushSelectorSession selectorSession) {
        List<BrushViewItem> validBrushes = brushQueryService.getMergedBrushes(player, selectorSession.getBrushFilter());

        if (!selectorSession.hasSelectorSnapshot()) {
            initializeSelectorSnapshot(selectorSession, validBrushes);
        } else {
            sortBrushesBySnapshot(selectorSession.getSelectorSnapshot(), validBrushes);
        }

        return validBrushes;
    }

    private void initializeSelectorSnapshot(BrushSelectorSession selectorSession, List<BrushViewItem> brushes) {
        brushes.sort(Comparator
                .comparingInt((BrushViewItem item) -> item.state().getOrderIndex())
                .thenComparingInt(item -> -item.state().getSortPriority()));

        selectorSession.setSelectorSnapshot(extractSnapshotKeys(brushes));
    }

    private List<String> extractSnapshotKeys(List<BrushViewItem> brushes) {
        List<String> snapshot = new ArrayList<>(brushes.size());
        for (BrushViewItem item : brushes) {
            snapshot.add(item.getUniqueKey());
        }
        return snapshot;
    }

    private void sortBrushesBySnapshot(List<String> snapshot, List<BrushViewItem> brushes) {
        brushes.sort(Comparator.comparingInt(item -> {
            int index = snapshot.indexOf(item.getUniqueKey());
            return index == -1 ? Integer.MAX_VALUE : index;
        }));
    }

    // ==========================================
    // Click Dispatch (единственная точка входа)
    // ==========================================

    /**
     * @param deferredGuard    проверка, актуально ли ещё выполнять отложенное действие
     *                         (вызывается в момент срабатывания задержки, не сразу).
     * @param onDeferredRefresh колбэк "обновить вид", вызывается после отложенного действия.
     */
    public boolean handleClick(Player player,
                               GuiSession session,
                               BrushSelectorSession selectorSession,
                               BrushViewItem viewItem,
                               InventoryClickEvent event,
                               ItemStack itemIcon,
                               BooleanSupplier deferredGuard,
                               Runnable onDeferredRefresh) {
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            return false;
        }

        String uniqueKey = viewItem.getUniqueKey();

        if (session.isSwapping() && event.isLeftClick()) {
            return handleSwapIntercept(player, session, selectorSession, uniqueKey);
        }


        BrushState state = viewItem.state();

        if (event.getClick() == ClickType.MIDDLE) {
            return handleMiddleClick(selectorSession, state);
        }

        if (event.isLeftClick()) {
            return event.isShiftClick()
                    ? handlePersistenceToggle(player, state)
                    : handlePrimaryLeftClick(player, session, state, itemIcon);
        }

        if (event.isRightClick()) {
            return event.isShiftClick()
                    ? handleShiftRightClick(player, state)
                    : handleRightClick(player, state);
        }

        return false;
    }

    // ==========================================
    // Swap / Drag
    // ==========================================

    private boolean handleSwapIntercept(Player player, GuiSession session, BrushSelectorSession selectorSession, String targetKey) {
        String sourceKey = swapService.consumeSwap(player, session);
        if (sourceKey != null && sourceKey.equals(targetKey)) {
            return true;
        }
        return handleDragSwap(player, selectorSession, targetKey, sourceKey);
    }

    public boolean handleDragSwap(Player player, BrushSelectorSession selectorSession, String targetKey, String sourceKey) {
        if (sourceKey == null || sourceKey.equals(targetKey)) {
            return false;
        }

        List<String> snapshot = selectorSession.getSelectorSnapshot();
        swapSnapshotElements(snapshot, sourceKey, targetKey);


        if (snapshot != null) {
            syncBrushOrders(player, snapshot);
        }
        return true;
    }

    private void swapSnapshotElements(List<String> snapshot, String sourceKey, String targetKey) {
        if (snapshot == null) {
            return;
        }
        int sourceIndex = snapshot.indexOf(sourceKey);
        int targetIndex = snapshot.indexOf(targetKey);
        if (sourceIndex != -1 && targetIndex != -1) {
            java.util.Collections.swap(snapshot, sourceIndex, targetIndex);
        }
    }


    // ==========================================
    // Preset Click (единственный очаг — View больше не дублирует эту логику)
    // ==========================================


    // ==========================================
    // Publish Fork Action (из контекстного меню выбранной кисти)
    // ==========================================


    // ==========================================
    // Local Brush Click Handlers
    // ==========================================

    private boolean handlePrimaryLeftClick(Player player, GuiSession session, BrushState state, ItemStack itemIcon) {
        String key = state.getStorageKey();
        UUID uuid = player.getUniqueId();

        if (doubleClickDetector.isDoubleClick(session, key)) {
            cancelPending(uuid);
            swapService.prepareDrag(player, session, key, itemIcon);
            return false;
        }

        scheduleDelayed(uuid, EQUIP_DELAY_TICKS, () -> hotbarSlotAllocator.equip(player, state.getTool()));
        return false;
    }

    private boolean handlePersistenceToggle(Player player, BrushState state) {

        togglePersistent(state);
        return true;
    }


    private boolean handleMiddleClick(BrushSelectorSession selectorSession, BrushState state) {
        if (selectorSession.getSelectedBrushKey() != null && selectorSession.getSelectedBrushKey().equals(state.getStorageKey())) {
            selectorSession.clearSelectedBrushKey();
        } else {
            selectorSession.setSelectedBrushKey(state.getStorageKey());
        }
        return true;
    }

    private boolean handleRightClick(Player player, BrushState state) {

        deleteBrush(player, state);
        return true;
    }

    private boolean handleShiftRightClick(Player player, BrushState state) {
        hotbarSlotAllocator.equip(player, state.getTool());
        settingsRouter.open(player, state);
        return false;
    }

    public void cancelSwap(Player player, GuiSession session) {
        swapService.cancelSwap(player, session);
    }

    public BrushItemRenderer getRenderer() {
        return brushItemRenderer;
    }

    // ==========================================
    // State Mutation (перенесено из удалённого BrushSelectorService)
    // ==========================================

    private void togglePersistent(BrushState state) {
        state.setPersistent(!state.isPersistent());
    }

    private void deleteBrush(Player player, BrushState state) {
        if (faweIntegrationService != null) {
            faweIntegrationService.unbindFaweBrushSilently(player, state.getTool());
        }
        brushStateManager.removeBrushSettings(player, state.getTool());
    }

    private void syncBrushOrders(Player player, List<String> snapshot) {
        List<BrushState> brushes = brushStateManager.getAllActiveBrushes(player);
        for (int i = 0; i < snapshot.size(); i++) {
            updateBrushOrderIndex(brushes, snapshot.get(i), i);
        }
    }

    private void updateBrushOrderIndex(List<BrushState> brushes, String key, int newIndex) {
        for (BrushState b : brushes) {
            if (b.getStorageKey().equals(key)) {
                b.setOrderIndex(newIndex);
                return;
            }
        }
    }

    // ==========================================
    // Debounce Helpers (унифицированный паттерн pendingEquipTasks/pendingExecutions)
    // ==========================================

    private void cancelPending(UUID uuid) {
        BukkitTask task = pendingActions.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void scheduleDelayed(UUID uuid, long delayTicks, Runnable action) {
        cancelPending(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingActions.remove(uuid);
            action.run();
        }, delayTicks);
        pendingActions.put(uuid, task);
    }
}