package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class SphereBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public SphereBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_SPHERE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        if (result.hasFlag("-h")) {
            return new HollowSphereBrush();
        } else {
            return new SphereBrush();
        }
    }
}