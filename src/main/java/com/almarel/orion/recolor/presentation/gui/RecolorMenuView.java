package com.almarel.orion.recolor.presentation.gui;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.gui.NavBarRenderer;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.common.GuiMenuButton;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.data.GuiLayout;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.widgets.WidgetPageNavigation;
import com.almarel.orion.recolor.palette.QuickPalettePreviewOption;
import com.almarel.orion.recolor.core.RecolorSelection;
import com.almarel.orion.recolor.core.RecolorSelectionResolver;
import com.almarel.orion.recolor.presentation.gui.widgets.QuickPaletteWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.almarel.orion.framework.gui.configurationlegacy.Palette.*;

/**
 * Рендер и обработка кликов GUI-меню Recolor.
 * Любое изменение состояния полностью пересобирает инвентарь и переоткрывает его игроку.
 */
@Singleton
public class RecolorMenuView {

    private static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&8Recolor");
    private static final int PAGE_SIZE = 18;

    private static final int ROW_WAS_A = 9;
    private static final int ROW_BECOMES_A = 18;
    private static final int ROW_WAS_B = 27;
    private static final int ROW_BECOMES_B = 36;
    private static final int SLOT_APPLY = 51;
    private static final int SLOT_SELECTION_REQUIRED_WARNING = 31;

    private static final ItemStack HEADER_FILLER =
            new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

    private static final ItemStack SELECTION_REQUIRED_FILLER =
            new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(" ").build();

    private final RecolorMenuPresenter presenter;
    private final RecolorApplyService applyService;
    private final NavBarRenderer navigationService;
    private final CommonGuiConfig commonGuiConfig;
    private final RecolorSelectionResolver selectionResolver;
    private final TaskRunner taskRunner;

    @Inject
    public RecolorMenuView(RecolorMenuPresenter presenter,
                           RecolorApplyService applyService,
                           RecolorSelectionResolver selectionResolver,
                           NavBarRenderer navigationService,
                           CommonGuiConfig commonGuiConfig,
                           TaskRunner taskRunner) {
        this.presenter = presenter;
        this.applyService = applyService;
        this.selectionResolver = selectionResolver;
        this.navigationService = navigationService;
        this.commonGuiConfig = commonGuiConfig;
        this.taskRunner = taskRunner;
    }

    // ========================================================================
    // PUBLIC API / ENTRY POINTS
    // ========================================================================

    public void open(Player player, World world, Region region) {
        RecolorGuiSession session = presenter.getOrCreateSession(player, world, region);
        int totalPages = presenter.getTotalPages(session, PAGE_SIZE);
        session.setCurrentPage(clampPage(session.getCurrentPage(), totalPages));
        render(player, session);
    }

    /**
     * Открывает техническое состояние меню: регион не выделен.
     * Полностью декоративно — клики гасятся общим обработчиком GuiWindowManager.
     */
    public void openSelectionRequired(Player player) {
        OrionMenuHolder holder = new OrionMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, GuiLayout.ROWS_6, TITLE);
        holder.setInventory(inventory);

        fillSelectionRequiredBackground(inventory);
        inventory.setItem(SLOT_SELECTION_REQUIRED_WARNING, buildSelectionRequiredWarningIcon());

