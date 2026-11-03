package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Разделяет memberBlockTypes одной DominantColorGroup на независимо
 * резолвящиеся "палитровые линии" FULL_BLOCK-материалов (concrete / wool /
 * terracotta / ...), когда в группе встретилось НЕСКОЛЬКО таких линий
 * одновременно — чтобы перекраска не схлопывала, например, brown_concrete
 * и brown_wool в единственный материал, теряя одну из двух фактур
 * (см. справку, Проблема 4).
 * <p>
 * Если среди FULL_BLOCK-членов группы только ОДНА линия (типичный случай —
 * доски одного дерева + их формы: ступеньки/заборы/ворота/люк) — возвращает
 * единственный кластер, равный ВСЕЙ группе целиком, что сохраняет поведение
 * {@link MaterialFamilyAnchorSelector} из Проблемы 1 без изменений.
 * <p>
 * Форменные (не-FULL_BLOCK) члены группы при конфликте линий присоединяются
 * к "основной" линии — той, что содержит representativeBlockType группы,
 * либо, если представитель сам не входит ни в одну линию, к самой
 * многочисленной (детерминированный выбор, не угадывание).
 */
@Singleton
public class FullBlockPaletteLineSplitter {

    private final BlockShapeClassifier shapeClassifier;
    private final MaterialBaseNameExtractor baseNameExtractor;

    @Inject
    public FullBlockPaletteLineSplitter(BlockShapeClassifier shapeClassifier,
                                        MaterialBaseNameExtractor baseNameExtractor) {
        this.shapeClassifier = shapeClassifier;
        this.baseNameExtractor = baseNameExtractor;
    }

    public List<PaletteLineCluster> split(Set<BlockType> memberBlockTypes, BlockType representativeBlockType) {
        Map<String, Set<BlockType>> byLine = new LinkedHashMap<>();
        Set<BlockType> shapeVariantMembers = new LinkedHashSet<>();

        for (BlockType member : memberBlockTypes) {
            if (shapeClassifier.classify(member) == BlockShapeCategory.FULL_BLOCK) {
                String key = baseNameExtractor.matchPaletteLineSuffix(member)
                        .orElseGet(() -> "unmatched:" + member.id()); // нераспознанные FULL_BLOCK никогда не сливаются друг с другом
                byLine.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(member);
            } else {
                shapeVariantMembers.add(member);
            }
        }

        if (byLine.isEmpty()) {
            // Ни одного FULL_BLOCK-члена — вся группа состоит из форм. Маловероятный
            // случай (DominantColorExtractor кластеризует по цвету независимо от формы),
            // но безопасный фоллбэк: один кластер на всю группу.
            return List.of(new PaletteLineCluster(Set.copyOf(memberBlockTypes), Optional.empty()));
        }

        if (byLine.size() == 1) {
            // Единственная палитровая линия — конфликта нет, поведение идентично
            // состоянию до этого патча: вся группа (линия + формы) резолвится одним anchor'ом.
            Map.Entry<String, Set<BlockType>> only = byLine.entrySet().iterator().next();
            Set<BlockType> merged = new LinkedHashSet<>(only.getValue());
            merged.addAll(shapeVariantMembers);
            return List.of(new PaletteLineCluster(Set.copyOf(merged), resolveSuffix(only.getKey())));
        }

        String primaryKey = determinePrimaryKey(byLine, representativeBlockType);

        List<PaletteLineCluster> result = new ArrayList<>(byLine.size());
        for (Map.Entry<String, Set<BlockType>> entry : byLine.entrySet()) {
            Set<BlockType> members = new LinkedHashSet<>(entry.getValue());
            if (entry.getKey().equals(primaryKey)) {
                members.addAll(shapeVariantMembers);
            }
            result.add(new PaletteLineCluster(Set.copyOf(members), resolveSuffix(entry.getKey())));
        }
        return result;
    }

    private Optional<String> resolveSuffix(String key) {
        return key.startsWith("unmatched:") ? Optional.empty() : Optional.of(key);
    }

    private String determinePrimaryKey(Map<String, Set<BlockType>> byLine, BlockType representativeBlockType) {
        for (Map.Entry<String, Set<BlockType>> entry : byLine.entrySet()) {
            if (entry.getValue().contains(representativeBlockType)) {
                return entry.getKey();
            }
        }
        return byLine.entrySet().stream()
                .max((a, b) -> Integer.compare(a.getValue().size(), b.getValue().size()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    public record PaletteLineCluster(Set<BlockType> members, Optional<String> paletteLineSuffix) {
    }
}