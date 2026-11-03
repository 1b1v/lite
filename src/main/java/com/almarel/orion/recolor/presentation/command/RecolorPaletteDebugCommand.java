package com.almarel.orion.recolor.presentation.command;

import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.recolor.core.RegionBlockTypeScanner;
import com.almarel.orion.recolor.palette.DominantColorExtractor;
import com.almarel.orion.recolor.palette.DominantColorGroup;
import com.almarel.orion.recolor.palette.PaletteVariant;
import com.almarel.orion.recolor.palette.PaletteVariantGenerator;
import com.almarel.orion.recolor.core.RecolorSelection;
import com.almarel.orion.recolor.core.RecolorSelectionResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Временный admin-инструмент для проверки палитрового трека (recolor.palette)
 * на реальных постройках без GUI. Не является частью финального UX — после
 * того как алгоритм кластеризации/гармонии будет проверен и откалиброван,
 * эта команда либо выпиливается, либо остаётся как скрытый debug-инструмент.
 * <p>
 * Намеренно не трогает RecolorMenuPresenter/RecolorMenuView ([DONE],
 * протестированы на сервере) — изолированная точка входа, которую можно
 * безопасно менять/удалить, не рискуя сломать финальный интерфейс.
 */
@Singleton
public class RecolorPaletteDebugCommand extends AbstractCommand {

    private static final List<String> ALIASES = List.of(
            "//recolorpalette", "/recolorpalette", "recolorpalette",
            "//rpal", "/rpal", "rpal"
    );
    private static final String PERMISSION = OrionPermissions.RECOLOR_USE;
    private static final int MAX_TARGET_LINES_PER_VARIANT = 12;

    private final RecolorSelectionResolver selectionResolver;
    private final RegionBlockTypeScanner scanner;
    private final DominantColorExtractor dominantColorExtractor;
    private final PaletteVariantGenerator paletteVariantGenerator;
    private final TaskRunner taskRunner;

    @Inject
    public RecolorPaletteDebugCommand(FaweSuggestionProvider faweSuggestionProvider,
                                      RecolorSelectionResolver selectionResolver,
                                      RegionBlockTypeScanner scanner,
                                      DominantColorExtractor dominantColorExtractor,
                                      PaletteVariantGenerator paletteVariantGenerator,
                                      TaskRunner taskRunner) {
        super(faweSuggestionProvider);
        this.selectionResolver = selectionResolver;
        this.scanner = scanner;
        this.dominantColorExtractor = dominantColorExtractor;
        this.paletteVariantGenerator = paletteVariantGenerator;
        this.taskRunner = taskRunner;
    }

    @Override
    public @NotNull String getPermission() {
        return PERMISSION;
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return ALIASES;
    }

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return true;
        }

        Optional<RecolorSelection> selectionOpt = selectionResolver.resolve(player);
        if (selectionOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "[Recolor Debug] Нет активного WorldEdit-выделения.");
            return true;
        }

        RecolorSelection selection = selectionOpt.get();
        player.sendMessage(ChatColor.GRAY + "[Recolor Debug] Сканирую регион...");

        taskRunner.supplyAsync(() -> computeReport(selection))
                .thenAccept(report -> taskRunner.runSyncIfOnline(player, p -> sendReport(p, report)));

        return true;
    }

    /**
     * Выполняется в async (TaskRunner.supplyAsync) — чтение чанков через
     * FAWE + вся CPU-математика кластеризации/гармонии. Возвращаемый
     * DebugReport не содержит ни одного Bukkit-объекта, поэтому безопасно
     * передаётся между потоками.
     */
    private DebugReport computeReport(RecolorSelection selection) {
        Map<BlockType, Long> counts = scanner.scanTypeCounts(selection.world(), selection.region());
        List<DominantColorGroup> groups = dominantColorExtractor.extract(counts);
        List<PaletteVariant> variants = paletteVariantGenerator.generateVariants(counts);
        long totalBlocksScanned = counts.values().stream().mapToLong(Long::longValue).sum();
        return new DebugReport(groups, variants, totalBlocksScanned);
    }

    /**
     * Выполняется в main thread (TaskRunner.runSyncIfOnline) — единственное
     * место, где этот класс трогает Bukkit API (player.sendMessage).
     */
    private void sendReport(Player player, DebugReport report) {
        player.sendMessage(ChatColor.GOLD + "=== Recolor Palette Debug ===");
        player.sendMessage(ChatColor.GRAY + "Отсканировано блоков: " + report.totalBlocksScanned());

        if (report.groups().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Доминирующие группы не найдены "
                    + "(регион пуст или все блоки исключены/без визуального профиля).");
            return;
        }

        player.sendMessage(ChatColor.GRAY + "Найдено доминирующих групп: " + report.groups().size());
        sendGroups(player, report.groups());

        if (report.variants().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Гармоничные варианты не сгенерированы "
                    + "(не найдено подходящих блоков-кандидатов под новые цвета).");
            return;
        }

        player.sendMessage(ChatColor.GRAY + "Сгенерировано схем: " + report.variants().size());
        for (PaletteVariant variant : report.variants()) {
            sendVariant(player, variant);
        }
    }

    private void sendGroups(Player player, List<DominantColorGroup> groups) {
        int index = 1;
        for (DominantColorGroup group : groups) {
            String percent = String.format("%.0f%%", group.weightFraction() * 100.0);
            player.sendMessage(ChatColor.YELLOW + "[" + index + "] " + ChatColor.WHITE
                    + group.representativeBlockType().id() + ChatColor.GRAY
                    + " (вес " + percent + ", объём " + group.blockCount()
                    + ", членов группы: " + group.memberBlockTypes().size() + ")");

            if (group.memberBlockTypes().size() > 1) {
                String members = formatMemberIds(group);
                player.sendMessage(ChatColor.DARK_GRAY + "    члены: " + members);
            }
            index++;
        }
    }

    private String formatMemberIds(DominantColorGroup group) {
        return group.memberBlockTypes().stream()
                .map(BlockType::id)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private void sendVariant(Player player, PaletteVariant variant) {
        player.sendMessage(ChatColor.AQUA + "— " + variant.schemeName() + " —");

        Map<BlockType, BlockType> sortedTargets = new TreeMap<>(
                (a, b) -> a.id().compareTo(b.id()));
        sortedTargets.putAll(variant.desiredTargets());

        int printed = 0;
        for (Map.Entry<BlockType, BlockType> entry : sortedTargets.entrySet()) {
            if (printed >= MAX_TARGET_LINES_PER_VARIANT) {
                int remaining = sortedTargets.size() - printed;
                player.sendMessage(ChatColor.DARK_GRAY + "   ...и ещё " + remaining + " замен(ы)");
                break;
            }
            player.sendMessage(ChatColor.GRAY + "   " + entry.getKey().id()
                    + ChatColor.DARK_GRAY + " -> " + ChatColor.GRAY + entry.getValue().id());
            printed++;
        }
    }

    private record DebugReport(List<DominantColorGroup> groups, List<PaletteVariant> variants,
                               long totalBlocksScanned) {
    }
}