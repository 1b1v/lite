package com.almarel.orion.brush.fawe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.enums.BrushProvider;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.fawe.receipt.InjectedToolReceiptRegistry;
import com.almarel.orion.brush.fawe.receipt.SlotVerdict;
import com.almarel.orion.brush.fawe.data.ArceonBrushState;
import com.almarel.orion.brush.fawe.data.ArceonConstructBrushState;
import com.almarel.orion.brush.fawe.data.FaweBrushState;
import com.almarel.orion.brush.fawe.data.FaweConstructBrushState;
import com.almarel.orion.brush.fawe.data.FaweToolState;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import com.almarel.orion.brush.BrushManager;
import com.almarel.orion.brush.fawe.adapters.OrionBrushAdapter;
import com.almarel.orion.brush.fawe.gateway.FaweBrushInjector;
import com.almarel.orion.brush.fawe.gateway.FaweOptionWriter;
import com.almarel.orion.brush.fawe.gateway.FaweSessionGateway;
import com.almarel.orion.brush.fawe.gateway.FaweStateReader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

@Singleton
public class FaweIntegrationService {

    private final NativeBrushService nativeBrushService;
    private final FaweBrushRegistry aliasManager;

    // Новые компоненты из разбиения WorldEditGateway
    private final FaweSessionGateway sessionGateway;
    private final FaweStateReader stateReader;
    private final FaweOptionWriter optionWriter;
    private final FaweBrushInjector brushInjector;
    private final InjectedToolReceiptRegistry receiptRegistry;

    @Inject
    public FaweIntegrationService(NativeBrushService nativeBrushService,
                                  FaweBrushRegistry aliasManager,
                                  FaweSessionGateway sessionGateway,
                                  FaweStateReader stateReader,
                                  FaweOptionWriter optionWriter,
                                  FaweBrushInjector brushInjector,
                                  InjectedToolReceiptRegistry receiptRegistry) {
        this.nativeBrushService = nativeBrushService;
        this.aliasManager = aliasManager;
        this.sessionGateway = sessionGateway;
        this.stateReader = stateReader;
        this.optionWriter = optionWriter;
        this.brushInjector = brushInjector;
        this.receiptRegistry = receiptRegistry;
    }
    // --- State Management ---

    public void updateSnapshot(UUID uuid, BrushState state) {
        stateReader.captureBrushState(uuid, state);
    }

    // --- Restore Logic ---

    public boolean restoreFaweBrush(Player player, BrushState state) {
        if (state == null) return false;

        String commandString = extractCommandString(state);
        if (commandString == null || commandString.isEmpty()) return false;

        try {
            boolean success = nativeBrushService.equipNativeBrush(player, commandString);
            if (success && player.isOnline()) {
                // Синхронное JIT-применение опций без задержек
                optionWriter.applyBrushOptions(player, state);
            }
            // [CRITICAL - SILENT FAILURES] Если API не смогло собрать кисть, просто возвращаем false.
            // КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО вызывать player.performCommand(commandString).
            return success;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error during quiet brush restoration: " + commandString, e);
            return false;
        }
    }

    private String extractCommandString(BrushState state) {
        // [ORION_CUT: FaweConstructBrushState]
        if (state instanceof FaweConstructBrushState fs) return fs.getFullCommandString();
        // [ORION_CUT_END]

        if (state instanceof FaweBrushState fs) return fs.getFullCommandString();

        // [ORION_CUT: ArceonConstructBrushState]
        if (state instanceof ArceonConstructBrushState as) return as.getFullCommandString();
        // [ORION_CUT_END]

        if (state instanceof ArceonBrushState as) return as.getFullCommandString();
        if (state instanceof FaweToolState ts) return ts.getFullCommandString();

        return null;
    }

    // --- Helpers & Injection ---

    public void unbindFaweBrushSilently(Player player, Material toolMaterial) {
        sessionGateway.unbindBrush(player, toolMaterial);
    }

    public boolean hasActiveBrush(Player player, Material toolMaterial) {
        return sessionGateway.hasActiveBrush(player, toolMaterial);
    }

