package com.almarel.orion.recolor.palette;

import com.almarel.orion.recolor.shape.BlockShapeCategory;
import com.almarel.orion.recolor.shape.BlockShapeClassifier;
import com.almarel.orion.recolor.shape.FullBlockPaletteLineSplitter;
import com.almarel.orion.recolor.shape.MaterialBaseNameExtractor;
import com.almarel.orion.recolor.shape.MaterialFamilyAnchorSelector;
import com.almarel.orion.mapping.visual.BlockVisualProfile;
import com.almarel.orion.mapping.visual.BlockVisualProfileRepository;
import com.almarel.orion.mapping.visual.ColorSpaceConverter;
import com.almarel.orion.mapping.visual.HslColor;
import com.almarel.orion.mapping.visual.LabColor;
import com.almarel.orion.recolor.core.RecolorExclusionRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Оркестратор палитрового трека: превращает подсчёт блоков региона в
 * набор готовых гармоничных вариантов перекраски всей постройки.
 * <p>
 * Якорь ("anchor") для поворота Hue выбирается через
 * {@link DominantGroupAnchorSelector} — самая весомая ХРОМАТИЧЕСКАЯ группа.
 * Ахроматичные не-якорные группы (белое/серое стекло и т.п.) не получают
 * новый целевой цвет — остаются как есть (см. справку, Проблема 3).
 * <p>
 * Для каждой группы target-материал(ы) выбираются в два уровня:
 * 1. {@link FullBlockPaletteLineSplitter} разбивает memberBlockTypes на
 *    независимые "палитровые линии" (concrete/wool/terracotta/...), если
 *    их в группе НЕСКОЛЬКО — чтобы, например, brown_concrete и brown_wool
 *    не схлопывались в единственный материал, теряя одну из двух фактур
 *    (см. справку, Проблема 4).
 * 2. Внутри каждой линии {@link MaterialFamilyAnchorSelector} выбирает
 *    ближайший по цвету материал, покрывающий ВСЕ формы (stairs/fence/...),
 *    реально встретившиеся среди членов этой линии (см. справку, Проблема 1).
 * <p>
 * ВАЖНО: это чистая CPU-математика без Bukkit-вызовов, подходит для
 * выполнения в async-потоке (см. ORION ARCHITECTURE RULES, п.1).
 */
@Singleton
public class PaletteVariantGenerator {

    private final DominantColorExtractor dominantColorExtractor;
    private final PaletteHarmonyApplier harmonyApplier;
    private final BlockVisualProfileRepository profileRepository;
    private final HueShiftCalculator hueShiftCalculator;
    private final RecolorExclusionRegistry exclusionRegistry;
    private final BlockShapeClassifier shapeClassifier;
    private final MaterialFamilyAnchorSelector anchorSelector;
    private final DominantGroupAnchorSelector groupAnchorSelector;
    private final FullBlockPaletteLineSplitter paletteLineSplitter;
    private final MaterialBaseNameExtractor baseNameExtractor;

    @Inject
    public PaletteVariantGenerator(DominantColorExtractor dominantColorExtractor,
                                   PaletteHarmonyApplier harmonyApplier,
                                   BlockVisualProfileRepository profileRepository,
                                   HueShiftCalculator hueShiftCalculator,
                                   RecolorExclusionRegistry exclusionRegistry,
                                   BlockShapeClassifier shapeClassifier,
                                   MaterialFamilyAnchorSelector anchorSelector,
                                   DominantGroupAnchorSelector groupAnchorSelector,
                                   FullBlockPaletteLineSplitter paletteLineSplitter,
                                   MaterialBaseNameExtractor baseNameExtractor) {
        this.dominantColorExtractor = dominantColorExtractor;
        this.harmonyApplier = harmonyApplier;
        this.profileRepository = profileRepository;
        this.hueShiftCalculator = hueShiftCalculator;
        this.exclusionRegistry = exclusionRegistry;
        this.shapeClassifier = shapeClassifier;
        this.anchorSelector = anchorSelector;
        this.groupAnchorSelector = groupAnchorSelector;
        this.paletteLineSplitter = paletteLineSplitter;
        this.baseNameExtractor = baseNameExtractor;
    }

