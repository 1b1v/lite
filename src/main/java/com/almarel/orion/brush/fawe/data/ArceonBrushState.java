package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.BaseBrushState;
import com.almarel.orion.brush.data.capabilities.Maskable;
import com.almarel.orion.brush.data.capabilities.Patternable;
import com.almarel.orion.brush.data.capabilities.Rangeable;
import com.almarel.orion.brush.data.presentation.TitledBrush;
import com.almarel.orion.brush.data.enums.BrushProvider;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;
import com.almarel.orion.brush.data.capabilities.ScrollActionable;
import com.almarel.orion.brush.data.enums.ScrollAction;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
@Setter
@NoArgsConstructor
public class ArceonBrushState extends BaseBrushState implements TitledBrush, Maskable, Patternable, Rangeable,
        ScrollActionable {

    public BrushProvider provider = BrushProvider.ARCEON;
    public String[] commandArgs;
    public String mainCommandLabel;

    public transient float lastYaw = 0.0f;
    public transient float lastPitch = 0.0f;

    public String arceonBrushName = "unknown";
    public String mask;
    public String pattern;
    public int range = -1;

    // --- Scroll Fields ---
    public ScrollAction currentScrollAction = ScrollAction.SIZE;
    public Set<ScrollAction> activeScrollActions = EnumSet.noneOf(ScrollAction.class);

    public ArceonBrushState(Material tool, String mainCommandLabel, String arceonBrushName, String[] args) {
        super(tool);
        this.mainCommandLabel = mainCommandLabel;
        this.arceonBrushName = arceonBrushName;
        this.commandArgs = args;
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    public void clearCommandArgs() {
        this.commandArgs = null;
    }

    public String getFullCommandString() {
        return new FaweCommandBuilder(mainCommandLabel)
                .withAlias(arceonBrushName)
                .withArgs(commandArgs)
                .build();
    }

    @Override
    public OrionBrushType getBrushType() {
        return OrionBrushType.FAWE;
    }

    @Override
    public String getGuiIdentifier() {
        if (arceonBrushName == null || arceonBrushName.equalsIgnoreCase("unknown")) return "Arceon";
        return BrushFormatter.capitalize(arceonBrushName);
    }

    @Override
    public String getMenuTitle(ConfigurationGuiStore configStore) {
        return configStore.getCommon().tabs().getTitleSettings();
    }

    public boolean isStateDesynced() {
        if (commandArgs == null || commandArgs.length == 0) return true;

        String newCommand = getFullCommandString();
        String oldCommand = new FaweCommandBuilder(mainCommandLabel)
                .withAlias(arceonBrushName)
                .withArgs(commandArgs)
                .build();

        return !newCommand.equalsIgnoreCase(oldCommand);
    }

    public void markAsSynced() {
        String[] parts = getFullCommandString().split("\\s+");
        if (parts.length > 2) {
            this.commandArgs = java.util.Arrays.copyOfRange(parts, 2, parts.length);
        }
    }

    @Override
    public void cachePlayerOrientation(Player player) {
        Location loc = player.getLocation();
        this.lastYaw = loc.getYaw();
        this.lastPitch = loc.getPitch();
    }

    @Override
    public boolean needsRecreationOnDesync() {
        if (isStateDesynced()) {
            markAsSynced();
            return true;
        }
        return false;
    }
}