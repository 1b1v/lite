package com.almarel.orion.recolor.palette;

import com.almarel.orion.mapping.visual.BlockVisualProfile;
import com.almarel.orion.mapping.visual.BlockVisualProfileRepository;
import com.almarel.orion.mapping.visual.LabColor;
import com.almarel.orion.recolor.shape.MaterialFamilyBundler;
import com.almarel.orion.recolor.shape.MaterialFamilyBundler.MaterialFamilyBundle;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Singleton
public class DominantColorExtractor {

    private static final double DEFAULT_CLUSTER_THRESHOLD = 18.0;
    private static final int DEFAULT_MAX_GROUPS = 5;

    private final BlockVisualProfileRepository profileRepository;
    private final MaterialFamilyBundler materialFamilyBundler;

    @Inject
    public DominantColorExtractor(BlockVisualProfileRepository profileRepository,
                                  MaterialFamilyBundler materialFamilyBundler) {
        this.profileRepository = profileRepository;
        this.materialFamilyBundler = materialFamilyBundler;
    }

    public List<DominantColorGroup> extract(Map<BlockType, Long> weightedBlockCounts) {
        return extract(weightedBlockCounts, DEFAULT_CLUSTER_THRESHOLD, DEFAULT_MAX_GROUPS);
    }

    public List<DominantColorGroup> extract(Map<BlockType, Long> weightedBlockCounts,
                                            double clusterThreshold,
                                            int maxGroups) {
        Map<BlockType, Long> eligibleWeights = collectEligibleWeights(weightedBlockCounts);
        if (eligibleWeights.isEmpty()) {
            return List.of();
        }

        List<MaterialFamilyBundle> bundles = materialFamilyBundler.bundle(eligibleWeights);
        List<MutableCluster> clusters = buildClusters(bundles, clusterThreshold);
        List<MutableCluster> limited = limitGroups(clusters, maxGroups);
        long totalWeight = eligibleWeights.values().stream().mapToLong(Long::longValue).sum();

        return limited.stream()
                .map(cluster -> toGroup(cluster, totalWeight))
                .toList();
    }

