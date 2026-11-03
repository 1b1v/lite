package com.almarel.orion.brush.fawe.util;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for formatting WorldEdit patterns and cleaning strings.
 * Centralizes logic previously scattered across WorldEditGateway and Renderers.
 */
public final class BrushFormatter {

    private BrushFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Cleans up a pattern string (removes "minecraft:" prefix).
     *
     * @param name The raw name.
     * @return Cleaned name or null if input is null.
     */
    public static String cleanPatternName(String name) {
        if (name == null) return null;
        return name.replace("minecraft:", "");
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str Input string.
     * @return Capitalized string.
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Резолвит отображаемое имя кисти для заголовка карточки в GUI-селекторе.
     * Вынесено из FaweConstructBrushState/ArceonConstructBrushState (было дублировано дословно).
     */
    public static String resolveHeaderBrushName(com.almarel.orion.brush.data.enums.OrionBrushType brushType,
                                                String rawBrushName,
                                                String[] commandArgs) {
        if (brushType != null && !brushType.name().equalsIgnoreCase("unknown")) {
            return capitalize(rawBrushName);
        }
        if (commandArgs != null && commandArgs.length > 0) {
            return capitalize(commandArgs[0]);
        }
        return "Unknown";
    }

    /**
     * Formats a WorldEdit Pattern into a human-readable string.
     * Coordinates specific formatting strategies based on the pattern type.
     *
     * @param pattern The WorldEdit pattern.
     * @return Formatted string representation.
     */
    public static String formatPattern(Pattern pattern) {
        if (pattern == null) {
            return null;
        }

        String blockFormatted = tryFormatBlockPattern(pattern);
        if (blockFormatted != null) {
            return blockFormatted;
        }

        if (pattern instanceof RandomPattern randomPattern) {
            String randomFormatted = formatRandomPattern(randomPattern);
            if (randomFormatted != null) {
                return randomFormatted;
            }
        }

        return formatFallbackPattern(pattern);
    }

    private static String tryFormatBlockPattern(Pattern pattern) {
        if (pattern instanceof BlockState blockState) {
            return cleanPatternName(blockState.getBlockType().getId());
        }
        if (pattern instanceof BaseBlock baseBlock) {
            return cleanPatternName(baseBlock.getBlockType().getId());
        }
        return null;
    }

    private static String formatRandomPattern(RandomPattern pattern) {
        Map<Pattern, Double> weights = extractRandomPatternWeights(pattern);
        if (weights == null || weights.isEmpty()) {
            return null;
        }

        return weights.keySet().stream()
                .map(BrushFormatter::formatPattern)
                .collect(Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    private static Map<Pattern, Double> extractRandomPatternWeights(RandomPattern pattern) {
        try {
            Field weightsField = RandomPattern.class.getDeclaredField("weights");
            weightsField.setAccessible(true);
            return (Map<Pattern, Double>) weightsField.get(pattern);
        } catch (Exception ignored) {
            // Fallback if reflection fails or field names change in future FAWE versions
            return null;
        }
    }

    private static String formatFallbackPattern(Pattern pattern) {
        String raw = pattern.toString();
        if (raw.startsWith("minecraft:")) {
            return cleanPatternName(raw);
        }
        if (raw.contains("@")) {
            return "Complex";
        }
        return raw;
    }
}