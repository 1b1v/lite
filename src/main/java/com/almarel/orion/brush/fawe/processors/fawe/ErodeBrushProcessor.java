package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.fastasyncworldedit.core.command.tool.brush.ErodeBrush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class ErodeBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public ErodeBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_ERODE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        int erodeFaces = result.getIntArg(1, 2);
        int erodeRec = result.getIntArg(2, 1);
        int fillFaces = result.getIntArg(3, 5);
        int fillRec = result.getIntArg(4, 1);

        return new ErodeBrush(erodeFaces, erodeRec, fillFaces, fillRec);
    }
}