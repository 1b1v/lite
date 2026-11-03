package com.almarel.orion.brush.fawe.processors.arceon;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.fawe.builtins.ReflectionUtils;
import com.almarel.orion.framework.data.OrionPermissions;

public class ArceonCropProcessor extends AbstractBrushProcessor {

    private static final String CLASS_NAME = "com.arceon.core.brush.CropBrush";

    private static final Class<?>[] PARAM_TYPES = new Class<?>[]{
            int.class,
            int.class
    };

    @Inject
    public ArceonCropProcessor(FaweParserService parserService) {
        super(OrionPermissions.ARCEON_CROP, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        int min = result.getIntArg(1, 0);
        int max = result.getIntArg(2, 7);

        Object[] args = new Object[]{ min, max };
        return ReflectionUtils.createBrushInstance(CLASS_NAME, PARAM_TYPES, args);
    }

    @Override
    protected void applySettings(BrushTool tool, ParsingResult result) {
        if (!result.args.isEmpty()) {
            double size = parserService.parseRadius(result.getArg(0), 5);
            tool.setSize(size);
        }
    }
}