package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.capabilities.Patternable;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class FaweReplToolState extends FaweToolState implements Patternable {

    private String pattern = "stone";

    public FaweReplToolState(Material tool, String mainCommandLabel, String toolType, String[] args) {
        super(tool, mainCommandLabel, toolType, args);
        if (args != null && args.length > 0) {
            this.pattern = args[0];
        }
    }

    @Override
    public String getFullCommandString() {
        return new FaweCommandBuilder(getMainCommandLabel())
                .withAlias(getToolType())
                .withArg(pattern)
                .build();
    }
}