    private Map<BlockType, Long> collectEligibleWeights(Map<BlockType, Long> weightedBlockCounts) {
        Map<BlockType, Long> result = new LinkedHashMap<>(weightedBlockCounts.size());
        for (Map.Entry<BlockType, Long> entry : weightedBlockCounts.entrySet()) {
            profileRepository.getProfile(entry.getKey())
                    .filter(profile -> !profile.excluded())
                    .ifPresent(profile -> result.put(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Leader-based (canopy) clustering, как и раньше, но теперь единица
     * обработки — АТОМАРНЫЙ {@link MaterialFamilyBundle}, а не отдельный
     * BlockType. Это структурно исключает разведение форм одного материала
     * по разным кластерам из-за порядка обработки (см. справку, Проблема 3) —
     * бандл либо целиком входит в кластер, либо целиком остаётся за ним.
     */
    private List<MutableCluster> buildClusters(List<MaterialFamilyBundle> bundles, double threshold) {
        List<BundleWithColor> sortedByWeight = bundles.stream()
                .map(this::toBundleWithColor)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong((BundleWithColor b) -> b.bundle().totalWeight()).reversed())
                .toList();

        List<MutableCluster> clusters = new ArrayList<>();
        for (BundleWithColor candidate : sortedByWeight) {
            MutableCluster nearest = findNearestCluster(clusters, candidate.labColor(), threshold);
            if (nearest == null) {
                clusters.add(new MutableCluster(candidate));
            } else {
                nearest.addBundle(candidate);
            }
        }

        return clusters;
    }

    private BundleWithColor toBundleWithColor(MaterialFamilyBundle bundle) {
        return profileRepository.getProfile(bundle.representativeType())
                .map(profile -> new BundleWithColor(bundle, profile))
                .orElse(null);
    }

    private MutableCluster findNearestCluster(List<MutableCluster> clusters, LabColor color, double threshold) {
        MutableCluster nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (MutableCluster cluster : clusters) {
            double distance = cluster.representativeProfile.labColor().deltaE76(color);
            if (distance < threshold && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = cluster;
            }
        }

        return nearest;
    }

    /**
     * Ограничивает число групп {@code maxGroups}, НЕ теряя при этом ни одного
     * BlockType. Кластеры, не попавшие в top-N по весу, принудительно (без
     * проверки clusterThreshold — цель гарантировать поглощение, а не повторить
     * логику {@link #findNearestCluster}) присоединяются к БЛИЖАЙШЕМУ по
     * Lab-цвету из оставленных top-N кластеров.
     * <p>
     * Раньше (до этого патча) лишние кластеры просто отбрасывались
     * ({@code .limit(maxGroups)} без обработки "хвоста"), из-за чего
     * малочисленные, но реально присутствующие в постройке материалы
     * (например, рама окна из тёмного дуба на фоне большого здания) могли
     * вообще не попасть в финальный список групп и остаться НЕ перекрашенными
     * авто-палитрой — см. справку, Проблема 3, кейс 2 ("абсурд": люк
     * перекрасился, ступеньки и плиты той же рамы — нет).
     */
    private List<MutableCluster> limitGroups(List<MutableCluster> clusters, int maxGroups) {
        if (maxGroups <= 0) {
            return List.of();
        }
        if (clusters.size() <= maxGroups) {
            return clusters.stream()
                    .sorted(Comparator.comparingLong((MutableCluster c) -> c.totalCount).reversed())
                    .toList();
        }

        List<MutableCluster> sortedByWeight = clusters.stream()
                .sorted(Comparator.comparingLong((MutableCluster c) -> c.totalCount).reversed())
                .toList();

        List<MutableCluster> kept = new ArrayList<>(sortedByWeight.subList(0, maxGroups));
        List<MutableCluster> overflow = sortedByWeight.subList(maxGroups, sortedByWeight.size());

        for (MutableCluster orphan : overflow) {
            MutableCluster closest = findClosestClusterUnconditional(kept, orphan.representativeProfile.labColor());
            closest.absorb(orphan);
        }

        return kept.stream()
                .sorted(Comparator.comparingLong((MutableCluster c) -> c.totalCount).reversed())
                .toList();
    }

    /**
     * В отличие от {@link #findNearestCluster}, не ограничена порогом —
     * здесь цель не "похож ли цвет достаточно, чтобы считаться тем же
     * материалом", а "к какому из оставшихся домов этот материал ближе
     * всего по цвету", когда разместить его отдельной группой уже нельзя
     * из-за лимита {@code maxGroups}. {@code kept} гарантированно не пуст:
     * вызывается только когда {@code maxGroups >= 1} и исходных кластеров
     * строго больше {@code maxGroups}.
     */
    private MutableCluster findClosestClusterUnconditional(List<MutableCluster> kept, LabColor color) {
        MutableCluster closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (MutableCluster cluster : kept) {
            double distance = cluster.representativeProfile.labColor().deltaE76(color);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = cluster;
            }
        }

        return closest;
    }

    private DominantColorGroup toGroup(MutableCluster cluster, long totalWeight) {
        double fraction = totalWeight == 0 ? 0.0 : (double) cluster.totalCount / totalWeight;
        return new DominantColorGroup(
                cluster.representativeProfile.labColor(),
                cluster.representativeProfile.hslColor(),
                cluster.representativeType,
                cluster.totalCount,
                fraction,
                Set.copyOf(cluster.members)
        );
    }

    private record BundleWithColor(MaterialFamilyBundle bundle, BlockVisualProfile representativeProfile) {
        LabColor labColor() {
            return representativeProfile.labColor();
        }
    }

    private static final class MutableCluster {
        private final BlockVisualProfile representativeProfile;
        private final BlockType representativeType;
        private final Set<BlockType> members = new HashSet<>();
        private long totalCount;

        private MutableCluster(BundleWithColor leader) {
            this.representativeProfile = leader.representativeProfile();
            this.representativeType = leader.bundle().representativeType();
            this.members.addAll(leader.bundle().members());
            this.totalCount = leader.bundle().totalWeight();
        }

        private void addBundle(BundleWithColor member) {
            this.members.addAll(member.bundle().members());
            this.totalCount += member.bundle().totalWeight();
        }

        /**
         * Поглощение целого overflow-кластера при превышении maxGroups
         * (см. {@link DominantColorExtractor#limitGroups}). representativeProfile/
         * representativeType поглощающего кластера НЕ меняются — anchor-цвет
         * группы остаётся цветом исходного, более весомого лидера, поглощённый
         * материал лишь добавляет свои BlockType и вес в группу.
         */
        private void absorb(MutableCluster other) {
            this.members.addAll(other.members);
            this.totalCount += other.totalCount;
        }
    }
}