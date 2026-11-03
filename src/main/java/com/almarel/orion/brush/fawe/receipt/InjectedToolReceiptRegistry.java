package com.almarel.orion.brush.fawe.receipt;

import com.google.inject.Singleton;
import com.sk89q.worldedit.command.tool.NavigationWand;
import com.sk89q.worldedit.command.tool.SelectionWand;
import com.sk89q.worldedit.command.tool.Tool;
import org.bukkit.Material;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Квитанции инжекта: какой объект Tool Орион сам положил в слот предмета игрока.
 * <p>
 * Хранит только WeakReference на Tool (LocalSession держит его сильной ссылкой) и UUID,
 * без Player. Не очищается при выходе игрока: если LocalSession истекла, ссылка умирает сама,
 * а если пережила релог, квитанция остаётся валидной.
 * <p>
 * Структуры lock-free; сам Tool читается вызывающим кодом из LocalSession в Main Thread
 */
@Singleton
public class InjectedToolReceiptRegistry {

    private record SlotReceipt(WeakReference<Tool> tool, boolean pending) {

        static final SlotReceipt PENDING_ONLY = new SlotReceipt(null, true);

        Tool liveTool() {
            return tool == null ? null : tool.get();
        }

        SlotReceipt asPending() {
            return new SlotReceipt(tool, true);
        }
    }

    private final Map<UUID, Map<Material, SlotReceipt>> receipts = new ConcurrentHashMap<>();

    /**
     * Намеренная запись DTO (GUI, команда, пресет): ближайшая установка вправе перезаписать слот.
     */
    public void markPending(UUID uuid, Material slot) {
        slotsOf(uuid).merge(slot, SlotReceipt.PENDING_ONLY, (old, ignored) -> old.asPending());
    }

    public void record(UUID uuid, Material slot, Tool tool) {
        if (tool == null) {
            return;
        }
        Map<Material, SlotReceipt> slots = slotsOf(uuid);
        if (isAlreadyRecorded(slots.get(slot), tool)) {
            return;
        }
        slots.put(slot, new SlotReceipt(new WeakReference<>(tool), false));
    }

    private boolean isAlreadyRecorded(SlotReceipt receipt, Tool tool) {
        return receipt != null && !receipt.pending() && receipt.liveTool() == tool;
    }

    /**
     * Удаляет запись о слоте (например, после сброса устаревшего DTO).
     */
    public void forget(UUID uuid, Material slot) {
        Map<Material, SlotReceipt> slots = receipts.get(uuid);
        if (slots != null) {
            slots.remove(slot);
        }
    }

    public SlotVerdict classify(UUID uuid, Material slot, Tool current) {
        SlotReceipt receipt = findReceipt(uuid, slot);
        if (receipt != null && receipt.pending()) {
            return SlotVerdict.WRITABLE;
        }
        if (current == null) {
            return SlotVerdict.WRITABLE;
        }
        return classifyPresent(receipt, current);
    }

    private SlotVerdict classifyPresent(SlotReceipt receipt, Tool current) {
        Tool owned = (receipt == null) ? null : receipt.liveTool();
        if (owned == current) {
            return SlotVerdict.OWNED;
        }
        if (owned != null) {
            return SlotVerdict.FOREIGN;
        }
        return isDefaultWand(current) ? SlotVerdict.WRITABLE : SlotVerdict.UNKNOWN;
    }

    private boolean isDefaultWand(Tool tool) {
        return tool instanceof SelectionWand || tool instanceof NavigationWand;
    }

    private SlotReceipt findReceipt(UUID uuid, Material slot) {
        Map<Material, SlotReceipt> slots = receipts.get(uuid);
        return (slots == null) ? null : slots.get(slot);
    }

    private Map<Material, SlotReceipt> slotsOf(UUID uuid) {
        return receipts.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>());
    }
}