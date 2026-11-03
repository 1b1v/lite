package com.almarel.orion.curve;

import com.almarel.orion.curve.data.*;
import com.almarel.orion.mapping.block.BlockVariantMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;

import java.util.List;

/**
 * Single Source of Truth для разбора аргументов всех curve-команд
 * (bridge, stream, liana, ribbon и будущих кривых). Устраняет дублирование
 * parseDoubleArg/parseIntArg/isNumeric между Command-классами и централизует
 * дефолтные значения параметров каждой кривой.
 */
@Singleton
public class CurveArgsParser {

    private static final String SLAB_SUFFIX = "_slab";
    private static final String STAIRS_SUFFIX = "_stairs";

    private static final String BRIDGE_DEFAULT_PATTERN = "spruce_slab,oak_slab";
    private static final double BRIDGE_DEFAULT_SIZE = 14.0;
    private static final double BRIDGE_DEFAULT_SAG = -10.0;

    private static final double STREAM_DEFAULT_WIDTH = 5.0;
    private static final String STREAM_DEFAULT_PATTERN = "gravel";
    private static final int STREAM_DEFAULT_DEPTH = 2;
    private static final double STREAM_DEFAULT_SAG = 0.0;

    private static final String LIANA_DEFAULT_PATTERN = "dark_oak_leaves,sunflower,fern";
    private static final double LIANA_DEFAULT_SAG = 40.0;
    private static final int LIANA_DEFAULT_STEM_LENGTH = 10;
    private static final int LIANA_DEFAULT_LIANA_COUNT = 2;

    private static final String RIBBON_DEFAULT_THEME = "red";
    private static final double RIBBON_DEFAULT_SAG = 40.0;

    private final BlockVariantMapper blockVariantMapper;

    @Inject
    public CurveArgsParser(BlockVariantMapper blockVariantMapper
    ) {
        this.blockVariantMapper = blockVariantMapper;
    }

    // ==========================================
    // BRIDGE
    // ==========================================


    // ==========================================
    // STREAM
    // ==========================================


    // ==========================================
    // LIANA
    // ==========================================

    public LianaCommandArgs parseLiana(String[] args, ParserContext context) throws Exception {
        boolean hasCustomPattern = args.length > 0 && !isNumeric(args[0]);
        String patternInput = hasCustomPattern ? args[0] : LIANA_DEFAULT_PATTERN;
        int argOffset = hasCustomPattern ? 1 : 0;

        LianaPatterns patterns = resolveLianaPatterns(patternInput, context);

        double sag = parseDoubleArg(args, argOffset, LIANA_DEFAULT_SAG, "sag");
        int stemLength = Math.max(0, parseIntArg(args, argOffset + 1, LIANA_DEFAULT_STEM_LENGTH, "stem length"));
        int lianaCount = Math.max(1, parseIntArg(args, argOffset + 2, LIANA_DEFAULT_LIANA_COUNT, "liana count"));

        return new LianaCommandArgs(
                patterns.base(),
                patterns.stem(),
                patterns.tip(),
                sag,
                stemLength,
                lianaCount
        );
    }

    private LianaPatterns resolveLianaPatterns(String patternInput, ParserContext context) throws Exception {
        List<String> patternParts = blockVariantMapper.splitPatternArg(patternInput);

        Pattern basePattern = CurveCommandUtils.parsePattern(patternParts.get(0), context);
        Pattern stemPattern = patternParts.size() > 1
                ? CurveCommandUtils.parsePattern(patternParts.get(1), context)
                : basePattern;
        Pattern tipPattern = patternParts.size() > 2
                ? CurveCommandUtils.parsePattern(patternParts.get(2), context)
                : null;

        return new LianaPatterns(basePattern, stemPattern, tipPattern);
    }

    private record LianaPatterns(Pattern base, Pattern stem, Pattern tip) {}

    // ==========================================
    // RIBBON
    // ==========================================


    // ==========================================
    // SHARED PRIMITIVES
    // ==========================================

    public boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private double parseDoubleArg(String[] args, int index, double defaultValue, String paramName) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for " + paramName + ": " + args[index]);
        }
    }

    private int parseIntArg(String[] args, int index, int defaultValue, String paramName) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for " + paramName + ": " + args[index]);
        }
    }
}