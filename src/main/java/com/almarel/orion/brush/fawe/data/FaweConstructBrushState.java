package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.capabilities.ConstructTransformable;
import com.almarel.orion.brush.data.capabilities.Constructable;
import com.almarel.orion.brush.data.capabilities.Groupable;
import com.almarel.orion.brush.data.presentation.Headerable;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class FaweConstructBrushState extends FaweBrushState implements Constructable, Groupable, ConstructTransformable, Headerable {

    public boolean constructMode = false;
    public String rawActiveGroup = null;
    public Set<String> selectedGroups = new HashSet<>();

    // --- Construct Transform Data (Flyweight Implementation) ---
    public boolean constructScaleByRadius = false;
    public double constructScaleMultiplier = 1.0;
    public double constructScaleX = 1.0;
    public double constructScaleY = 1.0;
    public double constructScaleZ = 1.0;
    public double[] constructRotationMatrix = new double[]{1,0,0, 0,1,0, 0,0,1};

    public FaweConstructBrushState(Material tool, String mainCommandLabel, String[] args) {
        super(tool, mainCommandLabel, args);
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    @Override
    public String toString() {
        return "FaweBrushState{faweBrushName='" + faweBrushName + "', tool=" + tool + ", size=" + size + "}";
    }

    @Override
    public String getDisplayHeader() {
        String brushName = BrushFormatter.resolveHeaderBrushName(getBrushType(), getFaweBrushName(), getCommandArgs());
        return org.bukkit.ChatColor.DARK_AQUA.toString() + org.bukkit.ChatColor.BOLD + "Brush " + brushName;
    }
}