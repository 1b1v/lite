package com.almarel.orion.region.frame;

import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

@Singleton
public class FrameGenerationService {

    public int fillFrame(Region region, Pattern pattern, EditSession session) throws WorldEditException {
        int count = 0;
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        for (BlockVector3 v : region) {
            if (isFrameEdge(v, min, max) && session.setBlock(v, pattern)) {
                count++;
            }
        }
        return count;
    }

    private boolean isFrameEdge(BlockVector3 v, BlockVector3 min, BlockVector3 max) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();

        boolean onX = (x == min.getBlockX() || x == max.getBlockX());
        boolean onY = (y == min.getBlockY() || y == max.getBlockY());
        boolean onZ = (z == min.getBlockZ() || z == max.getBlockZ());

        int facesTouching = (onX ? 1 : 0) + (onY ? 1 : 0) + (onZ ? 1 : 0);
        return facesTouching >= 2;
    }
}