package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.fastasyncworldedit.core.command.tool.brush.BlendBall;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class BlendBallBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public BlendBallBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_BLENDBALL, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        int minFreqDiff = result.getIntArg(1, 1);
        boolean onlyAir = result.hasFlag("-a");

        return new BlendBall(minFreqDiff, onlyAir, null);
    }
}