package com.almarel.orion.mask;

import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class AroundMask extends AbstractMask {

    private final BitSet snapshot;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int[] offsets;

    public AroundMask(BitSet snapshot, int radius, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        this.snapshot = snapshot;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;

        int radiusSq = radius * radius;
        List<Integer> offsetList = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distXZ = dx * dx + dz * dz;
                for (int dy = -radius; dy <= radius; dy++) {
                    // Пропускаем смещение (0,0,0), так как статус самого блока
                    // мы теперь проверяем быстрым fail-fast условием в начале test()
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    if (distXZ + dy * dy <= radiusSq) {
                        offsetList.add(dx);
                        offsetList.add(dy);
                        offsetList.add(dz);
                    }
                }
            }
        }
        this.offsets = new int[offsetList.size()];
        for (int i = 0; i < offsetList.size(); i++) {
            this.offsets[i] = offsetList.get(i);
        }
    }

    @Override
    public boolean test(BlockVector3 v) {
        int bx = v.x();
        int by = v.y();
        int bz = v.z();

        // 1. Проверяем, находится ли текущий блок внутри нашей "области сканирования"
        if (bx >= minX && by >= minY && bz >= minZ &&
                bx < minX + sizeX && by < minY + sizeY && bz < minZ + sizeZ) {

            // Если этот блок САМ является искомым (например, stone) - исключаем его.
            // Это в точности повторяет логику FAWE: обводка рисуется ВОКРУГ блока,
            // но сам исходный блок не заменяется.
            if (snapshot.get(getIndex(bx, by, bz))) {
                return false;
            }
        }

        // 2. Ищем соседей в радиусе (нулевого сдвига здесь уже нет)
        for (int i = 0; i < offsets.length; i += 3) {
            int x = bx + offsets[i];
            int y = by + offsets[i + 1];
            int z = bz + offsets[i + 2];

            // Быстрая проверка границ куба (если сосед вне выделения - пропускаем)
            if (x < minX || y < minY || z < minZ ||
                    x >= minX + sizeX || y >= minY + sizeY || z >= minZ + sizeZ) {
                continue;
            }

            if (snapshot.get(getIndex(x, y, z))) {
                return true;
            }
        }
        return false;
    }

    private int getIndex(int x, int y, int z) {
        return (x - minX) + (z - minZ) * sizeX + (y - minY) * sizeX * sizeZ;
    }

    @Override
    public AbstractMask copy() {
        return this;
    }
}