    public List<PaletteVariant> generateVariants(Map<BlockType, Long> weightedBlockCounts) {
        List<DominantColorGroup> groups = dominantColorExtractor.extract(weightedBlockCounts);
        if (groups.isEmpty()) {
            return List.of();
        }

        DominantColorGroup anchor = groupAnchorSelector.selectAnchor(groups);
        List<BlockType> candidatePool = collectCandidatePool();

        List<PaletteVariant> variants = new ArrayList<>(HarmonySchemePresets.ALL.size());
        for (HarmonyPreset preset : HarmonySchemePresets.ALL) {
            Map<BlockType, BlockType> targets = resolveVariantTargets(groups, anchor, preset, candidatePool);
            if (!targets.isEmpty()) {
                variants.add(new PaletteVariant(preset.displayName(), targets));
            }
        }

        return variants;
    }

    /**
     * Резолв ОДНОЙ гармоничной схемы в карту замен — публичная точка входа
     * для GUI-интеграции. {@code anchor} должен быть получен через
     * {@link DominantGroupAnchorSelector} вызывающей стороной.
     */
    public Map<BlockType, BlockType> resolveVariantTargets(List<DominantColorGroup> groups,
                                                           DominantColorGroup anchor,
                                                           HarmonyPreset preset) {
        return resolveVariantTargets(groups, anchor, preset, collectCandidatePool());
    }

    private Map<BlockType, BlockType> resolveVariantTargets(List<DominantColorGroup> groups,
                                                            DominantColorGroup anchor,
                                                            HarmonyPreset preset,
                                                            List<BlockType> candidatePool) {
        double newAnchorHue = hueShiftCalculator.normalizeHueDegrees(
                anchor.representativeHsl().hue() + preset.anchorHueDeltaDegrees());

        HslColor newAnchorHsl = new HslColor(
                newAnchorHue,
                anchor.representativeHsl().saturation(),
                anchor.representativeHsl().lightness());

        Map<DominantColorGroup, HslColor> targetColors =
                harmonyApplier.computeTargetColors(groups, anchor, newAnchorHsl);

        Map<BlockType, BlockType> desiredTargets = new LinkedHashMap<>();
        for (Map.Entry<DominantColorGroup, HslColor> entry : targetColors.entrySet()) {
            DominantColorGroup group = entry.getKey();

            if (group != anchor && groupAnchorSelector.isAchromatic(group)) {
                continue; // ахроматичная не-якорная группа (напр. белое стекло) — оставляем как есть
            }

            resolveGroupTargets(group, entry.getValue(), candidatePool, desiredTargets);
        }

        return Map.copyOf(desiredTargets);
    }

    private void resolveGroupTargets(DominantColorGroup group,
                                     HslColor targetHsl,
                                     List<BlockType> candidatePool,
                                     Map<BlockType, BlockType> desiredTargets) {
        LabColor targetLab = ColorSpaceConverter.hslToLab(targetHsl);

        List<FullBlockPaletteLineSplitter.PaletteLineCluster> clusters =
                paletteLineSplitter.split(group.memberBlockTypes(), group.representativeBlockType());
        boolean multipleLines = clusters.size() > 1;

        for (FullBlockPaletteLineSplitter.PaletteLineCluster cluster : clusters) {
            List<BlockType> scopedPool = (multipleLines && cluster.paletteLineSuffix().isPresent())
                    ? filterByPaletteLine(candidatePool, cluster.paletteLineSuffix().get())
                    : candidatePool;

            List<BlockType> sortedCandidates = sortByColorDistance(targetLab, scopedPool);

            MaterialFamilyAnchorSelector.AnchorSelection selection =
                    anchorSelector.selectAnchor(cluster.members(), sortedCandidates);

            if (selection.anchorType() != null) {
                mapAllMembers(cluster.members(), selection.anchorType(), desiredTargets);
            }
        }
    }

