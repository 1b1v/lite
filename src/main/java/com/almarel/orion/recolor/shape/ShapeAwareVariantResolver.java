package com.almarel.orion.recolor.shape;

import com.almarel.orion.mapping.visual.NearestBlockByColorFinder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.List;
import java.util.Optional;

@Singleton
public class ShapeAwareVariantResolver {

    private final BlockShapeClassifier classifier;
    private final MaterialBaseNameExtractor nameExtractor;
    private final ShapeSuffixCandidateGenerator candidateGenerator;
    private final NearestBlockByColorFinder nearestBlockByColorFinder;

    @Inject
    public ShapeAwareVariantResolver(BlockShapeClassifier classifier,
                                     MaterialBaseNameExtractor nameExtractor,
                                     ShapeSuffixCandidateGenerator candidateGenerator,
                                     NearestBlockByColorFinder nearestBlockByColorFinder) {
        this.classifier = classifier;
        this.nameExtractor = nameExtractor;
        this.candidateGenerator = candidateGenerator;
        this.nearestBlockByColorFinder = nearestBlockByColorFinder;
    }

    public Optional<BlockType> resolve(BlockStateHolder<?> oldBlock, BlockType desiredMaterial) {
        BlockShapeCategory category = classifier.classify(oldBlock);

        if (isDirectMatch(category, desiredMaterial)) {
            return Optional.of(desiredMaterial);
        }

        return findVariantBySuffix(category, desiredMaterial)
                .or(() -> findFallbackByColor(category, desiredMaterial));
    }

    private boolean isDirectMatch(BlockShapeCategory targetCategory, BlockType desiredMaterial) {
        return targetCategory == BlockShapeCategory.FULL_BLOCK
                || classifier.classify(desiredMaterial) == targetCategory;
    }

    private Optional<BlockType> findVariantBySuffix(BlockShapeCategory category, BlockType desiredMaterial) {
        String namespace = nameExtractor.extractNamespace(desiredMaterial);
        String baseName = nameExtractor.extractBaseName(desiredMaterial);

        List<String> candidates = candidateGenerator.generateCandidates(category, namespace, baseName);
        for (String candidateId : candidates) {
            Optional<BlockType> validCandidate = resolveValidCandidate(candidateId, category);
            if (validCandidate.isPresent()) {
                return validCandidate;
            }
        }

        return Optional.empty();
    }

    private Optional<BlockType> resolveValidCandidate(String candidateId, BlockShapeCategory expectedCategory) {
        BlockType candidate = BlockTypes.get(candidateId);
        if (candidate != null && classifier.classify(candidate) == expectedCategory) {
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private Optional<BlockType> findFallbackByColor(BlockShapeCategory category, BlockType desiredMaterial) {
        return nearestBlockByColorFinder.findNearest(desiredMaterial, classifier.getBlocksInCategory(category));
    }
}