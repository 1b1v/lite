package com.almarel.orion.brush.presentation.gui.settings.providers;

import com.google.inject.Inject;
import com.sk89q.worldedit.WorldEdit;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.ScrollAction;
import com.almarel.orion.brush.presentation.gui.settings.UniversalBrushController;
import com.almarel.orion.brush.presentation.gui.settings.widgets.*;
import com.almarel.orion.brush.presentation.gui.settings.widgets.grid.*;
import com.almarel.orion.brush.presentation.gui.api.WidgetProvider;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StandardBrushWidgetProvider implements WidgetProvider {

    private final ConfigurationGuiStore configStore;

    @Inject
    public StandardBrushWidgetProvider(ConfigurationGuiStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public void inject(OrionMenuHolder holder, BrushState state, Player player, UniversalBrushController ubc,
                       DynamicGridBuilder grid, Runnable refreshCallback) {
        drawAxisWidgets(holder, state, ubc);
        drawRotationWidget(holder, state, player, ubc);
        drawAnchorWidget(holder, state, ubc);
        drawScrollCheckboxes(holder, state, refreshCallback);

        // [FIX] Передаем refreshCallback в метод
        injectStandardGrid(holder, state, ubc, grid, refreshCallback);
    }

    private void injectStandardGrid(OrionMenuHolder holder, BrushState state, UniversalBrushController ubc, DynamicGridBuilder grid, Runnable refreshCallback) {
        double maxRadius = WorldEdit.getInstance().getConfiguration().maxBrushRadius;

        grid.tryInject(14, Patternable.class, (slot, st) -> WidgetMaterial.drawPatternWidget(holder, slot, st, configStore.getCommon(), ubc::onPatternAction))
                .tryInject(15, Sizeable.class, (slot, st) -> WidgetTransform.drawSizeWidget(holder, slot, st, maxRadius, configStore.getCommon(), ubc::onSizeChange))
                .tryInject(15, Scalable.class, (slot, st) -> WidgetTransform.drawGlobalScaleWidget(holder, slot, st, configStore.getCommon(), ubc::onGlobalScaleChange))
                .tryInject(16, RotationModeable.class, (slot, st) -> WidgetTransform.drawRotationModeWidget(holder, slot, st, configStore.getCommon(), ubc::onRotationModeAction))
                .tryInject(16, AngleModeable.class, (slot, st) -> WidgetTransform.drawAngleToggleWidget(holder, slot, st, configStore.getCommon(), ubc::onAngleToggle))
                .tryInject(23, Maskable.class, (slot, st) -> WidgetMaterial.drawMaskWidget(holder, slot, st, configStore.getCommon(), ubc::onMaskAction))
                .tryInject(23, SourceMaskable.class, (slot, st) -> WidgetMaterial.drawSourceMaskWidget(holder, slot, st, configStore.getCommon(), ubc::onSourceMaskAction))
                .tryInject(24, Hollowable.class, (slot, st) -> WidgetModifiers.drawHollowWidget(holder, slot, st, configStore.getCommon(), ubc::onHollowToggle))

                .tryInject(25, Gravityable.class, (slot, st) -> WidgetModifiers.drawGravityWidget(holder, slot, st, configStore.getCommon(), ubc::onGravityToggle))
                .tryInject(24, Smoothable.class, (slot, st) -> WidgetModifiers.drawSmoothWidget(holder, slot, st, configStore.getCommon(), ubc::onSmoothChange))
                .tryInject(25, Smearable.class, (slot, st) -> WidgetModifiers.drawSmearWidget(holder, slot, st, configStore.getCommon(), ubc::onSmearChange))
                .tryInject(32, Rangeable.class, (slot, st) -> WidgetTransform.drawRangeWidget(holder, slot, st, configStore.getCommon(), ubc::onRangeChange))
                .tryInject(33, Seedable.class, (slot, st) -> WidgetModifiers.drawSeedWidget(holder, slot, st, configStore.getCommon(), ubc::onSeedToggle))
                .tryInject(33, IgnoreListable.class, (slot, st) -> {
                    boolean hasIgnore = st.getIgnoreMaterials() != null && !st.getIgnoreMaterials().isEmpty() && !(st.getIgnoreMaterials().size() == 1 && st.getIgnoreMaterials().contains(Material.AIR));

                    WidgetSelection.drawIgnoreListWidget(holder, slot, st, configStore.getCommon(),
                            hasIgnore || st.isIgnoreWhitelist(), buildIgnoreLore(st, hasIgnore),
                            ubc::onIgnoreAddMaterial, ubc::onIgnoreClear, ubc::onIgnoreOpenEditor);
                })
                // [FIXED] Используем WidgetSelection вместо WidgetModifiers
                .tryInject(34, FloodFillable.class, (slot, st) -> WidgetSelection.drawFloodFillWidget(holder, slot, st, configStore.getCommon(), () -> refreshCallback.run()));
    }

    private void drawAxisWidgets(OrionMenuHolder holder, BrushState state, UniversalBrushController ubc) {
        if (state instanceof AxisScalable axisScalable) {
            WidgetAxisControl.drawScaleAxisWidget(holder, axisScalable, configStore.getCommon(), ubc::onAxisScaleChange);
        }
        if (state instanceof AxisSizeable axisSizeable) {
            WidgetAxisControl.drawSizeAxisWidget(holder, axisSizeable, configStore.getCommon(), ubc::onAxisSizeChange);
        }
    }

    private void drawRotationWidget(OrionMenuHolder holder, BrushState state, org.bukkit.entity.Player player, UniversalBrushController ubc) {
        if (state instanceof Rotatable r) {
            WidgetRotationControl.draw(
                    holder,
                    configStore.getCommon(),
                    ubc::onAxisRotationChange
            );
        }
    }

    private void drawAnchorWidget(OrionMenuHolder holder, BrushState state, UniversalBrushController ubc) {
        if (state instanceof Anchorable anchorable) {
            String rawMode = anchorable.getAnchorMode();
            if (rawMode == null) rawMode = "none";

            String modeDisplayName = rawMode.equalsIgnoreCase("none") ? "default" : rawMode.toLowerCase(Locale.ROOT);
            boolean isActive = !"none".equalsIgnoreCase(rawMode);

            WidgetAnchor.draw(holder, configStore.getCommon(), modeDisplayName, isActive, anchorable.getAnchorOffset(), new WidgetAnchor.AnchorCallback() {
                @Override public void onOffsetChange(int delta) { ubc.onAnchorOffsetChange(delta); }
                @Override public void onModeSwitch() { ubc.onAnchorModeSwitch(); }
            });
        }
    }

    private void drawScrollCheckboxes(OrionMenuHolder holder, BrushState state, Runnable refreshCallback) {
        if (!(state instanceof ScrollActionable scrollState)) return;

        if (state instanceof Sizeable) {
            WidgetScroll.drawCheckbox(holder, 38, configStore.getCommon(), configStore.getCommon().scroll().getRadius(), ScrollAction.SIZE, scrollState.getActiveScrollActions(), (action) -> toggleScrollAction(scrollState, action, refreshCallback));
        } else if (state instanceof Scalable) {
            WidgetScroll.drawCheckbox(holder, 38, configStore.getCommon(), configStore.getCommon().scroll().getScale(), ScrollAction.SCALE, scrollState.getActiveScrollActions(), (action) -> toggleScrollAction(scrollState, action, refreshCallback));
        }

        if (state instanceof AxisScalable) {
            WidgetScroll.drawCheckbox(holder, 39, configStore.getCommon(), configStore.getCommon().scroll().getScaleAxis(), ScrollAction.SCALE_AXIS, scrollState.getActiveScrollActions(), (action) -> toggleScrollAction(scrollState, action, refreshCallback));
        } else if (state instanceof AxisSizeable) {
            WidgetScroll.drawCheckbox(holder, 39, configStore.getCommon(), configStore.getCommon().scroll().getScaleAxis(), ScrollAction.SIZE_AXIS, scrollState.getActiveScrollActions(), (action) -> toggleScrollAction(scrollState, action, refreshCallback));
        }

        if (state instanceof Rotatable) {
            WidgetScroll.drawCheckbox(holder, 40, configStore.getCommon(), configStore.getCommon().scroll().getRotation(), ScrollAction.ROTATION, scrollState.getActiveScrollActions(), (action) -> toggleScrollAction(scrollState, action, refreshCallback));
        }

        if (state instanceof Bufferable) {
            WidgetScroll.drawCheckbox(holder, 41, configStore.getCommon(), configStore.getCommon().scroll().getBufferSlot(), ScrollAction.BUFFER_SLOT, scrollState.getActiveScrollActions(), (action) -> toggleScrollAction(scrollState, action, refreshCallback));
        }
    }

    private List<String> buildIgnoreLore(IgnoreListable st, boolean hasIgnore) {
        List<String> lore = new ArrayList<>();
        lore.add(st.isIgnoreWhitelist() ? configStore.getCommon().ignore().getModeWhite() : configStore.getCommon().ignore().getModeBlack());
        lore.add(" ");
        if (hasIgnore) {
            lore.add("  " + configStore.getCommon().ignore().getListHeader());
            st.getIgnoreMaterials().stream().filter(m -> m != Material.AIR).limit(5)
                    .forEach(m -> lore.add("  " + configStore.getCommon().ignore().getItem(m.name().toLowerCase(java.util.Locale.ROOT))));
            if (st.getIgnoreMaterials().size() > 6) {
                lore.add("  " + configStore.getCommon().ignore().getMore(String.valueOf(st.getIgnoreMaterials().size() - 6)));
            }
        } else {
            lore.add("  " + configStore.getCommon().ignore().getEmpty());
        }
        // [FIX] Удалили старые строки configStore.getCommon().ignore().getAddHint() и т.д.
        return lore;
    }

    private void toggleScrollAction(ScrollActionable state, ScrollAction action, Runnable refresh) {
        if (state.getActiveScrollActions() != null) {
            if (state.getActiveScrollActions().contains(action)) {
                state.getActiveScrollActions().remove(action);
            } else {
                state.getActiveScrollActions().add(action);
            }
            refresh.run();
        }
    }
}