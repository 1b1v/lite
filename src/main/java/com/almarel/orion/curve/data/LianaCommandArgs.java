package com.almarel.orion.curve.data;

import com.sk89q.worldedit.function.pattern.Pattern;

public record LianaCommandArgs(
        Pattern pattern,
        Pattern stemPattern,
        Pattern tipPattern,
        double sag,
        int stemLength,
        int lianaCount
) {}