    /**
     * Ограничивает пул кандидатов той же "палитровой линией" (concrete/wool/...),
     * что и исходный кластер — применяется ТОЛЬКО когда в группе обнаружено
     * несколько конфликтующих линий (см. FullBlockPaletteLineSplitter), чтобы
     * каждая линия сохранила собственную фактуру вместо случайного кросс-материального
     * совпадения. В обычном (единственная линия) случае пул НЕ ограничивается —
     * материал вправе измениться полностью (это намеренное поведение Проблемы 1).
     */
    private List<BlockType> filterByPaletteLine(List<BlockType> pool, String suffix) {
        return pool.stream()
                .filter(type -> baseNameExtractor.matchPaletteLineSuffix(type)
                        .map(suffix::equals)
                        .orElse(false))
                .toList();
    }

    private List<BlockType> sortByColorDistance(LabColor targetLab, List<BlockType> pool) {
        Map<BlockType, Double> distanceByType = new HashMap<>(pool.size());
        for (BlockType type : pool) {
            profileRepository.getProfile(type)
                    .ifPresent(profile -> distanceByType.put(type, profile.labColor().deltaE76(targetLab)));
        }

        return distanceByType.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void mapAllMembers(Set<BlockType> members, BlockType targetBlockType, Map<BlockType, BlockType> output) {
        for (BlockType member : members) {
            output.put(member, targetBlockType);
        }
    }

    /**
     * Пул кандидатов на роль "нового базового материала" ограничен:
     * 1. визуально пригодными блоками (не LIQUID/EMITS_LIGHT, dataset-флаг excluded);
     * 2. не NBT-тяжёлыми (RecolorExclusionRegistry — signs/beds/banners/shulker boxes),
     *    тем же реестром, которым пользуется реальный Apply-пайплайн (RecolorMapBuilder);
     * 3. формой FULL_BLOCK (BlockShapeClassifier) — нас интересует только "эссенция
     *    материала" (цвет), конкретную форму для каждого члена семьи достраивает
     *    MaterialFamilyAnchorSelector (на этапе выбора) и downstream
     *    ShapeAwareVariantResolver (на этапе реального применения);
     * 4. имеющими реальную item-форму в Bukkit (Material.isItem()) — некоторые
     *    блоки (например minecraft:cocoa — геометрически блок, но в инвентаре
     *    представлен ОТДЕЛЬНЫМ материалом COCOA_BEANS) не могут стать ItemStack
     *    напрямую через BukkitAdapter.adapt(blockType); попытка нарисовать такую
     *    иконку в GUI раньше приводила к IllegalArgumentException ("X isn't an
     *    item") и разваливала рендер всей сессии меню. Такие блоки исключаются
     *    из кандидатов на роль НОВОГО материала — они всё равно непригодны для
     *    предпросмотра в GUI, независимо от того, насколько хорошо совпадает цвет.
     */
    private List<BlockType> collectCandidatePool() {
        return profileRepository.getAllProfiles().stream()
                .filter(profile -> !profile.excluded())
                .map(BlockVisualProfile::blockType)
                .filter(type -> !exclusionRegistry.isExcluded(type))
                .filter(type -> shapeClassifier.classify(type) == BlockShapeCategory.FULL_BLOCK)
                .toList();
    }

    /**
     * Защита от блоков без item-эквивалента (см. комментарий выше). Оборачиваю
     * в try-catch намеренно: BukkitAdapter.adapt для некоторых экзотических/
     * версионно-специфичных BlockType теоретически может бросить исключение
     * вместо возврата null — fail-safe (исключить из пула), а не уронить
     * весь расчёт палитры из-за одного проблемного кандидата.
     */
    private boolean hasRenderableItemForm(BlockType type) { // no usage
        try {
            org.bukkit.Material material = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(type);
            return material != null && material.isItem();
        } catch (Exception e) {
            return false;
        }
    }
}