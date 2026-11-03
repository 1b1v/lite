package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.fastasyncworldedit.core.command.tool.brush.FlattenBrush;
import com.fastasyncworldedit.core.math.heightmap.ScalableHeightMap;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class CliffBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public CliffBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_HEIGHT, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        int minY = player.getWorld().getMinY();
        int maxY = player.getWorld().getMaxY();

        return new FlattenBrush(null, 0, 1, false, true, null, ScalableHeightMap.Shape.CYLINDER, minY, maxY);
    }
}