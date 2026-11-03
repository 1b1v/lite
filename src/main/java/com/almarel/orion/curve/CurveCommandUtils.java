// TYPE B: FULL FILE
package com.almarel.orion.curve;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CurveCommandUtils {

    private static final int MIN_CONVEX_POINTS = 2;

    private CurveCommandUtils() {}

    // ==========================================
    // Region Vector Extraction
    // ==========================================

    /**
     * Извлекает векторы из текущего выделения игрока (Cuboid или Convex).
     * Возвращает null, если выделение некорректно.
     * Не отправляет сообщения игроку — это ответственность слоя презентации (Command)!
     */
    public static List<Vector3> extractPoints(com.sk89q.worldedit.entity.Player wePlayer, LocalSession localSession) {
        RegionSelector selector = localSession.getRegionSelector(wePlayer.getWorld());

        if (selector instanceof CuboidRegionSelector cuboidSelector) {
            return extractCuboidPoints(cuboidSelector);
        }

        if (selector instanceof ConvexPolyhedralRegionSelector convexSelector) {
            return extractConvexPoints(convexSelector);
        }

        return null;
    }

    private static List<Vector3> extractCuboidPoints(CuboidRegionSelector cuboidSelector) {
        BlockVector3 pos1 = cuboidSelector.getIncompleteRegion().getPos1();
        BlockVector3 pos2 = cuboidSelector.getIncompleteRegion().getPos2();

        if (pos1 == null || pos2 == null) {
            return null;
        }

        return List.of(pos1.toVector3(), pos2.toVector3());
    }

    private static List<Vector3> extractConvexPoints(ConvexPolyhedralRegionSelector convexSelector) {
        ConvexPolyhedralRegion convexRegion = (ConvexPolyhedralRegion) convexSelector.getIncompleteRegion();
        List<Vector3> points = new ArrayList<>();

        for (BlockVector3 vertex : convexRegion.getVertices()) {
            points.add(vertex.toVector3());
        }

        if (points.size() < MIN_CONVEX_POINTS) {
            return null;
        }

        return points;
    }

    // ==========================================
    // Pattern Parsing
    // ==========================================

    /**
     * Безопасный парсинг паттерна с очисткой от скобок.
     */
    public static Pattern parsePattern(String input, ParserContext context) throws Exception {
        String cleanInput = sanitizePatternInput(input);
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(cleanInput, context);
    }

    private static String sanitizePatternInput(String input) {
        return input.replaceAll("^\\[|\\]$", "");
    }

    // ==========================================
    // EditSession Execution Lifecycle
    // ==========================================

    public static void executeGeneration(Plugin plugin,
                                         TaskRunner taskRunner,
                                         CompletableFuture<Boolean> future,
                                         LocalSession localSession,
                                         EditSession editSession) {
        future.whenComplete((success, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("Curve generation failed: " + throwable.getMessage());
            }

            flushQueueSafely(plugin, editSession);
            recordAndCloseSession(plugin, taskRunner, localSession, editSession);
        });
    }

    private static void flushQueueSafely(Plugin plugin, EditSession editSession) {
        try {
            // Применяем все изменения (даже если генерация упала на полпути)
            editSession.flushQueue();
        } catch (Exception e) {
            plugin.getLogger().severe("Error flushing queue: " + e.getMessage());
        }
    }

    private static void recordAndCloseSession(Plugin plugin, TaskRunner taskRunner, LocalSession localSession, EditSession editSession) {
        // В ЛЮБОМ СЛУЧАЕ сохраняем сессию в историю, чтобы //undo работал!
        taskRunner.runSync(() -> {
            try {
                localSession.remember(editSession);
            } catch (Exception e) {
                plugin.getLogger().severe("Error remembering session: " + e.getMessage());
            } finally {
                editSession.close();
            }
        });
    }
}