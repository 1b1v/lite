package com.almarel.orion.brush.procedural.generator;

import com.google.inject.Inject;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.almarel.orion.brush.custom.plump.data.PlumpBrushState;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.math.shapes.PlumpShape;
import com.almarel.orion.math.ProceduralTransform;
import com.almarel.orion.brush.procedural.GeometryPipeline;
import org.bukkit.entity.Player;

import java.util.List;

public class PlumpGenerator {

    private final GeometryPipeline renderer;
    private final FaweParserService parserService;

    @Inject
    public PlumpGenerator(GeometryPipeline renderer,
                          FaweParserService parserService) {
        this.renderer = renderer;
        this.parserService = parserService;
    }

    public void generateDirect(EditSession session, BlockVector3 target, Player player, PlumpBrushState st, Pattern overridePattern) {
        double radius = calculateClampedRadius(st.size);

        List<ProceduralTransform> elements = renderer.buildElements(
                player, st.constructMode, st.getActiveGroup(), st.size,
                st.scaleX, st.scaleY, st.scaleZ, st, true, st.randomSeed
        );

        PlumpShape shape = new PlumpShape(Math.max(1.0, radius) + 1.5, st.grain, st.hollow);
        Pattern finalPattern = resolveEffectivePattern(player, st.pattern, overridePattern);

        renderer.renderDirect(
                session, target, player, shape, elements, radius, finalPattern,
                false, // isHollow жестко false, так как PlumpShape сам обрабатывает hollow математически (Narrow Band)
                (st.getRotationMode() == com.almarel.orion.brush.data.enums.BrushRotationMode.ANGLE),
                st.tool, st.getRotationMatrix(), true, st.anchorMode, st.anchorOffset,
                st.smearDepth
        );
    }

    private double calculateClampedRadius(double size) {
        int maxRadius = WorldEdit.getInstance().getConfiguration().maxBrushRadius;
        return Math.min(size, maxRadius);
    }

    private Pattern resolveEffectivePattern(Player player, String patternInput, Pattern overridePattern) {
        if (overridePattern != null) {
            return overridePattern;
        }
        Pattern parsed = parserService.parsePattern(player, patternInput);
        return parsed != null ? parsed : com.sk89q.worldedit.world.block.BlockTypes.STONE.getDefaultState();
    }
}