package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.capabilities.Maskable;
import com.almarel.orion.brush.data.capabilities.Rangeable;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class FaweStackerToolState extends FaweToolState implements Maskable, Rangeable {

    private int range = 10;
    private String mask = "!#existing";

    public FaweStackerToolState(Material tool, String mainCommandLabel, String toolType, String[] args) {
        super(tool, mainCommandLabel, toolType, args);
        if (args != null) {
            if (args.length > 0) {
                try { this.range = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
            }
            if (args.length > 1) this.mask = args[1];
        }
    }

    @Override
    public String getFullCommandString() {
        FaweCommandBuilder builder = new FaweCommandBuilder(getMainCommandLabel())
                .withAlias(getToolType())
                .withArg(range);
        if (mask != null && !mask.isEmpty()) {
            builder.withArg(mask);
        }
        return builder.build();
    }
}