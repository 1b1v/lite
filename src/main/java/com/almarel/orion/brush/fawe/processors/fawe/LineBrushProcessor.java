package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.fastasyncworldedit.core.command.tool.brush.LineBrush;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class LineBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public LineBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_LINE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        boolean shell = result.hasFlag("-h");
        boolean select = result.hasFlag("-s");
        boolean flat = result.hasFlag("-f");

        return new LineBrush(shell, select, flat);
    }
}