package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class GravityBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public GravityBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_GRAVITY, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        boolean fromMaxY = result.hasFlag("-h");
        return new GravityBrush(fromMaxY);
    }
}