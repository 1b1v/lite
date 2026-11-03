package com.almarel.orion.region.centervertical;

import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

/**
 * Чистая геометрическая логика генерации вертикальной колонны по центру региона.
 * Никакого threading/messaging — вся оркестрация (сессия, async, уведомления)
 * вынесена в CenterVerticalCommand.
 */
@Singleton
public class CenterVerticalGenerationService {

    public int fillColumn(Region region, Pattern pattern, EditSession session) throws WorldEditException {
        int centerX = region.getCenter().getBlockX();
        int centerZ = region.getCenter().getBlockZ();
        int minY = region.getMinimumPoint().getBlockY();
        int maxY = region.getMaximumPoint().getBlockY();

        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            if (session.setBlock(BlockVector3.at(centerX, y, centerZ), pattern)) {
                count++;
            }
        }
        return count;
    }
}