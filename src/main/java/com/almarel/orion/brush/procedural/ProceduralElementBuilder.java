package com.almarel.orion.brush.procedural;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.almarel.orion.brush.data.capabilities.ConstructTransformable;
import com.almarel.orion.math.FastNoise;
import com.almarel.orion.math.ProceduralTransform;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Отвечает за трансляцию DTO игрока и библиотек в чистые математические трансформации.
 */
@Singleton
public class ProceduralElementBuilder {


    public List<ProceduralTransform> buildElements(Player player, boolean constructMode, String activeGroup,
                                                   double radius, double scaleX, double scaleY, double scaleZ,
                                                   ConstructTransformable transformable,
                                                   boolean multiplyByGlobalScale, boolean useRandomSeed) {
        List<ProceduralTransform> elements = new ArrayList<>();
        FastNoise sharedNoise = new FastNoise(ThreadLocalRandom.current().nextInt());

        {
            elements.add(createDefaultTransform(scaleX, scaleY, scaleZ, sharedNoise));
        }
        return elements;
    }

    private ProceduralTransform createDefaultTransform(double scaleX, double scaleY, double scaleZ, FastNoise noise) {
        double[] defaultMatrix = new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
        return new ProceduralTransform(0, 0, 0, scaleX, scaleY, scaleZ, defaultMatrix, noise);
    }

}