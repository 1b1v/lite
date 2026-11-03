package com.almarel.orion.brush.presentation.gui.settings;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.BrushRotationMode;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.brush.services.BrushRotationService;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UniversalBrushController {

    private final BrushState state;
    private final Player player;
    private final FaweIntegrationService faweService;
    private final GuiMaterialListMenu maskMenu;
    private final CommonGuiConfig commonGuiConfig;
    private final MessageManager messageManager;
    private final Runnable refreshCallback;
    private final BrushRotationService brushRotationService;

    private static final int LIMIT_STANDARD = 54;
    private static final int LIMIT_IGNORE = 24;

    public UniversalBrushController(BrushState state, Player player,
                                    FaweIntegrationService faweService,
                                    GuiMaterialListMenu maskMenu,
                                    CommonGuiConfig commonGuiConfig,
                                    MessageManager messageManager,
                                    Runnable refreshCallback,
                                    BrushRotationService brushRotationService) {
        this.state = state;
        this.player = player;
        this.faweService = faweService;
        this.maskMenu = maskMenu;
        this.commonGuiConfig = commonGuiConfig;
        this.messageManager = messageManager;
        this.refreshCallback = refreshCallback;
        this.brushRotationService = brushRotationService;
    }

    /**
     * Создает заголовок меню в стиле "Brush NAME Editor".
     */
    private String buildBreadcrumbTitle(String editorName) {
        String brushName = state.getGuiIdentifier();
        // [FIX] Возвращаем слово Brush для консистентности (убираем стрелочку для экономии места)
        return "§9Brush §a" + brushName + " §e" + editorName;
    }

    private boolean isStringAtLimit(String current, int limit) {
        if (current == null || current.isEmpty()) return false;
        return current.split(",").length >= limit;
    }

    // --- DRY ШАБЛОН ДЛЯ СТРОКОВЫХ СПИСКОВ (Pattern, Mask, SourceMask) ---

    private void handleStringListAction(org.bukkit.event.inventory.InventoryClickEvent event, String current, int limit, String editorTitle, Consumer<String> setter) {
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR && cursor.getType().isBlock()) {
            String newMat = cursor.getType().name().toLowerCase();
            if (current != null && current.contains(newMat)) return;

            if (isStringAtLimit(current, limit)) {
                messageManager.send(player, OrionMessage.GUI_LIMIT_REACHED);
                return;
            }

            String finalStr = (current == null || current.isEmpty()) ? newMat : current + "," + newMat;
            setter.accept(finalStr);
            return;
        }
        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
            // [FIX] Генерируем хлебные крошки перед открытием меню
            String fullTitle = buildBreadcrumbTitle(editorTitle);
            maskMenu.open(player, fullTitle, current, limit, null, setter, refreshCallback);
        }
    }

    // --- PATTERN ---
    public void onPatternAction(InventoryClickEvent event) {
        if (!(state instanceof Patternable p)) return;
        handleStringListAction(event, p.getPattern(), LIMIT_STANDARD, commonGuiConfig.editors().getPattern(), this::setPattern);
        if (event.isRightClick()) setPattern("stone");
    }

    private void setPattern(String pattern) {
        if (!(state instanceof Patternable p)) return;
        p.setPattern(pattern);
        if (!state.needsRecreationOnDesync()) {
            boolean success = faweService.setBrushPattern(player, state.getTool(), pattern);
            if (!success) messageManager.sendMessage(player, "gui.error.invalid-syntax");
        }
        refreshCallback.run();
    }

    // --- MASK ---
    public void onMaskAction(InventoryClickEvent event) {
        if (!(state instanceof Maskable m)) return;
        handleStringListAction(event, m.getMask(), LIMIT_STANDARD, commonGuiConfig.editors().getMask(), this::setMask);
        if (event.isRightClick()) setMask(null);
    }

    private void setMask(String mask) {
        if (!(state instanceof Maskable m)) return;
        m.setMask(mask);
        if (!state.needsRecreationOnDesync()) {
            boolean success = faweService.setBrushMask(player, state.getTool(), mask);
            if (!success) messageManager.sendMessage(player, "gui.error.invalid-syntax");
        }
        refreshCallback.run();
    }

    // --- SOURCE MASK ---
    public void onSourceMaskAction(InventoryClickEvent event) {
        if (!(state instanceof SourceMaskable sm)) return;
        handleStringListAction(event, sm.getSourceMask(), LIMIT_STANDARD, commonGuiConfig.editors().getSourceMask(), this::setSourceMask);
        if (event.isRightClick()) setSourceMask(null);
    }

    private void setSourceMask(String sourceMask) {
        if (!(state instanceof SourceMaskable sm)) return;
        sm.setSourceMask(sourceMask);
        if (!state.needsRecreationOnDesync()) {
            boolean success = faweService.setSourceMask(player, state.getTool(), sourceMask);
            if (!success) messageManager.sendMessage(player, "gui.error.invalid-syntax");
        }
        refreshCallback.run();
    }

    // --- NUMERIC VALUES ---
    public void onSizeChange(double val) { if (state instanceof Sizeable s) { s.setSize(val); faweService.setBrushSize(player, state.getTool(), (int) Math.round(val)); refreshCallback.run(); } }
    public void onGlobalScaleChange(double val) { if (state instanceof Scalable s) { s.setScale(val); refreshCallback.run(); } }
    public void onRangeChange(int val) {
        if (!(state instanceof Rangeable r)) return;
        r.setRange(val);
        if (!state.needsRecreationOnDesync()) {
            faweService.setBrushRange(player, state.getTool(), val);
        }
        refreshCallback.run();
    }
    public void onSmearChange(int val) { if (state instanceof Smearable s) { s.setSmearDepth(val); refreshCallback.run(); } }
    public void onSmoothChange(int val) { if (state instanceof Smoothable s) { s.setSmoothIterations(val); refreshCallback.run(); } }

    // --- HOLLOW & SEED ---
    public void onHollowToggle() { if (state instanceof Hollowable h) { h.setHollow(!h.isHollow()); refreshCallback.run(); } }
    public void onSeedToggle() { if (state instanceof Seedable s) { s.setRandomSeed(!s.isRandomSeed()); refreshCallback.run(); } }
    public void onGravityToggle() { if (state instanceof Gravityable g) { g.setGravity(!g.hasGravity()); refreshCallback.run(); } }

    // --- ROTATION & ANGLE ---
    public void onAngleToggle() { if (state instanceof AngleModeable a) { a.setAngleMode(!a.isAngleMode()); refreshCallback.run(); } }
    public void onRotationModeAction(InventoryClickEvent event) {
        if (!(state instanceof RotationModeable r)) return;

        java.util.Set<BrushRotationMode> available = r.getAvailableRotationModes();
        if (available == null || available.isEmpty()) return;

        if (event.isLeftClick()) {
            r.cycleNextRotationMode();
        } else if (event.isRightClick()) {
            int stepsBack = available.size() - 1;
            for (int i = 0; i < stepsBack; i++) {
                r.cycleNextRotationMode();
            }
        }
        refreshCallback.run();
    }

    // --- IGNORE LIST ---
    public void onIgnoreAddMaterial(Material mat) {
        if (state instanceof IgnoreListable i) {
            Set<Material> mats = i.getIgnoreMaterials();
            if (mats.size() >= LIMIT_IGNORE && !mats.contains(mat)) {
                messageManager.send(player, OrionMessage.GUI_LIMIT_REACHED);
                return;
            }
            mats.add(mat); refreshCallback.run();
        }
    }

    public void onIgnoreClear() { if (state instanceof IgnoreListable i) { i.getIgnoreMaterials().clear(); i.getIgnoreMaterials().add(Material.AIR); refreshCallback.run(); } }
    public void onIgnoreOpenEditor() {
        if (state instanceof IgnoreListable i) {
            java.util.Map<Integer, com.almarel.orion.framework.gui.common.GuiMenuButton> extras = new java.util.HashMap<>();

            if (!i.isUseTargetBlock()) {
                extras.put(40, com.almarel.orion.brush.presentation.gui.settings.widgets.WidgetFilterExtras.createWhitelistToggleButton(i, this::onIgnoreOpenEditor));
            }
            extras.put(41, com.almarel.orion.brush.presentation.gui.settings.widgets.WidgetFilterExtras.createTargetBlockMaskButton(i, this::onIgnoreOpenEditor));

            // [FIX] Используем "Filter" (или getMask(), если в конфиге нет getFilter()) и добавляем крошки
            String filterTitle = buildBreadcrumbTitle("Filter");

            maskMenu.open(player, filterTitle, i.getIgnoreMaterials(), LIMIT_IGNORE, extras, (newSet) -> {
                i.setIgnoreMaterials(newSet != null && !newSet.isEmpty() ? newSet : java.util.Set.of(Material.AIR));
                refreshCallback.run();
            }, refreshCallback);
        }
    }

    // --- ANCHOR ---
    public void onAnchorOffsetChange(int delta) { if (state instanceof Anchorable a) { a.setAnchorOffset(a.getAnchorOffset() + delta); refreshCallback.run(); } }
    public void onAnchorModeSwitch() {
        if (state instanceof Anchorable a) {
            List<String> availableModes = a.getAvailableAnchorModes();
            if (availableModes == null || availableModes.isEmpty()) return;

            String current = a.getAnchorMode();
            if (current == null) current = "none";

            int currentIndex = availableModes.indexOf(current);
            int nextIndex = currentIndex == -1 ? 0 : (currentIndex + 1) % availableModes.size();

            a.setAnchorMode(availableModes.get(nextIndex));
            refreshCallback.run();
        }
    }

    // --- AXIS TRANSFORMS ---
    public void onAxisScaleChange(String axis, double delta) {
        if (state instanceof AxisScalable a) {
            a.updateAxisScale(axis, delta);
            refreshCallback.run();
        }
    }

    public void onAxisSizeChange(String axis, double delta) {
        if (state instanceof AxisSizeable a) {
            a.updateAxisSize(axis, delta);
            refreshCallback.run();
        }
    }

    // --- AXIS TRANSFORMS ---
    public void onAxisRotationChange(String action, int delta) {
        if (state instanceof Rotatable r) {
            // [ИЗМЕНЕНО] Вся логика ушла в Сервис. Контроллер чист!
            brushRotationService.applyBrushRotation(player, r, action, delta);
            refreshCallback.run();
        }
    }
}