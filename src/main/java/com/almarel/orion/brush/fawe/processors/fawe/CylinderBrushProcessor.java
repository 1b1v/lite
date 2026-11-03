package com.almarel.orion.brush.fawe.processors.fawe;

import com.google.inject.Inject;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.entity.Player;
import com.almarel.orion.brush.fawe.builtins.AbstractBrushProcessor;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.data.OrionPermissions;

public class CylinderBrushProcessor extends AbstractBrushProcessor {

    @Inject
    public CylinderBrushProcessor(FaweParserService parserService) {
        super(OrionPermissions.FAWE_BRUSH_CYLINDER, parserService);
    }

    @Override
    protected Brush createBrush(Player player, ParsingResult result) {
        int height = result.getIntArg(2, 1);

        if (result.hasFlag("-h")) {
            double thickness = 0.0;
            String thicknessArg = result.getArg(3);
            if (thicknessArg != null) {
                try {
                    thickness = Double.parseDouble(thicknessArg);
                } catch (NumberFormatException ignored) {}
            }
            return new HollowCylinderBrush(height, thickness);
        } else {
            return new CylinderBrush(height);
        }
    }
}