    public void injectOrionBrush(Player player, BrushManager brushManager, OrionBrushType type, BrushState params) {
        // Создаем ЕДИНСТВЕННЫЙ универсальный адаптер, передавая ему тип кисти.
        OrionBrushAdapter adapter = new OrionBrushAdapter(brushManager, type);

        Material tool = params != null ? params.getTool() : player.getInventory().getItemInMainHand().getType();

        brushInjector.injectAdapter(player, tool, adapter);

        if (params != null) {
            optionWriter.applyBrushOptions(player, params);
        }
    }

    public boolean isOrionBrushActive(Player player, Material toolMaterial, OrionBrushType type) {
        // Получаем сырый адаптер из FAWE
        Brush currentBrush = brushInjector.getActiveAdapter(player, toolMaterial);

        // Если адаптер есть, и это НАШ универсальный адаптер...
        if (currentBrush instanceof OrionBrushAdapter orionAdapter) {
            // Просто сверяем тип!
            return orionAdapter.getType() == type;
        }

        return false;
    }

    // --- Direct State Accessors ---

    public int getBrushSize(Player player, Material toolMaterial) { // no usage
        return sessionGateway.getBrushSize(player, toolMaterial);
    }

    public void setBrushSize(Player player, Material toolMaterial, int size) {
        sessionGateway.setBrushSize(player, toolMaterial, size);
    }

    public int getBrushRange(Player player, Material toolMaterial) { // no usage
        return sessionGateway.getBrushRange(player, toolMaterial);
    }

    public void setBrushRange(Player player, Material toolMaterial, int range) {
        sessionGateway.setBrushRange(player, toolMaterial, range);
    }

    public String getBrushMask(Player player, Material toolMaterial) { // no usage
        return sessionGateway.getBrushMask(player, toolMaterial);
    }

    public boolean setBrushMask(Player player, Material toolMaterial, String mask) {
        return sessionGateway.setBrushMask(player, toolMaterial, mask);
    }

    public String getBrushPattern(Player player, Material toolMaterial) { // no usage
        return sessionGateway.getBrushPattern(player, toolMaterial);
    }

    public boolean setSourceMask(Player player, Material toolMaterial, String sourceMask) {
        return sessionGateway.setSourceMask(player, toolMaterial, sourceMask);
    }

    public boolean setBrushPattern(Player player, Material toolMaterial, String pattern) {
        return sessionGateway.setBrushPattern(player, toolMaterial, pattern);
    }

    // --- Alias Resolvers ---

    public BrushProvider identifyProvider(String brushName) {
        if (brushName == null) return BrushProvider.FAWE;
        if (aliasManager.isArceonBrush(brushName)) {
            return BrushProvider.ARCEON;
        }
        return BrushProvider.FAWE;
    }

    public boolean isTool(String brushName) {
        if (brushName == null) return false;
        return aliasManager.isTool(brushName);
    }

    public boolean supportsConstructs(String brushName) {
        if (brushName == null) return false;
        return aliasManager.supportsConstructs(brushName);
    }

    // --- Slot Receipts (кто владеет инструментом в слоте) ---

    /** Намеренная запись DTO: ближайший клик вправе перезаписать слот. */
    public void markSlotPending(Player player, Material tool) {
        receiptRegistry.markPending(player.getUniqueId(), tool);
    }

    public void forgetSlotReceipt(Player player, Material tool) {
        receiptRegistry.forget(player.getUniqueId(), tool);
    }

    /** Фиксирует инструмент, который сейчас лежит в слоте, как установленный Орионом. Main Thread. */
    public void recordSlotReceipt(Player player, Material tool) {
        if (!isHolding(player, tool)) return;
        try {
            Tool current = sessionGateway.getToolInSlot(player);
            receiptRegistry.record(player.getUniqueId(), tool, current);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to record slot receipt: " + tool, e);
        }
    }

    /** Сбой чтения = UNKNOWN (неразрушающий вердикт: слот не трогаем, DTO не удаляем). Main Thread. */
    public SlotVerdict classifySlot(Player player, Material tool) {
        if (!isHolding(player, tool)) return SlotVerdict.UNKNOWN;
        try {
            Tool current = sessionGateway.getToolInSlot(player);
            return receiptRegistry.classify(player.getUniqueId(), tool, current);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to classify slot: " + tool, e);
            return SlotVerdict.UNKNOWN;
        }
    }

    private boolean isHolding(Player player, Material tool) {
        return player.getInventory().getItemInMainHand().getType() == tool;
    }

    // --- Lifecycle ---

}