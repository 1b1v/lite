package com.almarel.orion.generation.onion;

import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.almarel.orion.math.shapes.OnionShape;

@Singleton
public class OnionGenerationService {

    public int fillOnion(Region region, Pattern pattern, EditSession session) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        double width = region.getWidth();
        double height = region.getHeight();
        double length = region.getLength();

        double centerX = min.getX() + width / 2.0 - 0.5;
        double centerZ = min.getZ() + length / 2.0 - 0.5;
        double minY = min.getY();

        OnionShape shape = new OnionShape(width, height, length);

        int count = 0;
        for (BlockVector3 v : region) {
            double localX = v.getX() - centerX;
            double localY = v.getY() - minY;
            double localZ = v.getZ() - centerZ;

            if (shape.isInside(localX, localY, localZ, null)) {
                if (session.setBlock(v, pattern)) {
                    count++;
                }
            }
        }
        return count;
    }
}