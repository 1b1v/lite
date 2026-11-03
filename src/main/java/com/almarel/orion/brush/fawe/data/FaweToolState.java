package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.BaseBrushState;
import com.almarel.orion.brush.data.presentation.Headerable;
import com.almarel.orion.brush.data.presentation.TitledBrush;
import com.almarel.orion.brush.data.enums.BrushProvider;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class FaweToolState extends BaseBrushState implements TitledBrush, Headerable {

    private BrushProvider provider = BrushProvider.FAWE;
    private String toolType = "unknown";
    private String[] commandArgs;
    private String mainCommandLabel;

    // Sync-бухгалтерия: что было реально применено к живому FAWE-объекту в прошлый раз.
    // Не часть пользовательских настроек — поэтому transient и не экспонируется через Lombok-геттер/сеттер.
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private transient String lastAppliedCommandString;

    public FaweToolState(Material tool, String mainCommandLabel, String toolType, String[] args) {
        super(tool);
        this.mainCommandLabel = mainCommandLabel;
        this.toolType = toolType;
        this.commandArgs = args;
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    public String getFullCommandString() {
        return new FaweCommandBuilder(mainCommandLabel)
                .withAlias(toolType)
                .withArgs(commandArgs)
                .build();
    }

    @Override
    public boolean needsRecreationOnDesync() {
        String current = getFullCommandString();
        boolean desynced = !java.util.Objects.equals(current, lastAppliedCommandString);
        if (desynced) {
            lastAppliedCommandString = current;
        }
        return desynced;
    }

    @Override
    public OrionBrushType getBrushType() {
        return OrionBrushType.FAWE;
    }

    @Override
    public String getGuiIdentifier() {
        if (toolType == null || toolType.equalsIgnoreCase("unknown")) return "FAWE Tool";
        return BrushFormatter.capitalize(toolType);
    }

    @Override
    public String getMenuTitle(ConfigurationGuiStore configStore) {
        return configStore.getCommon().tabs().getTitleSettings();
    }

    @Override
    public String getDisplayHeader() {
        String toolName = (getToolType() != null && !getToolType().equals("unknown"))
                ? BrushFormatter.capitalize(getToolType())
                : "Unknown";
        return org.bukkit.ChatColor.YELLOW.toString() + org.bukkit.ChatColor.BOLD + "Tool " + toolName;
    }
}