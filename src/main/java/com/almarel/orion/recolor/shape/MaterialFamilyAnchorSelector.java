package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Выбирает target-материал ("anchor") для DominantColorGroup с учётом не только
 * цвета, но и покрытия форм ("shapes"), реально встретившихся среди
 * memberBlockTypes этой группы — чтобы вся материальная семья (доски, ступеньки,
 * забор, ворота, люк...) перекрашивалась в ОДИН согласованный материал.
 * <p>
 * LOG намеренно исключён из requiredShapes: у non-wood материалов (terracotta,
 * concrete, wool...) принципиально нет "бревенчатой" формы, унификация через
 * суффиксацию для него недостижима в принципе. LOG-члены группы продолжают
 * резолвиться независимым color-fallback'ом ниже по цепочке (решение
 * согласовано явно, не технический долг).
 * <p>
 * Пул кандидатов должен быть заранее отсортирован по возрастанию цветовой
 * дистанции (nearest-first) — это ответственность вызывающей стороны
 * (PaletteVariantGenerator), этот класс цветовых сравнений сам не делает.
 */
@Singleton
public class MaterialFamilyAnchorSelector {

    private final BlockShapeClassifier shapeClassifier;
    private final MaterialFamilyCoverageScorer coverageScorer;

    @Inject
    public MaterialFamilyAnchorSelector(BlockShapeClassifier shapeClassifier,
                                        MaterialFamilyCoverageScorer coverageScorer) {
        this.shapeClassifier = shapeClassifier;
        this.coverageScorer = coverageScorer;
    }

    public AnchorSelection selectAnchor(Set<BlockType> memberBlockTypes, List<BlockType> colorSortedCandidates) {
        Set<BlockShapeCategory> requiredShapes = memberBlockTypes.stream()
                .map(shapeClassifier::classify)
                .filter(category -> category != BlockShapeCategory.LOG)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BlockShapeCategory.class)));

        BlockType bestCandidate = null;
        MaterialFamilyCoverageScorer.CoverageResult bestResult = null;

        for (BlockType candidate : colorSortedCandidates) {
            MaterialFamilyCoverageScorer.CoverageResult result = coverageScorer.score(candidate, requiredShapes);

            if (bestResult == null || result.covered() > bestResult.covered()) {
                bestCandidate = candidate;
                bestResult = result;
            }

            if (result.isFullCoverage()) {
                break; // пул отсортирован по цвету — первый full-coverage одновременно и ближайший
            }
        }

        return new AnchorSelection(bestCandidate, bestResult);
    }

    public record AnchorSelection(BlockType anchorType, MaterialFamilyCoverageScorer.CoverageResult coverage) {
    }
}