package com.almarel.orion.brush.procedural.generator;

import com.google.inject.Inject;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.brush.custom.crystal.data.CrystalBrushState;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.math.shapes.CrystalShape;
import com.almarel.orion.math.ProceduralTransform;
import com.almarel.orion.brush.procedural.GeometryPipeline;
import org.bukkit.entity.Player;

import java.util.List;

public class CrystalGenerator {

    private final GeometryPipeline renderer;
    private final FaweParserService parserService;

    @Inject
    public CrystalGenerator(GeometryPipeline renderer,
                            FaweParserService parserService) {
        this.renderer = renderer;
        this.parserService = parserService;
    }

    public void generateDirect(EditSession session, BlockVector3 target, Player player, CrystalBrushState st, Pattern overridePattern) {
        double baseRadius = calculateBaseRadius(st.size);

        List<ProceduralTransform> elements = renderer.buildElements(
                player, st.constructMode, st.getActiveGroup(), st.size,
                st.scaleX, st.scaleY, st.scaleZ,
                st, false, false
        );

        CrystalShape shape = new CrystalShape(baseRadius);
        Pattern finalPattern = resolveEffectivePattern(player, st.pattern, overridePattern);

        renderer.renderDirect(
                session, target, player, shape, elements, baseRadius, finalPattern,
                st.hollow, (st.getRotationMode() == com.almarel.orion.brush.data.enums.BrushRotationMode.ANGLE),
                st.tool, st.getRotationMatrix(), false,
                st.anchorMode, st.anchorOffset, st.smearDepth
        );
    }

    private double calculateBaseRadius(double size) {
        int maxRadius = WorldEdit.getInstance().getConfiguration().maxBrushRadius;
        return Math.min(size, maxRadius) + 4.2;
    }

    private Pattern resolveEffectivePattern(Player player, String patternInput, Pattern overridePattern) {
        if (overridePattern != null) {
            return overridePattern;
        }
        Pattern parsed = parserService.parsePattern(player, patternInput);
        return parsed != null ? parsed : com.sk89q.worldedit.world.block.BlockTypes.STONE.getDefaultState();
    }
}