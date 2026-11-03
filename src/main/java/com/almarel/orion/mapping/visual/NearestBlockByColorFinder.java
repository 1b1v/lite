package com.almarel.orion.mapping.visual;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Collection;
import java.util.Optional;

/**
 * Finds the visually closest BlockType to a target color among a given set
 * of candidates, using CIE76 color distance over BlockVisualProfile data.
 * <p>
 * Shape-agnostic by design: callers decide what "candidates" means (a single
 * shape category, the whole dataset, etc.) — this class only knows about
 * color, consistent with SRP. Candidates without a visual profile, or
 * flagged excluded in the dataset, are skipped.
 */
@Singleton
public class NearestBlockByColorFinder {

    private final BlockVisualProfileRepository repository;

    @Inject
    public NearestBlockByColorFinder(BlockVisualProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds the candidate whose profile color is closest to the target
     * BlockType's own profile color. Convenience overload for the common
     * case where the target is itself a real block (e.g. desired material).
     */
    public Optional<BlockType> findNearest(BlockType target, Collection<BlockType> candidates) {
        return repository.getProfile(target)
                .flatMap(targetProfile -> findNearest(targetProfile.labColor(), candidates));
    }

    /**
     * Finds the candidate whose profile color is closest to an arbitrary
     * target Lab color (e.g. a hue computed by palette generation, not tied
     * to any single real block).
     */
    public Optional<BlockType> findNearest(LabColor target, Collection<BlockType> candidates) {
        BlockType best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockType candidate : candidates) {
            Optional<BlockVisualProfile> profile = findEligibleProfile(candidate);
            if (profile.isEmpty()) {
                continue;
            }

            double distance = target.deltaE76(profile.get().labColor());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return Optional.ofNullable(best);
    }

    private Optional<BlockVisualProfile> findEligibleProfile(BlockType candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        return repository.getProfile(candidate)
                .filter(profile -> !profile.excluded());
    }
}