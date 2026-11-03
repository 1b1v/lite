package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Оценивает, насколько кандидат на роль target-материала ("anchor") способен
 * заменить собой ВСЕ формы ("shapes"), реально встретившиеся среди
 * memberBlockTypes цветовой группы — через поиск реальных suffix-вариантов
 * в реестре (переиспользует ту же генерацию id, что и ShapeAwareVariantResolver,
 * поэтому результат scorer'а и фактический резолв в RecolorMapBuilder согласованы).
 * <p>
 * Формы без суффиксного паттерна (LOG, CHAIN, LANTERN и т.п. — см.
 * {@link ShapeSuffixCandidateGenerator}) помечаются как "не применимые" (N/A)
 * и не влияют на итоговый score: для них family-unity через суффиксацию
 * принципиально недостижим, ими продолжает заниматься независимый
 * color-fallback механизм ниже по цепочке.
 */
@Singleton
public class MaterialFamilyCoverageScorer {

    private final MaterialBaseNameExtractor baseNameExtractor;
    private final ShapeSuffixCandidateGenerator suffixGenerator;

    @Inject
    public MaterialFamilyCoverageScorer(MaterialBaseNameExtractor baseNameExtractor,
                                        ShapeSuffixCandidateGenerator suffixGenerator) {
        this.baseNameExtractor = baseNameExtractor;
        this.suffixGenerator = suffixGenerator;
    }

    public CoverageResult score(BlockType anchor, Set<BlockShapeCategory> requiredShapes) {
        String namespace = baseNameExtractor.extractNamespace(anchor);
        String baseName = baseNameExtractor.extractBaseName(anchor);

        int applicable = 0;
        int covered = 0;
        Map<BlockShapeCategory, BlockType> resolvedVariants = new EnumMap<>(BlockShapeCategory.class);

        for (BlockShapeCategory shape : requiredShapes) {
            if (shape == BlockShapeCategory.FULL_BLOCK) {
                continue; // anchor сам по себе покрывает FULL_BLOCK
            }

            List<String> candidateIds = suffixGenerator.generateCandidates(shape, namespace, baseName);
            if (candidateIds.isEmpty()) {
                continue; // форма без суффиксного паттерна — N/A
            }

            applicable++;
            for (String id : candidateIds) {
                BlockType resolved = BlockTypes.get(id);
                if (resolved != null) {
                    covered++;
                    resolvedVariants.put(shape, resolved);
                    break;
                }
            }
        }

        double ratio = (applicable == 0) ? 1.0 : (double) covered / applicable;
        return new CoverageResult(covered, applicable, ratio, resolvedVariants);
    }

    public record CoverageResult(int covered, int applicable, double ratio,
                                 Map<BlockShapeCategory, BlockType> resolvedVariants) {
        public boolean isFullCoverage() {
            return applicable == 0 || covered == applicable;
        }
    }
}