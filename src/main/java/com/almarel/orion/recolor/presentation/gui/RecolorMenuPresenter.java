package com.almarel.orion.recolor.presentation.gui;

import com.almarel.orion.recolor.core.RecolorMapBuilder;
import com.almarel.orion.recolor.core.RegionBlockTypeScanner;
import com.almarel.orion.recolor.palette.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Чистая бизнес-логика GUI recolor. Не содержит Bukkit Inventory API —
 * не знает о слотах, страницах-в-терминах-инвентаря, рендере иконок.
 * PAGE_SIZE сознательно не хранится здесь: View решает, сколько пар
 * помещается на экран, Presenter лишь режет список по переданному размеру.
 *
 * Preview (resolvePreview) и финальный Apply (RecolorApplyService, Stage 4)
 * обязаны использовать один и тот же RecolorMapBuilder.build(...) — это
 * гарантирует, что увиденное в GUI совпадёт с результатом применения.
 */
@Singleton
public class RecolorMenuPresenter {

    private final RegionBlockTypeScanner scanner;
    private final RecolorMapBuilder mapBuilder;
    private final RecolorSessionStore sessionStore;

    private final DominantColorExtractor dominantColorExtractor;
    private final QuickPalettePreviewCalculator quickPalettePreviewCalculator;
    private final PaletteVariantGenerator paletteVariantGenerator;
    private final DominantGroupAnchorSelector groupAnchorSelector;

    private static final List<String> QUICK_PALETTE_PRESET_NAMES =
            List.of("Complementary", "Triadic (+)", "Triadic (-)", "Split-Complementary (+)", "Analogous (+)");

    private static final List<HarmonyPreset> QUICK_PALETTE_PRESETS =
            HarmonySchemePresets.ALL.stream()
                    .filter(preset -> QUICK_PALETTE_PRESET_NAMES.contains(preset.displayName()))
                    .toList();

    @Inject
    public RecolorMenuPresenter(RegionBlockTypeScanner scanner,
                                RecolorMapBuilder mapBuilder,
                                RecolorSessionStore sessionStore,
                                DominantColorExtractor dominantColorExtractor,
                                QuickPalettePreviewCalculator quickPalettePreviewCalculator,
                                PaletteVariantGenerator paletteVariantGenerator,
                                DominantGroupAnchorSelector groupAnchorSelector) {
        this.scanner = scanner;
        this.mapBuilder = mapBuilder;
        this.sessionStore = sessionStore;
        this.dominantColorExtractor = dominantColorExtractor;
        this.quickPalettePreviewCalculator = quickPalettePreviewCalculator;
        this.paletteVariantGenerator = paletteVariantGenerator;
        this.groupAnchorSelector = groupAnchorSelector;
    }

    public RecolorGuiSession getOrCreateSession(Player player, World world, Region region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        RecolorGuiSession existing = sessionStore.getExisting(player);
        if (existing != null && existing.matchesRegion(world, min, max)) {
            return existing;
        }

        RecolorGuiSession fresh = scanRegion(world, region, min, max);
        sessionStore.put(player, fresh);
        return fresh;
    }

    private RecolorGuiSession scanRegion(World world, Region region, BlockVector3 min, BlockVector3 max) {
        Map<BlockType, Long> counts = scanner.scanTypeCounts(world, region);

        List<RecolorEntry> entries = counts.keySet().stream()
                .map(RecolorEntry::new)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

        List<DominantColorGroup> dominantGroups = dominantColorExtractor.extract(counts);
        List<QuickPalettePreviewOption> quickPaletteOptions =
                quickPalettePreviewCalculator.calculate(dominantGroups, QUICK_PALETTE_PRESETS);

        return new RecolorGuiSession(world, min, max, entries, dominantGroups, quickPaletteOptions);
    }

    // Q2: тяжёлый путь — вызывается только по клику на конкретную иконку, строго async
    public Map<BlockType, BlockType> resolveQuickPaletteTargets(RecolorGuiSession session,
                                                                QuickPalettePreviewOption option) {
        List<DominantColorGroup> groups = session.getDominantGroups();
        if (groups.isEmpty()) {
            return Map.of();
        }

        DominantColorGroup anchor = groupAnchorSelector.selectAnchor(groups);
        return paletteVariantGenerator.resolveVariantTargets(groups, anchor, option.preset());
    }

    public void applyResolvedTargets(RecolorGuiSession session, Map<BlockType, BlockType> resolvedTargets) {
        for (RecolorEntry entry : session.getEntries()) {
            BlockType target = resolvedTargets.get(entry.getOriginal());
            entry.setDesiredTarget(target);
        }
    }

    public boolean trySetDesiredTarget(RecolorEntry entry, @Nullable ItemStack cursorItem) {
        if (cursorItem == null || cursorItem.getType() == Material.AIR || !cursorItem.getType().isBlock()) {
            return false;
        }

        BlockType rawTarget;
        try {
            rawTarget = BukkitAdapter.asBlockType(cursorItem.getType());
        } catch (Exception e) {
            return false;
        }

        if (rawTarget == null) {
            return false;
        }

        entry.setDesiredTarget(rawTarget);
        return true;
    }

    public void clearDesiredTarget(RecolorEntry entry) {
        entry.clearDesiredTarget();
    }

    public Optional<BlockType> resolvePreview(RecolorEntry entry) {
        BlockType desired = entry.getDesiredTarget();
        if (desired == null) {
            return Optional.empty();
        }

        Map<BlockType, BlockType> resolved = mapBuilder.build(Map.of(entry.getOriginal(), desired));
        return Optional.ofNullable(resolved.get(entry.getOriginal()));
    }

    public Map<BlockType, BlockType> collectDesiredTargets(RecolorGuiSession session) {
        Map<BlockType, BlockType> result = new LinkedHashMap<>();
        for (RecolorEntry entry : session.getEntries()) {
            if (entry.hasDesiredTarget()) {
                result.put(entry.getOriginal(), entry.getDesiredTarget());
            }
        }
        return result;
    }

    public List<RecolorEntry> getPageEntries(RecolorGuiSession session, int pageSize) {
        List<RecolorEntry> all = session.getEntries();
        int fromIndex = Math.min(session.getCurrentPage() * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        return all.subList(fromIndex, toIndex);
    }

    public int getTotalPages(RecolorGuiSession session, int pageSize) {
        int totalItems = session.getEntries().size();
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    public boolean isActiveSession(Player player, RecolorGuiSession session) {
        return sessionStore.getExisting(player) == session;
    }
}