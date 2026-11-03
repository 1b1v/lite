package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector2;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonCubeProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.CubeBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            Vector2.class,
            int.class,
            Vector2.class,
            Vector2.class,
            boolean.class,
            boolean.class,
            boolean.class,
            Player.class
    };

    @Inject
    public ArceonCubeProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_CUBE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        Object[] args = buildConstructorArgs(player, result);
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    private Object[] buildConstructorArgs(Player player, ParsingResult result) {
        Vector2 size = parserService.parseVector2(result.getArg(1), 8, 8);
        int snapAngle = result.getIntArg(2, 45);
        Vector2 height = parserService.parseVector2(result.getArg(3), 10000, 10000);
        Vector2 width = parserService.parseVector2(result.getArg(4), 10000, 10000);

        boolean hollow = result.hasFlag("-h");
        boolean angled = result.hasFlag("-a");
        boolean centered = result.hasFlag("-c");

        return new Object[]{
                size,
                snapAngle,
                height,
                width,
                hollow,
                angled,
                centered,
                player
        };
    }
}