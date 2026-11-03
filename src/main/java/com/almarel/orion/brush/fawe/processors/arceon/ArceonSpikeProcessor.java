package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonSpikeProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.SpikeBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            double.class,
            double.class,
            double.class,
            boolean.class
    };

    @Inject
    public ArceonSpikeProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_SPIKE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        Object[] args = buildConstructorArgs(result);
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    private Object[] buildConstructorArgs(ParsingResult result) {
        double length = parserService.parseRadius(result.getArg(1), 5);
        double sizeStart = parserService.parseRadius(result.getArg(2), 2);
        double sizeEnd = parserService.parseRadius(result.getArg(3), 0.5);
        boolean noise = result.hasFlag("-n");

        return new Object[]{
                length,
                sizeStart,
                sizeEnd,
                noise
        };
    }
}