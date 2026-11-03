package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.capabilities.Patternable;
import com.almarel.orion.brush.data.capabilities.Rangeable;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class FaweFloodToolState extends FaweToolState implements Patternable, Rangeable {

    private String pattern = "stone";
    private int range = 5;

    public FaweFloodToolState(Material tool, String mainCommandLabel, String toolType, String[] args) {
        super(tool, mainCommandLabel, toolType, args);
        if (args != null) {
            if (args.length > 0) this.pattern = args[0];
            if (args.length > 1) {
                try { this.range = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
            }
        }
    }

    @Override
    public String getFullCommandString() {
        return new FaweCommandBuilder(getMainCommandLabel())
                .withAlias(getToolType())
                .withArg(pattern)
                .withArg(range)
                .build();
    }
}