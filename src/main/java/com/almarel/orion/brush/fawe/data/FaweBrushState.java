package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.BaseBrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.BrushProvider;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.data.enums.ScrollAction;
import com.almarel.orion.brush.data.presentation.TitledBrush;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class FaweBrushState extends BaseBrushState implements TitledBrush, Sizeable, Patternable, Maskable, SourceMaskable,
        Rangeable, ScrollActionable {

    public BrushProvider provider = BrushProvider.FAWE;
    public String[] commandArgs;
    public String mainCommandLabel;

    // --- Async Thread-Safety ---
    public transient float lastYaw = 0.0f;
    public transient float lastPitch = 0.0f;

    public String faweBrushName = "unknown";
    public double size = 5;
    public String pattern;
    public String mask;
    public String sourceMask;
    public int range = -1;
    public Map<String, Object> flags = new HashMap<>();

    // --- Scroll Fields ---
    public ScrollAction currentScrollAction = ScrollAction.SIZE;
    public Set<ScrollAction> activeScrollActions = EnumSet.noneOf(ScrollAction.class);

    public FaweBrushState(Material tool, String mainCommandLabel, String[] args) {
        super(tool);
        this.mainCommandLabel = mainCommandLabel;
        this.commandArgs = args;
        this.size = 5;
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    public void clearCommandArgs() {
        this.commandArgs = null;
    }

    public void addFlag(String key, Object value) {
        this.flags.put(key, value);
    }

    public String getFullCommandString() {
        return new FaweCommandBuilder(mainCommandLabel)
                .withAlias(faweBrushName != null ? faweBrushName : "sphere")
                .withArgs(commandArgs)
                .build();
    }

    @Override
    public OrionBrushType getBrushType() {
        return OrionBrushType.FAWE;
    }

    @Override
    public String getMenuTitle(ConfigurationGuiStore configStore) {
        return configStore.getCommon().tabs().getTitleSettings();
    }

    public boolean isStateDesynced() {
        if (commandArgs == null || commandArgs.length == 0) return true;

        String newCommand = getFullCommandString();
        String oldCommand = new FaweCommandBuilder(mainCommandLabel)
                .withAlias(faweBrushName)
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
    public String getGuiIdentifier() {
        if (faweBrushName == null || faweBrushName.equalsIgnoreCase("unknown")) return "FAWE";
        return BrushFormatter.capitalize(faweBrushName);
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