package com.almarel.orion.brush.procedural;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.almarel.orion.brush.data.capabilities.ConstructTransformable;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.math.ProceduralTransform;
import com.almarel.orion.math.SDF;
import com.almarel.orion.math.TransformMathUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Фасад для координации процедурного рендеринга.
 * Делегирует сборку элементов Builder'у, а вычисления Processor'у.
 */
@Singleton
public class GeometryPipeline {

    private static final double MAX_SAFE_RENDER_RADIUS = 150.0;

    private final ProceduralElementBuilder elementBuilder;
    private final AsyncExecutor asyncExecutor;

    @Inject
    public GeometryPipeline(
            ProceduralElementBuilder elementBuilder,
            AsyncExecutor asyncExecutor
    ) {
        this.elementBuilder = elementBuilder;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Метод-делегат, сохраняющий обратную совместимость с генераторами кистей.
     */
    public List<ProceduralTransform> buildElements(Player player, boolean constructMode, String activeGroup,
                                                   double radius, double scaleX, double scaleY, double scaleZ,
                                                   ConstructTransformable transformable,
                                                   boolean multiplyByGlobalScale, boolean useRandomSeed) {
        return elementBuilder.buildElements(player, constructMode, activeGroup, radius, scaleX, scaleY, scaleZ, transformable, multiplyByGlobalScale, useRandomSeed);
    }

    public void renderDirect(EditSession session, BlockVector3 target, Player player, SDF shapeStrategy,
                             List<ProceduralTransform> elements, double radius, Pattern finalPattern,
                             boolean isHollow, boolean angleMode, Material tool, double[] rotMatrix,
                             boolean swapDefaultYZ, String anchorMode, int anchorOffset, int smearDepth) {
        try {
            if (radius > MAX_SAFE_RENDER_RADIUS) return;

            TransformMathUtils.TransformContext ctx = TransformMathUtils.calculateRotation(
                    player.getLocation().getYaw(), player.getLocation().getPitch(), angleMode, rotMatrix
            );

            ProceduralProcessor processor = executeGeometryProcessing(
                    elements, ctx, shapeStrategy, radius, isHollow, angleMode, swapDefaultYZ
            );

            BlockVector3 anchor = resolveAnchorOffset(player, processor, anchorMode, anchorOffset, radius);
            BlockVector3 finalOrigin = target.add(anchor);

            // Мгновенная вставка маски в мир
            processor.flushToSession(session, finalPattern, finalOrigin, smearDepth);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ProceduralProcessor executeGeometryProcessing(List<ProceduralTransform> elements,
                                                          TransformMathUtils.TransformContext ctx,
                                                          SDF shapeStrategy, double radius,
                                                          boolean isHollow, boolean angleMode,
                                                          boolean swapDefaultYZ) {
        CuboidRegion region = ProceduralProcessor.calculateBounds(elements, ctx, radius, angleMode, swapDefaultYZ);
        ProceduralProcessor processor = new ProceduralProcessor(
                region, elements, ctx, shapeStrategy, angleMode, swapDefaultYZ, asyncExecutor
        );

        processor.computeGeometry();
        if (isHollow) {
            processor.applyHollow();
        }
        return processor;
    }

    private BlockVector3 resolveAnchorOffset(Player player, ProceduralProcessor processor,
                                             String anchorMode, int anchorOffset, double radius) {
        if (!"surface".equalsIgnoreCase(anchorMode)) {
            return BlockVector3.ZERO;
        }
        Vector direction = player.getLocation().getDirection().normalize();
        return processor.calculateSurfaceAnchor(direction, anchorOffset, radius);
    }
}