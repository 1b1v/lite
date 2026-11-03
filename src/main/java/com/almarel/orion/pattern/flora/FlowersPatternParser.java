package com.almarel.orion.pattern.flora;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.pattern.util.WeightedPalette;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class FlowersPatternParser extends RichParser<Pattern> {

    private static final int DEFAULT_SIZE = 5;
    private static final double SCALE_CONSTANT = 0.35;

    private static final List<String> SIZE_SUGGESTIONS = List.of("1", "3", "5", "10", "20");

    // Только красивые слова для подсказок в чате
    private static final List<String> PRESET_SUGGESTIONS = List.of(
            "blue", "blue2", "red", "pink", "orange",
            "yellow", "green", "white", "white2", "brown", "random"
    );

    // Полный список для валидации (чтобы паттерн всё равно работал, если ввести цифру вручную)
    private static final List<String> VALID_PRESETS = List.of(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
            "blue", "blue2", "red", "pink", "orange",
            "yellow", "green", "white", "white2", "brown", "random"
    );

    public FlowersPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#flowers");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (!hasPermission(context)) {
            return Stream.empty();
        }

        if (index == 0) {
            if (argumentInput.contains(",")) {
                return suggestPresetTokens(argumentInput);
            }
            return Stream.concat(
                    SIZE_SUGGESTIONS.stream().filter(s -> s.startsWith(argumentInput)),
                    suggestPresetTokens(argumentInput)
            );
        } else if (index == 1) {
            return suggestPresetTokens(argumentInput);
        }

        return Stream.empty();
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (!matchesThisPrefix(input)) {
            return super.parseFromInput(input, context);
        }

        if (!hasPermission(context)) {
            throw new InputParseException("No permission for #flowers pattern.");
        }

        if (!input.contains("[")) {
            return parseFromInput(new String[0], context);
        }
        return super.parseFromInput(input, context);
    }

    private boolean matchesThisPrefix(String input) {
        int bracketIndex = input.indexOf('[');
        String prefix = bracketIndex != -1 ? input.substring(0, bracketIndex) : input;
        return prefix.equalsIgnoreCase(getPrefix());
    }

    private boolean hasPermission(ParserContext context) {
        return context.getActor() == null || context.getActor().hasPermission(OrionPermissions.PATTERN_FLOWERS);
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        validateArgumentCount(arguments);

        FlowerParameters params = resolveParameters(arguments);
        double frequency = calculateFrequency(params.size());

        return buildPattern(params.presetInput(), frequency);
    }

    // ==========================================
    // MODULE: Suggestions Helper
    // ==========================================

    private Stream<String> suggestPresetTokens(String argumentInput) {
        int lastComma = argumentInput.lastIndexOf(',');
        if (lastComma >= 0) {
            String prefix = argumentInput.substring(0, lastComma + 1);
            String current = argumentInput.substring(lastComma + 1).toLowerCase(Locale.ROOT);
            return PRESET_SUGGESTIONS.stream()
                    .filter(s -> s.startsWith(current))
                    .map(s -> prefix + s);
        }
        return PRESET_SUGGESTIONS.stream().filter(s -> s.startsWith(argumentInput.toLowerCase(Locale.ROOT)));
    }

    // ==========================================
    // MODULE: Argument Validation & Parsing
    // ==========================================

    private void validateArgumentCount(String[] arguments) throws InputParseException {
        if (arguments.length > 2) {
            throw new InputParseException("Usage: #flowers[size][preset] or #flowers[preset]");
        }
    }

    private FlowerParameters resolveParameters(String[] arguments) throws InputParseException {
        if (arguments.length == 0) {
            return new FlowerParameters(DEFAULT_SIZE, null);
        }
        if (arguments.length == 1) {
            return parseSingleArgument(arguments[0]);
        }
        return parseTwoArguments(arguments[0], arguments[1]);
    }

    private FlowerParameters parseSingleArgument(String arg) throws InputParseException {
        if (arg.isEmpty()) {
            throw new InputParseException("Expected size (for example, 5) or color (for example, red)");
        }

        if (arg.matches("\\d+")) {
            return new FlowerParameters(parseSize(arg), null);
        }

        validatePresets(arg);
        return new FlowerParameters(DEFAULT_SIZE, arg);
    }

    private FlowerParameters parseTwoArguments(String sizeArg, String presetArg) throws InputParseException {
        if (sizeArg.isEmpty()) {
            throw new InputParseException("Expected size (for example, 5)");
        }
        if (presetArg.isEmpty()) {
            throw new InputParseException("Expected color (for example, blue)");
        }

        int size = parseSize(sizeArg);
        validatePresets(presetArg);
        return new FlowerParameters(size, presetArg);
    }

    private int parseSize(String input) throws InputParseException {
        try {
            int size = Integer.parseInt(input);
            if (size <= 0) {
                throw new InputParseException("Size must be a positive integer");
            }
            return size;
        } catch (NumberFormatException e) {
            // Эта ошибка "будит" автодополнение Piston'а для первой скобки
            throw new InputParseException("Invalid size: " + input);
        }
    }

    private void validatePresets(String input) throws InputParseException {
        // Разбиваем строку (например, "red,bl") и проверяем каждую часть
        String[] parts = input.split(",", -1);
        for (String part : parts) {
            String trimmed = part.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                throw new InputParseException("The preset name is expected after the comma");
            }
            if (!VALID_PRESETS.contains(trimmed)) {
                // Именно эта ошибка "будит" автодополнение Piston'а для недописанных слов!
                throw new InputParseException("Unknown preset: " + trimmed);
            }
        }
    }

    // ==========================================
    // MODULE: Pattern Construction
    // ==========================================

    private double calculateFrequency(int size) {
        return SCALE_CONSTANT / size;
    }

    private Pattern buildPattern(String presetInput, double frequency) {
        FlowerPreset.PaletteSet palettes = FlowerPreset.fromInputCombined(presetInput);

        return new FlowersPattern(
                frequency,
                new WeightedPalette(palettes.grass()),
                new WeightedPalette(palettes.flowers()),
                new WeightedPalette(palettes.tallGrass())
        );
    }

    private record FlowerParameters(int size, String presetInput) {}
}