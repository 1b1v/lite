package com.almarel.orion.curve.liana;

import com.almarel.orion.curve.CurveExecutionService;
import com.almarel.orion.curve.CurvePathService;
import com.google.inject.Inject;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.almarel.orion.curve.data.CurvePoint;
import com.almarel.orion.curve.data.LianaCommandArgs;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class LianaGenerationService {

    private final CurveExecutionService executionService;
    private final CurvePathService curvePathService;
    private final LianaRenderer lianaRenderer;

    @Inject
    public LianaGenerationService(CurveExecutionService executionService,
                                  CurvePathService curvePathService,
                                  LianaRenderer lianaRenderer) {
        this.executionService = executionService;
        this.curvePathService = curvePathService;
        this.lianaRenderer = lianaRenderer;
    }

    public CompletableFuture<Boolean> generateLiana(Player player,
                                                    List<Vector3> points,
                                                    LianaCommandArgs cmdArgs,
                                                    EditSession session) {
        return executionService.executeAsync(player, "Liana", () -> {
            int totalBlocks = 0;
            List<List<CurvePoint>> allPaths = new ArrayList<>();
            int lianaCount = cmdArgs.lianaCount();

            if (lianaCount == 3) {
                // Режим "Асимметричные ярусы": 1 главная лиана + 2 свисающие, стыкующиеся в центре
                List<CurvePoint> mainPath = curvePathService.calculatePath(points, -cmdArgs.sag());
                allPaths.add(mainPath);

                if (mainPath.size() > 4) {
                    ThreadLocalRandom random = ThreadLocalRandom.current();

                    // Уменьшенные отступы от краев (5% - 10%)
                    double marginStart = 0.05 + random.nextDouble(0.05);
                    double marginEnd = 0.05 + random.nextDouble(0.05);

                    // Доступное пространство между отступами
                    double availableSpace = 1.0 - marginStart - marginEnd;

                    // Точка стыковки: делим доступное пространство в пропорции 2/3 и 1/3
                    double splitPoint = marginStart + (availableSpace * (2.0 / 3.0));

                    // Вторая лиана (2/3): прогиб составляет 50% - 60% от главного
                    double start2 = marginStart;
                    double end2 = splitPoint;
                    double subSag2 = -cmdArgs.sag() * (0.50 + random.nextDouble(0.10));
                    allPaths.add(generateSubPath(mainPath, start2, end2, subSag2));

                    // Третья лиана (1/3): прогиб составляет 25% - 35% от главного (строго меньше)
                    double start3 = splitPoint;
                    double end3 = 1.0 - marginEnd;
                    double subSag3 = -cmdArgs.sag() * (0.25 + random.nextDouble(0.10));
                    allPaths.add(generateSubPath(mainPath, start3, end3, subSag3));
                }
            } else {
                // Стандартный режим: параллельные лианы
                for (int i = 0; i < lianaCount; i++) {
                    double currentSag = -cmdArgs.sag() / (1.0 + i);
                    allPaths.add(curvePathService.calculatePath(points, currentSag));
                }
            }

            PerlinNoise sharedNoise = new PerlinNoise();
            sharedNoise.setFrequency(0.06);

            for (List<CurvePoint> path : allPaths) {
                totalBlocks += lianaRenderer.renderStems(session, path, cmdArgs.stemPattern(), cmdArgs.tipPattern(), cmdArgs.stemLength(), sharedNoise);
            }

            for (List<CurvePoint> path : allPaths) {
                totalBlocks += lianaRenderer.renderArc(session, path, cmdArgs.pattern());
            }

            return totalBlocks;
        });
    }

    /**
     * Создает подвешенную под-лиану, извлекая точки из главной кривой.
     */
    private List<CurvePoint> generateSubPath(List<CurvePoint> mainPath, double startFrac, double endFrac, double sag) {
        int startIndex = (int) (mainPath.size() * startFrac);
        int endIndex = (int) (mainPath.size() * endFrac);

        startIndex = Math.max(0, Math.min(mainPath.size() - 1, startIndex));
        endIndex = Math.max(0, Math.min(mainPath.size() - 1, endIndex));

        if (startIndex >= endIndex) {
            return new ArrayList<>();
        }

        Vector3 startPos = mainPath.get(startIndex).position();
        Vector3 endPos = mainPath.get(endIndex).position();

        // CurvePathService ожидает координаты блоков (int), так как внутри прибавляет 0.5.
        // Округляем позиции обратно до углов блоков.
        Vector3 blockStart = Vector3.at(
                Math.floor(startPos.getX()),
                Math.floor(startPos.getY()),
                Math.floor(startPos.getZ())
        );
        Vector3 blockEnd = Vector3.at(
                Math.floor(endPos.getX()),
                Math.floor(endPos.getY()),
                Math.floor(endPos.getZ())
        );

        List<Vector3> subPoints = new ArrayList<>();
        subPoints.add(blockStart);
        subPoints.add(blockEnd);

        return curvePathService.calculatePath(subPoints, sag);
    }
}