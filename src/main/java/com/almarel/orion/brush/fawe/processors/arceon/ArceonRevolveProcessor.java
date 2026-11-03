package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonRevolveProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.RevolveBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            int.class, int.class, int.class,
            boolean.class, boolean.class, boolean.class,
            double.class, Player.class
    };

    @Inject
    public ArceonRevolveProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_REVOLVE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        Object[] args = buildConstructorArgs(player, result);
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    private Object[] buildConstructorArgs(Player player, ParsingResult result) {
        int amount = result.getIntArg(0, 5);
        int start = result.getIntArg(1, 0);
        int end = result.getIntArg(2, 360);
        double heightDiff = resolveHeightDiff(result);

        return new Object[]{
                amount,
                start,
                end,
                result.hasFlag("-p"),
                result.hasFlag("-s"),
                result.hasFlag("-r"),
                heightDiff,
                player
        };
    }

    private double resolveHeightDiff(ParsingResult result) {
        return parserService.parseRadius(result.getArg(3), 0);
    }

    @Override
    protected void applySettings(BrushTool tool, ParsingResult result) {
        tool.setSize(1.0);
    }
}