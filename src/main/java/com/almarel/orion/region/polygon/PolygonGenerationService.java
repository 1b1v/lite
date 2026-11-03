package com.almarel.orion.region.polygon;

import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.almarel.orion.math.SDF;
import com.almarel.orion.math.shapes.PolygonShape;
import com.almarel.orion.region.RegionParser;

@Singleton
public class PolygonGenerationService {

    public int fillPolygon(Region region, RegionParser.PolygonSettings settings, EditSession session) throws WorldEditException {
        PolygonShape shape = buildPolygonShape(region, settings);

        BlockVector3 min = region.getMinimumPoint();
        double centerX = min.getX() + region.getWidth() / 2.0 - 0.5;
        double centerZ = min.getZ() + region.getLength() / 2.0 - 0.5;
        double minY = min.getY();

        return generateSDF(session, region, settings.pattern(), shape, centerX, minY, centerZ);
    }

    private PolygonShape buildPolygonShape(Region region, RegionParser.PolygonSettings settings) {
        return new PolygonShape(region.getWidth(), region.getLength(), settings.vertices(), settings.hollow());
    }

    private int generateSDF(EditSession session, Region region, Pattern pattern, SDF shape,
                            double originX, double originY, double originZ) throws WorldEditException {
        int count = 0;
        for (BlockVector3 v : region) {
            if (isPointInsideSdf(shape, v, originX, originY, originZ) && session.setBlock(v, pattern)) {
                count++;
            }
        }
        return count;
    }

    private boolean isPointInsideSdf(SDF shape, BlockVector3 v, double originX, double originY, double originZ) {
        double localX = v.getX() - originX;
        double localY = v.getY() - originY;
        double localZ = v.getZ() - originZ;
        return shape.isInside(localX, localY, localZ, null);
    }
}