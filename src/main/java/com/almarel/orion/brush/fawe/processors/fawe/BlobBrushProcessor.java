package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.fastasyncworldedit.core.command.tool.brush.BlobBrush;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector3;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class BlobBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public BlobBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_ROCK, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        Vector3 radius = parserService.parseVector3(result.getArg(1), 10);

        double sphericity = parserService.parseRadius(result.getArg(2), 100) / 100.0;
        double frequency = parserService.parseRadius(result.getArg(3), 30) / 100.0;
        double amplitude = parserService.parseRadius(result.getArg(4), 50) / 100.0;

        double maxRadius = Math.max(radius.getX(), Math.max(radius.getY(), radius.getZ()));
        Vector3 normalizedRadius = radius.divide(maxRadius);

        return new BlobBrush(normalizedRadius, frequency, amplitude, sphericity);
    }

    @Override
    protected void applySettings(BrushTool tool, ParsingResult result) {
        super.applySettings(tool, result);

        Vector3 radius = parserService.parseVector3(result.getArg(1), 10);
        double maxRadius = Math.max(radius.getX(), Math.max(radius.getY(), radius.getZ()));

        tool.setSize(maxRadius);
    }
}