        player.openInventory(inventory);
    }

    // ========================================================================
    // RENDER PIPELINE
    // ========================================================================

    private void render(Player player, RecolorGuiSession session) {
        OrionMenuHolder holder = new OrionMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, GuiLayout.ROWS_6, TITLE);
        holder.setInventory(inventory);

        drawHeader(holder);
        drawQuickPaletteRow(holder, session, player);
        drawPairs(holder, session);
        navigationService.draw(inventory, player, MenuType.RECOLOR);
        drawApplyButton(holder, player, session);
        drawPagination(holder, player, session);

        player.openInventory(inventory);
    }

    private void drawHeader(OrionMenuHolder holder) {
        Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < GuiLayout.ROW_SIZE; slot++) {
            inventory.setItem(slot, HEADER_FILLER);
        }
    }

    // ========================================================================
    // QUICK PALETTE ROW
    // ========================================================================

    private void drawQuickPaletteRow(OrionMenuHolder holder, RecolorGuiSession session, Player player) {
        QuickPaletteWidget.draw(holder, session.getQuickPaletteOptions(),
                option -> handleQuickPaletteClick(session, player, option));
    }

    private void handleQuickPaletteClick(RecolorGuiSession session,
                                         Player player,
                                         QuickPalettePreviewOption option) {
        if (!session.tryBeginQuickPaletteResolve()) {
            return; // Предыдущий резолв сессии ещё выполняется
        }

        taskRunner.runAsync(() -> resolveQuickPaletteAsync(session, player, option));
    }

    private void resolveQuickPaletteAsync(RecolorGuiSession session,
                                          Player player,
                                          QuickPalettePreviewOption option) {
        Map<BlockType, BlockType> resolvedTargets = presenter.resolveQuickPaletteTargets(session, option);

        taskRunner.runSyncIfOnline(player, onlinePlayer ->
                applyQuickPaletteResultSync(onlinePlayer, session, resolvedTargets));
    }

    private void applyQuickPaletteResultSync(Player player,
                                             RecolorGuiSession session,
                                             Map<BlockType, BlockType> resolvedTargets) {
        session.endQuickPaletteResolve();

        if (!presenter.isActiveSession(player, session)) {
            return; // Игрок переоткрыл меню для другого региона
        }

        presenter.applyResolvedTargets(session, resolvedTargets);
        rerenderPreservingCursor(player, session);
    }

    // ========================================================================
    // PAIR ROWS (2-5)
    // ========================================================================

    private void drawPairs(OrionMenuHolder holder, RecolorGuiSession session) {
        List<RecolorEntry> pageEntries = presenter.getPageEntries(session, PAGE_SIZE);

        for (int i = 0; i < pageEntries.size(); i++) {
            RecolorEntry entry = pageEntries.get(i);
            drawOriginalSlot(holder, entry, computeWasSlot(i));
            drawTargetSlot(holder, session, entry, computeBecomesSlot(i));
        }
    }

    private int computeWasSlot(int index) {
        int column = index % GuiLayout.ROW_SIZE;
        int baseRow = (index < GuiLayout.ROW_SIZE) ? ROW_WAS_A : ROW_WAS_B;
        return baseRow + column;
    }

    private int computeBecomesSlot(int index) {
        int column = index % GuiLayout.ROW_SIZE;
        int baseRow = (index < GuiLayout.ROW_SIZE) ? ROW_BECOMES_A : ROW_BECOMES_B;
        return baseRow + column;
    }

    private void drawOriginalSlot(OrionMenuHolder holder, RecolorEntry entry, int slot) {
        ItemStack icon = buildOriginalIcon(entry);
        // Read-only слот намеренно без addButton
        holder.getInventory().setItem(slot, icon);
    }

    private ItemStack buildOriginalIcon(RecolorEntry entry) {
        return resolveDisplayMaterial(entry.getOriginal())
                .map(this::buildValidOriginalIcon)
                .orElseGet(() -> buildMissingItemIcon(entry.getOriginal().id()));
    }

    private ItemStack buildValidOriginalIcon(Material material) {
        return new ItemBuilder(material)
                .name(LORE_VAL + prettyName(material))
                .lore(LORE_TEXT + "Source block (read-only)")
                .glow()
                .build();
    }

    private ItemStack buildMissingItemIcon(String blockId) {
        return new ItemBuilder(Material.BARRIER)
                .name(ERROR + "No icon available")
                .lore(
                        LORE_TEXT + "This block has no inventory item form",
                        LORE_TEXT + "(" + blockId + ")"
                )
                .build();
    }

    private void drawTargetSlot(OrionMenuHolder holder, RecolorGuiSession session, RecolorEntry entry, int slot) {
        ItemStack icon = buildTargetIcon(entry);
        holder.addButton(slot, new GuiMenuButton(icon, event -> onTargetSlotClick(event, entry, session)));
    }

    private void onTargetSlotClick(InventoryClickEvent event, RecolorEntry entry, RecolorGuiSession session) {
        Player clicker = (Player) event.getWhoClicked();

        if (event.isRightClick()) {
            presenter.clearDesiredTarget(entry);
            rerenderPreservingCursor(clicker, session);
        } else if (event.isLeftClick()) {
            if (presenter.trySetDesiredTarget(entry, event.getCursor())) {
                rerenderPreservingCursor(clicker, session);
            }
        }
    }

    private ItemStack buildTargetIcon(RecolorEntry entry) {
        if (!entry.hasDesiredTarget()) {
            return new ItemBuilder(Material.AIR).build();
        }

        Optional<BlockType> resolved = presenter.resolvePreview(entry);
        if (resolved.isEmpty()) {
            return buildNoShapeMatchIcon();
        }

        return resolveDisplayMaterial(resolved.get())
                .map(this::buildResolvedTargetIcon)
                .orElseGet(() -> buildResolvedMissingItemIcon(resolved.get().id()));
    }

    private ItemStack buildResolvedTargetIcon(Material material) {
        return new ItemBuilder(material)
                .name(BTN_ACT + prettyName(material))
                .lore(commonGuiConfig.getText("controls.delete-pkm"))
                .build();
    }

    private ItemStack buildResolvedMissingItemIcon(String blockId) {
        return new ItemBuilder(Material.BARRIER)
                .name(ERROR + "No icon available")
                .lore(
                        LORE_TEXT + "Resolved block has no inventory item form",
                        LORE_TEXT + "(" + blockId + ")",
                        commonGuiConfig.getText("controls.delete-pkm")
                )
                .build();
    }

    private ItemStack buildNoShapeMatchIcon() {
        return new ItemBuilder(Material.BARRIER)
                .name(ERROR + "No shape match")
                .lore(
                        LORE_TEXT + "No matching variant was found for this shape.",
                        commonGuiConfig.getText("controls.delete-pkm")
                )
                .build();
    }

    private Optional<Material> resolveDisplayMaterial(BlockType blockType) {
        try {
            Material material = BukkitAdapter.adapt(blockType);
            return (material != null && material.isItem()) ? Optional.of(material) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ========================================================================
    // FOOTER (ROW 6) & ACTIONS
    // ========================================================================

    private void drawApplyButton(OrionMenuHolder holder, Player player, RecolorGuiSession session) {
        ItemStack icon = new ItemBuilder(Material.ANVIL)
                .name(BTN_ACT + "Apply Replace")
                .lore(
                        LORE_TEXT + "Execute the configured pairs",
                        LORE_TEXT + "across the selected region."
                )
                .build();

        holder.addButton(SLOT_APPLY, new GuiMenuButton(icon, event -> handleApplyClick(player, session)));
    }

    private void handleApplyClick(Player player, RecolorGuiSession session) {
        Map<BlockType, BlockType> desiredTargets = presenter.collectDesiredTargets(session);
        if (desiredTargets.isEmpty()) {
            player.sendMessage(ERROR + "No recolor pairs configured. Set at least one block above.");
            return;
        }

        Optional<RecolorSelection> currentSelection = selectionResolver.resolve(player);
        if (currentSelection.isEmpty()) {
            player.sendMessage(ERROR + "Make a region selection first.");
            return;
        }

        RecolorSelection selection = currentSelection.get();
        if (!isSessionMatchingSelection(session, selection)) {
            player.sendMessage(ERROR + "Your selection changed since this menu was opened. Reopen //recolor to refresh the pairs.");
            return;
        }

        applyService.apply(player, selection.region(), desiredTargets);
    }

    private boolean isSessionMatchingSelection(RecolorGuiSession session, RecolorSelection selection) {
        BlockVector3 min = selection.region().getMinimumPoint();
        BlockVector3 max = selection.region().getMaximumPoint();
        return session.matchesRegion(selection.world(), min, max);
    }

    private void drawPagination(OrionMenuHolder holder, Player player, RecolorGuiSession session) {
        int totalPages = presenter.getTotalPages(session, PAGE_SIZE);
        int page = session.getCurrentPage();
        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;

        WidgetPageNavigation.draw(holder, commonGuiConfig, page, hasPrev, hasNext, newPage -> {
            session.setCurrentPage(newPage);
            rerenderPreservingCursor(player, session);
        });
    }

    // ========================================================================
    // HELPERS & VIEW STATE BUILDERS
    // ========================================================================

    private void fillSelectionRequiredBackground(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, SELECTION_REQUIRED_FILLER);
        }
    }

    private ItemStack buildSelectionRequiredWarningIcon() {
        return new ItemBuilder(Material.BARRIER)
                .name(ERROR + "No region selected")
                .lore(
                        LORE_TEXT + "Use //pos1 and //pos2, or a wand,",
                        LORE_TEXT + "to select a region first."
                )
                .build();
    }

    private void rerenderPreservingCursor(Player player, RecolorGuiSession session) {
        ItemStack cursor = player.getItemOnCursor();
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;

        if (hasCursor) {
            player.setItemOnCursor(null);
        }

        render(player, session);

        if (hasCursor) {
            ItemStack clone = cursor.clone();
            taskRunner.runSync(() -> player.setItemOnCursor(clone));
        }
    }

    private int clampPage(int rawPage, int totalPages) {
        if (totalPages <= 0) return 0;
        if (rawPage >= totalPages) return totalPages - 1;
        return Math.max(rawPage, 0);
    }

    private String prettyName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return result.toString().trim();
    }
}