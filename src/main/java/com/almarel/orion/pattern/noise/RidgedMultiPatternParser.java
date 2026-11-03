package com.almarel.orion.pattern.noise;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.math.random.SimpleRandom;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.math.noise.RidgedMultiFractalNoise;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.almarel.orion.framework.data.OrionPermissions;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class RidgedMultiPatternParser extends RichParser<Pattern> {

    public RidgedMultiPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#ridgedmulti");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (!hasPermission(context)) {
            return Stream.empty();
        }

        if (index == 0) {
            return SuggestionHelper.suggestPositiveDoubles(argumentInput);
        } else if (index == 1) {
            return worldEdit.getPatternFactory().getSuggestions(argumentInput, context).stream();
        }
        return Stream.empty();
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (!matchesThisPrefix(input)) {
            return super.parseFromInput(input, context);
        }

        if (!hasPermission(context)) {
            throw new InputParseException("No permission for #ridgedmulti pattern.");
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

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        validateArgumentCount(arguments);

        double scale = parseScale(arguments[0]);
        Pattern inner = worldEdit.getPatternFactory().parseFromInput(arguments[1], context);

        RidgedMultiFractalNoise noise = createNoiseGenerator();
        SimpleRandom random = new NormalizedRidgedRandom(noise, scale);

        return buildRidgedPattern(inner, random, context);
    }

    private void validateArgumentCount(String[] arguments) throws InputParseException {
        if (arguments.length != 2) {
            throw new InputParseException("Usage: #ridgedmulti[size][pattern] (e.g. #ridgedmulti[5][dirt,stone])");
        }
    }

    private double parseScale(String argument) throws InputParseException {
        try {
            double size = Double.parseDouble(argument);
            return 1.0 / Math.sqrt(Math.max(1.0, size));
        } catch (NumberFormatException e) {
            throw new InputParseException("Invalid size: " + argument);
        }
    }

    private RidgedMultiFractalNoise createNoiseGenerator() {
        RidgedMultiFractalNoise noise = new RidgedMultiFractalNoise();
        noise.setFrequency(0.15);
        noise.setOctaveCount(1);
        return noise;
    }

    private Pattern buildRidgedPattern(Pattern inner, SimpleRandom random, ParserContext context) throws InputParseException {
        if (inner instanceof RandomPattern randomPattern) {
            return new RandomPattern(random, randomPattern);
        }
        if (inner instanceof BlockStateHolder) {
            return wrapSingleBlock(inner, random, context);
        }
        throw new InputParseException("Pattern " + inner.getClass().getSimpleName() + " cannot be used with #ridgedmulti");
    }

    private Pattern wrapSingleBlock(Pattern blockPattern, SimpleRandom random, ParserContext context) {
        // Если введен 1 блок: добавляем #existing для впадин, чтобы отображались только хребты
        RandomPattern rp = new RandomPattern(random);
        try {
            Pattern existing = worldEdit.getPatternFactory().parseFromInput("#existing", context);
            rp.add(existing, 1.0); // Занимает нижнюю половину спектра шума (впадины)
        } catch (InputParseException ignored) {
            // Фоллбэк, если #existing недоступен
        }
        rp.add(blockPattern, 1.0); // Занимает верхнюю половину спектра шума (хребты)
        return rp;
    }

    private boolean hasPermission(ParserContext context) {
        return context.getActor() == null || context.getActor().hasPermission(OrionPermissions.PATTERN_RIDGEDMULTI);
    }

    /**
     * Нормализатор шума для 1-октавного RidgedMulti.
     * Эмпирически выверенная формула: сырой шум дает пик ~0.6, делим на 0.65 с запасом.
     * Диапазон [0.0, ~0.65] переходит в корректный [0.0, 1.0],
     * что позволяет RandomPattern корректно распределять 3+ блоков.
     */
    private record NormalizedRidgedRandom(NoiseGenerator generator, double scale) implements SimpleRandom {
        @Override
        public double nextDouble(int x, int y, int z) {
            // ОПТИМИЗАЦИЯ: Умножаем примитивы до создания вектора.
            // Это избавляет от создания второго объекта в методе .multiply()
            double raw = generator.noise(Vector3.at(x * scale, y * scale, z * scale));

            double normalized = raw / 0.65;

            // Жесткая защита от погрешностей вычислений с плавающей точкой
            if (normalized >= 1.0) return 0.9999999999999999;
            if (normalized < 0.0) return 0.0;

            return normalized;
        }
    }
}