package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.fastasyncworldedit.core.command.tool.brush.SplineBrush;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class SplineBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public SplineBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_SPLINE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        return new SplineBrush(player);
    }
}