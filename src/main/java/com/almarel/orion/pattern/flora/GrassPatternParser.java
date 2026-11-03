package com.almarel.orion.pattern.flora;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.framework.data.OrionPermissions;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class GrassPatternParser extends RichParser<Pattern> {

    private final AsyncExecutor asyncExecutor;

    public GrassPatternParser(WorldEdit worldEdit, AsyncExecutor asyncExecutor) {
        super(worldEdit, "#grass");
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (!hasPermission(context)) {
            return Stream.empty();
        }

        if (index == 0) {
            String input = argumentInput == null ? "" : argumentInput.toLowerCase();
            return Stream.of("moss_block,grass_block", "stone,dirt")
                    .filter(s -> s.startsWith(input));
        }
        return Stream.empty();
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (!matchesThisPrefix(input)) {
            return super.parseFromInput(input, context);
        }

        if (!hasPermission(context)) {
            throw new InputParseException("No permission for #grass pattern.");
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
        return context.getActor() == null || context.getActor().hasPermission(OrionPermissions.PATTERN_GRASS);
    }

    @Override
    protected Pattern parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        GrassConfig config = parseArguments(arguments, context);
        Region region = resolveRegion(context);
        validateVolume(region);

        return new GrassPattern(config.baseBlock, config.edgeBlock, region, asyncExecutor);
    }

    // ==========================================
    // MODULE: Argument Parsing
    // ==========================================

    private GrassConfig parseArguments(String[] arguments, ParserContext context) throws InputParseException {
        validateArgumentCount(arguments);

        GrassConfig config = new GrassConfig();

        if (arguments.length == 1) {
            if (arguments[0].isEmpty()) {
                throw new InputParseException("Аргумент не может быть пустым. Ожидается baseBlock,edgeBlock.");
            }
            parseBlocksArg(arguments[0], config, context);
        }

        return config;
    }

    private void validateArgumentCount(String[] arguments) throws InputParseException {
        if (arguments.length > 1) {
            throw new InputParseException("Usage: #grass or #grass[base,edge]");
        }
    }

    private void parseBlocksArg(String arg, GrassConfig config, ParserContext context) throws InputParseException {
        String[] parts = arg.split(",");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new InputParseException("Format: baseBlock,edgeBlock");
        }
        config.baseBlock = parseBlock(parts[0], context);
        config.edgeBlock = parseBlock(parts[1], context);
    }

    private BlockState parseBlock(String input, ParserContext context) throws InputParseException {
        try {
            return WorldEdit.getInstance().getBlockFactory()
                    .parseFromInput(input, context)
                    .applyBlock(BlockVector3.ZERO)
                    .toBlockState();
        } catch (InputParseException e) {
            throw new InputParseException("Invalid block: " + input + ". " + e.getMessage());
        }
    }

    // ==========================================
    // MODULE: Region & Volume Validation
    // ==========================================

    private Region resolveRegion(ParserContext context) throws InputParseException {
        Region region = context.getSelection();
        if (region == null) {
            throw new InputParseException("#grass requires a region selection to pre-compute.");
        }
        return region;
    }

    /**
     * Pre-check объёма ДО постройки SlopeSmoothingField (D11-паттерн,
     * идентичный SmoothPatternParser.validateVolume).
     */
    private void validateVolume(Region region) throws InputParseException {
        long volume = SlopeSmoothingField.computeVolume(region);
        if (volume > SlopeSmoothingField.MAX_VOLUME) {
            throw new InputParseException("#grass selection too large: " + volume
                    + " voxels (incl. blur padding), max " + SlopeSmoothingField.MAX_VOLUME
                    + ". Reduce selection size.");
        }
    }

    // ==========================================
    // DTO Classes
    // ==========================================

    private static class GrassConfig {
        BlockState baseBlock = BlockTypes.GRASS_BLOCK.getDefaultState();
        BlockState edgeBlock = BlockTypes.MOSS_BLOCK.getDefaultState();
    }
}