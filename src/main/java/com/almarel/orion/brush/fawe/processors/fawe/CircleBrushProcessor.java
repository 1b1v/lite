package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.fastasyncworldedit.core.command.tool.brush.CircleBrush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class CircleBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public CircleBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_SPHERE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        boolean filled = false;
        String filledArg = result.getArg(2);
        if (filledArg != null) {
            filled = Boolean.parseBoolean(filledArg);
        }

        if (result.hasFlag("-f")) filled = true;

        return new CircleBrush(filled);
    }
}