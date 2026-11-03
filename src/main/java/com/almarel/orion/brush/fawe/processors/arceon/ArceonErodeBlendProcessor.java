package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonErodeBlendProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.ErodeBlendBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            int.class,
            int.class
    };

    @Inject
    public ArceonErodeBlendProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_ERODE_BLEND, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        String type = result.getArg(1);
        int[] erodeFill = resolveErodeFillParameters(type);

        Object[] args = new Object[]{ erodeFill[0], erodeFill[1] };
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    private int[] resolveErodeFillParameters(String type) {
        if (type == null) {
            type = "smooth";
        }

        switch (type.toLowerCase()) {
            case "fill":
                return new int[]{5, 2};
            case "lift":
                return new int[]{6, 1};
            case "clear":
                return new int[]{1, 6};
            case "smooth":
            default:
                return new int[]{3, 3};
        }
    }

    @Override
    protected void applySettings(BrushTool tool, ParsingResult result) {
        if (!result.args.isEmpty()) {
            double r = parserService.parseRadius(result.getArg(0), 5);
            tool.setSize(r);
        }
    }
}