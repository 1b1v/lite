package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.fastasyncworldedit.core.command.tool.brush.SurfaceSphereBrush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class SurfaceBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public SurfaceBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_SURFACE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        return new SurfaceSphereBrush();
    }
}