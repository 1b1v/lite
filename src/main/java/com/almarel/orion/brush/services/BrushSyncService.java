package com.almarel.orion.brush.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.BrushManager;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.brush.fawe.receipt.SlotVerdict;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.gui.events.OrionGuiNavigationEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Singleton
public class BrushSyncService implements Listener {

    private final BrushStateManager brushStateManager;
    private final FaweIntegrationService faweIntegrationService;
    private final BrushManager brushManager;

    @Inject
    public BrushSyncService(BrushStateManager brushStateManager,
                            FaweIntegrationService faweIntegrationService,
                            BrushManager brushManager) {
        this.brushStateManager = brushStateManager;
        this.faweIntegrationService = faweIntegrationService;
        this.brushManager = brushManager;
    }

    @EventHandler
    public void onGuiNavigation(OrionGuiNavigationEvent event) {
        if (event.getPlayer() != null) {
            syncActiveTool(event.getPlayer());
        }
    }

    public void syncActiveTool(Player player) {
        if (player == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null) {
            syncStateToSnapshot(player, item.getType());
        }
    }

    public void savePassiveTool(Player player, ItemStack item) {
        if (player == null || item == null) return;
        syncStateToSnapshot(player, item.getType());
    }

    public boolean restoreActiveTool(Player player, ItemStack item) {
        if (item == null || isInvalidTool(item.getType())) return false;

        Material tool = item.getType();
        BrushState state = brushStateManager.resolveActiveState(player, tool);
        if (state == null) return false;

        state.cachePlayerOrientation(player);
        if (state.getBrushType() == OrionBrushType.GENERIC) return false;

        return applyVerdict(player, tool, state, faweIntegrationService.classifySlot(player, tool));
    }

    private boolean applyVerdict(Player player, Material tool, BrushState state, SlotVerdict verdict) {
        return switch (verdict) {
            case FOREIGN -> discardStaleState(player, tool);
            case UNKNOWN -> handleUnknownSlot(player, tool, state);
            case WRITABLE, OWNED -> restoreAndRecord(player, tool, state, verdict);
        };
    }

    /** В слоте инструмент, поставленный не Орионом: DTO устарел, FAWE не трогаем. */
    private boolean discardStaleState(Player player, Material tool) {
        brushStateManager.removeBrushSettings(player, tool);
        return false;
    }

    /** Квитанции нет: усыновляем только если в слоте реально лежит ожидаемая кисть. */
    private boolean handleUnknownSlot(Player player, Material tool, BrushState state) {
        if (!slotMatchesState(player, tool, state)) return false;
        return restoreAndRecord(player, tool, state, SlotVerdict.OWNED);
    }

    private boolean slotMatchesState(Player player, Material tool, BrushState state) {
        OrionBrushType type = state.getBrushType();
        if (type == OrionBrushType.FAWE) {
            return faweIntegrationService.hasActiveBrush(player, tool);
        }
        return faweIntegrationService.isOrionBrushActive(player, tool, type);
    }

    private boolean restoreAndRecord(Player player, Material tool, BrushState state, SlotVerdict verdict) {
        boolean ready = dispatchBrushRestoration(player, tool, state, verdict);
        if (ready) {
            faweIntegrationService.recordSlotReceipt(player, tool);
        }
        return ready;
    }

    private boolean dispatchBrushRestoration(Player player, Material tool, BrushState state, SlotVerdict verdict) {
        OrionBrushType type = state.getBrushType();
        if (type == OrionBrushType.FAWE) {
            return processFaweBrush(player, tool, state, verdict);
        }
        return processOrionBrush(player, tool, type, state);
    }

    private boolean processFaweBrush(Player player, Material tool, BrushState state, SlotVerdict verdict) {
        if (shouldRecreateFaweBrush(player, tool, state, verdict)
                && !faweIntegrationService.restoreFaweBrush(player, state)) {
            return false; // кисть не встала: чужой слот не оборачиваем прокси и не фиксируем
        }

        return true;
    }

    private boolean shouldRecreateFaweBrush(Player player, Material tool, BrushState state, SlotVerdict verdict) {
        if (state.needsRecreationOnDesync()) return true;
        if (verdict == SlotVerdict.OWNED) return false;
        return !faweIntegrationService.hasActiveBrush(player, tool);
    }

    private boolean processOrionBrush(Player player, Material tool, OrionBrushType type, BrushState state) {
        boolean active = faweIntegrationService.isOrionBrushActive(player, tool, type);
        if (!active) {
            faweIntegrationService.injectOrionBrush(player, brushManager, type, state);
            active = faweIntegrationService.isOrionBrushActive(player, tool, type);
        }
        return active;
    }

    private void syncStateToSnapshot(Player player, Material tool) {
        if (isInvalidTool(tool)) return;

        BrushState state = brushStateManager.resolveActiveState(player, tool);
        if (state == null || state.getBrushType() == OrionBrushType.GENERIC) return;

        if (discardIfForeign(player, tool)) return;

        if (state.getBrushType() == OrionBrushType.FAWE) {
            faweIntegrationService.updateSnapshot(player.getUniqueId(), state);
        }
    }

    /** В слоте инструмент, поставленный не Орионом: DTO устарел, FAWE не читаем. */
    private boolean discardIfForeign(Player player, Material tool) {
        if (faweIntegrationService.classifySlot(player, tool) != SlotVerdict.FOREIGN) {
            return false;
        }
        brushStateManager.removeBrushSettings(player, tool);
        return true;
    }

    private boolean isInvalidTool(Material tool) {
        return tool == null || tool == Material.AIR || tool.isBlock();
    }
}