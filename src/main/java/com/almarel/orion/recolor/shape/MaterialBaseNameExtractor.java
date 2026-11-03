package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Singleton
public class MaterialBaseNameExtractor {

    private static final String[] FULL_BLOCK_SUFFIXES = {
            "_concrete_powder", "_stained_glass_pane", "_stained_glass",
            "_terracotta", "_concrete", "_planks", "_wool", "_carpet", "_bricks", "_block"
    };

    /** FULL_BLOCK_SUFFIXES, отсортированы по убыванию длины — для matchPaletteLineSuffix. */
    private static final String[] SORTED_FULL_BLOCK_SUFFIXES;

    static {
        List<String> sorted = new ArrayList<>(List.of(FULL_BLOCK_SUFFIXES));
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        SORTED_FULL_BLOCK_SUFFIXES = sorted.toArray(String[]::new);
    }

    /** Отсортированы по убыванию длины (сначала длинные, потом короткие) */
    private final String[] knownSuffixes;

    @Inject
    public MaterialBaseNameExtractor(ShapeSuffixCandidateGenerator suffixGenerator) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(List.of(FULL_BLOCK_SUFFIXES));
        merged.addAll(suffixGenerator.getAllSuffixes());

        List<String> sorted = new ArrayList<>(merged);
        sorted.sort(Comparator.comparingInt(String::length).reversed());

        this.knownSuffixes = sorted.toArray(String[]::new);
    }

    public String extractBaseName(BlockType type) {
        String name = extractName(type);

        for (String suffix : knownSuffixes) {
            if (name.length() > suffix.length() && name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }

        return name;
    }

    /**
     * Определяет "палитровую линию" FULL_BLOCK-материала — одно из известных
     * семейств с 16 цветовыми вариациями ваниллы (concrete / wool / terracotta /
     * stained_glass / ...). Используется, чтобы НЕ схлопывать разные линии
     * одного приблизительного цвета (например brown_concrete и brown_wool) в
     * единственный целевой блок — каждая линия должна сохранить свою текстуру
     * при перекраске (см. справку, Проблема 4: банка из стекла и крышки —
     * concrete+wool не должны терять одну из двух фактур).
     * <p>
     * В отличие от {@link #extractBaseName}, проверяет ТОЛЬКО палитровые
     * суффиксы (не смешивает их с shape-суффиксами вроде "_stairs") — формы
     * (ступеньки/заборы/...) этим методом намеренно не распознаются, для них
     * вернётся {@link Optional#empty()}.
     */
    public Optional<String> matchPaletteLineSuffix(BlockType type) {
        String name = extractName(type);

        for (String suffix : SORTED_FULL_BLOCK_SUFFIXES) {
            if (name.length() > suffix.length() && name.endsWith(suffix)) {
                return Optional.of(suffix);
            }
        }

        return Optional.empty();
    }

    /**
     * Возвращает префикс namespace С двоеточием, например "minecraft:".
     * (Текущее API генератора кандидатов ожидает именно такой формат.)
     */
    public String extractNamespace(BlockType type) {
        String id = type.id();
        int colon = id.indexOf(':');
        return (colon >= 0) ? id.substring(0, colon + 1) : "minecraft:";
    }

    private String extractName(BlockType type) {
        String id = type.id();
        int colon = id.indexOf(':');
        return (colon >= 0) ? id.substring(colon + 1) : id;
    }
}