package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Группирует отдельные BlockType в атомарные "материальные семьи" ДО
 * цветовой кластеризации ({@link com.almarel.orion.recolor.palette.DominantColorExtractor}).
 * <p>
 * Назначение: гарантировать СТРУКТУРНО (а не вероятностно, через случайную
 * близость по Lab-цвету и зависимость от порядка обработки), что все формы
 * одного материала (доски+ступени+плита+люк одной породы дерева) окажутся
 * в одном {@link MaterialFamilyBundle} и физически не смогут быть разведены
 * по разным DominantColorGroup leader-based алгоритмом (см. справку,
 * Проблема 3: "один исходный материал раскалывается на 2+ целевых").
 * <p>
 * Правило объединения:
 * <ul>
 *   <li>Все элементы группируются по {@link MaterialBaseNameExtractor#extractBaseName}.</li>
 *   <li>Внутри одной базовой группы FULL_BLOCK-члены дополнительно делятся
 *       по палитровой линии ({@link MaterialBaseNameExtractor#matchPaletteLineSuffix}).
 *       Если линия ровно одна (типичный случай породы дерева — только
 *       "_planks") — shape-формы присоединяются к ней напрямую, неоднозначности
 *       нет. Если линий 2+ (например red_wool / red_concrete) — ЭТОТ класс их
 *       намеренно НЕ объединяет: каждая линия остаётся отдельным бандлом,
 *       а shape-формы без палитрового суффикса (carpet и т.п.) выделяются в
 *       свой отдельный бандл. Разрешение этой неоднозначности — зона
 *       ответственности {@link FullBlockPaletteLineSplitter}, который
 *       запускается позже, уже после цветовой кластеризации, и не меняется
 *       этим классом (см. справку, Проблема 4 — её логика остаётся рабочей
 *       как есть).</li>
 * </ul>
 * <p>
 * Чистая CPU-математика без Bukkit-вызовов, без знания о цвете/визуальном
 * профиле — подходит для async-потока (ORION ARCHITECTURE RULES, п.1).
 */
@Singleton
public class MaterialFamilyBundler {

    private final BlockShapeClassifier shapeClassifier;
    private final MaterialBaseNameExtractor baseNameExtractor;

    @Inject
    public MaterialFamilyBundler(BlockShapeClassifier shapeClassifier,
                                 MaterialBaseNameExtractor baseNameExtractor) {
        this.shapeClassifier = shapeClassifier;
        this.baseNameExtractor = baseNameExtractor;
    }

    /**
     * @param weights веса (счётчики из региона), УЖЕ отфильтрованные вызывающей
     *                стороной до визуально пригодных (не excluded) BlockType —
     *                этот класс не знает о {@code BlockVisualProfile} и не
     *                выполняет собственной фильтрации.
     */
    public List<MaterialFamilyBundle> bundle(Map<BlockType, Long> weights) {
        Map<String, List<BlockType>> byBaseName = groupByBaseName(weights.keySet());

        List<MaterialFamilyBundle> result = new ArrayList<>();
        for (List<BlockType> baseNameGroup : byBaseName.values()) {
            result.addAll(bundleBaseNameGroup(baseNameGroup, weights));
        }
        return result;
    }

    private Map<String, List<BlockType>> groupByBaseName(Set<BlockType> types) {
        Map<String, List<BlockType>> grouped = new LinkedHashMap<>();
        for (BlockType type : types) {
            grouped.computeIfAbsent(baseNameExtractor.extractBaseName(type), k -> new ArrayList<>()).add(type);
        }
        return grouped;
    }

    private List<MaterialFamilyBundle> bundleBaseNameGroup(List<BlockType> baseNameGroup,
                                                           Map<BlockType, Long> weights) {
        Map<String, Set<BlockType>> byLine = new LinkedHashMap<>();
        Set<BlockType> shapeMembers = new LinkedHashSet<>();

        for (BlockType type : baseNameGroup) {
            if (shapeClassifier.classify(type) == BlockShapeCategory.FULL_BLOCK) {
                String key = baseNameExtractor.matchPaletteLineSuffix(type)
                        .orElseGet(() -> "unmatched:" + type.id()); // как в FullBlockPaletteLineSplitter: никогда не сливаются друг с другом
                byLine.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(type);
            } else {
                shapeMembers.add(type);
            }
        }

        if (byLine.isEmpty()) {
            // Нет ни одного FULL_BLOCK-эквивалента в этой базовой группе вообще
            // (например, в регионе есть только формы дерева, без самих досок) —
            // всё равно один материал, один бандл.
            return List.of(toBundle(shapeMembers, weights));
        }

        if (byLine.size() == 1) {
            // Единственная палитровая линия — неоднозначности нет. Это и есть
            // фикс Проблемы 3: формы присоединяются к линии СТРУКТУРНО, а не
            // по случайному результату цветовой кластеризации.
            Set<BlockType> merged = new LinkedHashSet<>(byLine.values().iterator().next());
            merged.addAll(shapeMembers);
            return List.of(toBundle(merged, weights));
        }

        // 2+ палитровых линии в одной базовой группе (Проблема 4, например
        // wool vs concrete) — намеренно НЕ разрешаем здесь. Каждая линия —
        // свой бандл, формы без палитрового суффикса — отдельный бандл.
        // Их взаимное разрешение остаётся за FullBlockPaletteLineSplitter,
        // который видит это уже после цветовой кластеризации, как и раньше.
        List<MaterialFamilyBundle> bundles = new ArrayList<>(byLine.size() + 1);
        for (Set<BlockType> line : byLine.values()) {
            bundles.add(toBundle(line, weights));
        }
        if (!shapeMembers.isEmpty()) {
            bundles.add(toBundle(shapeMembers, weights));
        }
        return bundles;
    }

    private MaterialFamilyBundle toBundle(Set<BlockType> members, Map<BlockType, Long> weights) {
        BlockType representative = null;
        long maxWeight = Long.MIN_VALUE;
        long totalWeight = 0L;

        for (BlockType member : members) {
            long weight = weights.getOrDefault(member, 0L);
            totalWeight += weight;
            if (weight > maxWeight) {
                maxWeight = weight;
                representative = member;
            }
        }

        return new MaterialFamilyBundle(representative, Set.copyOf(members), totalWeight);
    }

    /**
     * @param representativeType самый тяжёлый (по весу) член бандла — его Lab-цвет
     *                           будет использован вызывающей стороной для цветовой
     *                           кластеризации, тем же принципом "лидер по весу",
     *                           что и раньше в {@code DominantColorExtractor}.
     */
    public record MaterialFamilyBundle(BlockType representativeType, Set<BlockType> members, long totalWeight) {
    }
}