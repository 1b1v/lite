package com.almarel.orion.curve.liana;

import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.almarel.orion.curve.data.CurvePoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class LianaRenderer {

    public int renderArc(EditSession session, List<CurvePoint> path, Pattern pattern) {
        int count = 0;
        BlockVector3 lastPlaced = null;

        for (CurvePoint point : path) {
            BlockVector3 bPos = point.position().toBlockPoint();

            if (!bPos.equals(lastPlaced)) {
                if (session.setBlock(bPos, pattern)) {
                    count++;
                }
                lastPlaced = bPos;
            }
        }

        return count;
    }

    public int renderStems(EditSession session, List<CurvePoint> path, Pattern stemPattern, Pattern tipPattern, int maxStemLength, PerlinNoise perlin) {
        if (maxStemLength <= 0) return 0;

        Map<BlockVector3, Pattern> stemMap = new HashMap<>();

        for (CurvePoint point : path) {
            BlockVector3 bPos = point.position().toBlockPoint();

            float noise = perlin.noise(Vector2.at(bPos.getX(), bPos.getZ()));
            float normalized = (noise + 1.0f) / 2.0f;
            normalized = (float) Math.pow(normalized, 3.0);

            if (normalized > 0.3f) {
                int stemLength = Math.max(1, (int) (normalized * maxStemLength));
                stemLength = Math.min(stemLength, maxStemLength);

                for (int y = 1; y <= stemLength; y++) {
                    BlockVector3 stemPos = BlockVector3.at(bPos.getX(), bPos.getY() - y, bPos.getZ());

                    boolean isTip = tipPattern != null && (stemLength - y) < 2;

                    if (isTip) {
                        stemMap.put(stemPos, tipPattern); // Кромка всегда перезаписывает веревку
                    } else {
                        stemMap.putIfAbsent(stemPos, stemPattern); // Веревка не перезаписывает кромку
                    }
                }
            }
        }

        int count = 0;
        for (Map.Entry<BlockVector3, Pattern> entry : stemMap.entrySet()) {
            if (session.setBlock(entry.getKey(), entry.getValue())) count++;
        }
        return count;
    }
}