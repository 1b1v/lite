package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.entity.Player;
import com.fastasyncworldedit.core.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class ExtinguishBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public ExtinguishBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_EXTINGUISH, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        return new SphereBrush();
    }

    @Override
    protected void applySettings(BrushTool tool, ParsingResult result) {
        if (result.args.size() > 0) {
            double r = parserService.parseRadius(result.args.get(0), 5);
            tool.setSize(r);
        }

        tool.setFill(BlockTypes.AIR.getDefaultState());

        try {
            tool.setMask(new SingleBlockTypeMask(result.player.getWorld(), BlockTypes.FIRE));
        } catch (Exception e) {
            // Ignored
        }
    }
}