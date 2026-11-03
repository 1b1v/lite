package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector2;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonBoulderProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.BoulderBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            Vector2.class,
            int.class,
            boolean.class,
            boolean.class,
            boolean.class,
            Vector2.class,
            Vector2.class
    };

    @Inject
    public ArceonBoulderProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_BOULDER, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        Object[] args = buildConstructorArgs(result);
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    private Object[] buildConstructorArgs(ParsingResult result) {
        Vector2 size = parserService.parseVector2(result.getArg(1), 8, 8);
        int smoothness = result.getIntArg(2, 3);
        Vector2 height = parserService.parseVector2(result.getArg(3), 10000, 10000);
        Vector2 width = parserService.parseVector2(result.getArg(4), 10000, 10000);

        boolean hollow = result.hasFlag("-h");
        boolean falling = result.hasFlag("-f");
        boolean angled = result.hasFlag("-a");

        return new Object[]{
                size,
                smoothness,
                hollow,
                falling,
                angled,
                height,
                width
        };
    }
}