package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.Vector2;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonVineProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.VineBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            Pattern.class,
            int.class,
            Vector2.class
    };

    @Inject
    public ArceonVineProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_VINE, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        Object[] args = buildConstructorArgs(result);
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    private Object[] buildConstructorArgs(ParsingResult result) {
        Pattern pattern = result.pattern;
        int chance = result.getIntArg(2, 25);
        Vector2 vineSize = parserService.parseVector2(result.getArg(3), 5, 5);

        return new Object[]{
                pattern,
                chance,
                vineSize
        };
    }
}