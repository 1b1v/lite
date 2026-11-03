package com.almarel.orion.brush.custom.plump.data;

import com.almarel.orion.brush.data.BaseBrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.BrushRotationMode;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.data.enums.ScrollAction;
import com.almarel.orion.brush.data.presentation.TitledBrush;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class PlumpBrushState extends BaseBrushState implements ScrollActionable, Rotatable, AxisScalable, TitledBrush, Sizeable,
        Hollowable, Anchorable, Patternable, Maskable, Rangeable, Constructable, Groupable, Smearable, RotationModeable,
        ConstructTransformable {

    // --- Restored Base Fields ---
    public String pattern;
    public String mask;
    public int range = -1;
    public boolean constructMode = false;
    public String rawActiveGroup = null;
    public Set<String> selectedGroups = new HashSet<>();

    // --- Plump Specific ---
    public double size = 10.0;
    public boolean hollow = false;
    public BrushRotationMode rotationMode = BrushRotationMode.ANGLE;
    public double grain = 1.0;
    public boolean randomSeed = false;
    public int smearDepth = 0;
    public int smearHeight = 1;
    public int smearHeightPercent = 0;
    public String anchorMode = "center";
    public int anchorOffset = 0;

    // --- Construct Transform Data (Flyweight Implementation) ---
    public boolean constructScaleByRadius = false;
    public double constructScaleMultiplier = 1.0;
    public double constructScaleX = 1.0;
    public double constructScaleY = 1.0;
    public double constructScaleZ = 1.0;
    public double[] constructRotationMatrix = new double[]{1,0,0, 0,1,0, 0,0,1};

    // --- Scroll & Transform Fields ---
    public double scaleX = 1.0, scaleY = 1.0, scaleZ = 1.0;
    public double[] rotationMatrix = new double[]{1,0,0, 0,1,0, 0,0,1};
    public ScrollAction currentScrollAction = ScrollAction.SIZE;
    public Set<ScrollAction> activeScrollActions = EnumSet.noneOf(ScrollAction.class);

    public PlumpBrushState() {
        super();
        init();
    }

    public PlumpBrushState(Material tool) {
        super(tool);
        init();
    }

    private void init() {
        this.pattern = "stone";
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    @Override
    public double updateAxisScale(String axis, double delta) {
        switch (axis) {
            case "X" -> { scaleX = Math.max(0.1, Math.min(scaleX + delta, AxisScalable.PROCEDURAL_LIMIT)); return scaleX; }
            case "Y" -> { scaleY = Math.max(0.1, Math.min(scaleY + delta, AxisScalable.PROCEDURAL_LIMIT)); return scaleY; }
            default -> { scaleZ = Math.max(0.1, Math.min(scaleZ + delta, AxisScalable.PROCEDURAL_LIMIT)); return scaleZ; }
        }
    }

    @Override
    public OrionBrushType getBrushType() {
        return OrionBrushType.PLUMP;
    }

    @Override
    public List<String> getAvailableAnchorModes() {
        return List.of("center", "surface");
    }

    @Override
    public void setRotationMode(BrushRotationMode mode) {
        this.rotationMode = mode != null ? mode : BrushRotationMode.OFF;
    }

    @Override
    public Set<BrushRotationMode> getAvailableRotationModes() {
        return EnumSet.of(BrushRotationMode.OFF, BrushRotationMode.ANGLE);
    }

    @Override
    public String getGuiIdentifier() {
        return "Plump";
    }

    @Override
    public String getMenuTitle(ConfigurationGuiStore configStore) {
        return "Brush " + getGuiIdentifier() + configStore.getBrushGui().getSuffixHand();
    }
}