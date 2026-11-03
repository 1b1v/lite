package com.almarel.orion.generation.platform;

import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

@Singleton
public class PlatformGenerationService {

    public int fillPlatform(BlockVector3 center, int radius, Pattern pattern, EditSession session) throws WorldEditException {
        int count = 0;
        int y = center.getY();
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (session.setBlock(BlockVector3.at(x, y, z), pattern)) {
                    count++;
                }
            }
        }
        return count;
    }
}