package com.almarel.orion.brush.presentation.parsing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.enums.BrushRotationMode;
import com.almarel.orion.framework.message.MessageManager;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class BrushArgsParser {

    private final MessageManager messageManager;

    // --- Single Source of Truth for Tab-Completion ---
    public static final List<String> AVAILABLE_FLAGS = List.of(
            "-a",  // Angle
            "-v",  // View
            "-w",  // 16-point
            "-r",  // Random
            "-z",  // Z-axis air only
            "-ar", // Angle Roll
            "-i"   // Ignore/Include mask
    );

    @Inject
    public BrushArgsParser(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public BrushParseFlags parse(String[] args, int startIndex) throws IllegalArgumentException {
        if (args == null || startIndex >= args.length) {
            return buildDefaultFlags();
        }

        BrushParseState state = new BrushParseState();

        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("-i")) {
                i = processIgnoreFlag(args, i, state);
            } else if (arg.equalsIgnoreCase("-ar")) {
                i = processRollFlag(args, i, state);
            } else if (arg.startsWith("-")) {
                i = processCombinedFlags(args, i, arg, state);
            }
        }

        return state.toBrushFlags();
    }

    // --- Extracted Processing Methods ---

    private int processIgnoreFlag(String[] args, int currentIndex, BrushParseState state) {
        if (currentIndex + 1 >= args.length) {
            throw new IllegalArgumentException("Flag -i requires a block list (e.g., -i stone,dirt or -i !stone,dirt)");
        }

        String maskArg = args[currentIndex + 1];
        IgnoreMaskData maskData = parseIgnoreMask(maskArg);

        state.rawIgnoreMaterials = maskData.materials();
        state.ignoreWhitelist = maskData.isWhitelist();

        return currentIndex + 1; // Consume the next argument
    }

    private int processRollFlag(String[] args, int currentIndex, BrushParseState state) {
        if (currentIndex + 1 >= args.length) {
            throw new IllegalArgumentException(messageManager.get("utils.flag-ar-arg"));
        }

        String rollArg = args[currentIndex + 1];
        state.rollDeg = parseRollDegree(rollArg);

        return currentIndex + 1; // Consume the next argument
    }

    private int processCombinedFlags(String[] args, int currentIndex, String arg, BrushParseState state) {
        String flags = arg.substring(1);
        if (flags.isEmpty()) return currentIndex;

        for (char c : flags.toCharArray()) {
            switch (c) {
                case 'a' -> state.rotationMode = BrushRotationMode.ANGLE;
                case 'v' -> state.rotationMode = BrushRotationMode.VIEW;
                case 'w' -> state.rotationMode = BrushRotationMode.SIXTEEN;
                case 'r' -> state.rotationMode = BrushRotationMode.RANDOM;
                case 'z' -> state.zOnlyAir = true;
                default -> throw new IllegalArgumentException(
                        messageManager.get("utils.unknown-flag").replace("{0}", String.valueOf(c))
                );
            }
        }

        // Implicit smooth iteration check (e.g. `-a 5`)
        if (state.rotationMode == BrushRotationMode.ANGLE && currentIndex + 1 < args.length) {
            String nextArg = args[currentIndex + 1];
            if (nextArg.matches("^\\d+$")) {
                state.smoothIterations = parseImplicitSmoothIteration(nextArg);
                return currentIndex + 1; // Consume the extra number argument
            }
        }

        return currentIndex;
    }

    // --- Raw Parsers ---

    private IgnoreMaskData parseIgnoreMask(String maskArg) {
        boolean isWhitelist = maskArg.startsWith("!");
        if (isWhitelist) {
            maskArg = maskArg.substring(1);
        }

        if (maskArg.isEmpty()) {
            throw new IllegalArgumentException("Block list cannot be empty after !");
        }

        List<String> materials = new ArrayList<>();
        for (String name : maskArg.split(",")) {
            materials.add(name.toUpperCase());
        }
        return new IgnoreMaskData(materials, isWhitelist);
    }

    private int parseRollDegree(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(messageManager.get("utils.flag-ar-error"));
        }
    }

    private int parseImplicitSmoothIteration(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BrushParseFlags buildDefaultFlags() {
        return new BrushParseFlags(
                BrushRotationMode.OFF, false, null, false, null, false,
                null, null, null, 0, false, null, 0
        );
    }

    // --- Local Data Structures ---

    private record IgnoreMaskData(List<String> materials, boolean isWhitelist) {}

    /**
     * Mutable state holder to keep track of parsed values during iteration.
     */
    private static class BrushParseState {
        BrushRotationMode rotationMode = BrushRotationMode.OFF;
        boolean zOnlyAir = false;
        Integer rollDeg = null;
        int smoothIterations = 0;
        List<String> rawIgnoreMaterials = null;
        boolean ignoreWhitelist = false;

        BrushParseFlags toBrushFlags() {
            return new BrushParseFlags(
                    rotationMode, zOnlyAir, rollDeg, false, rawIgnoreMaterials, ignoreWhitelist,
                    null, null, null, smoothIterations, false, null, 0
            );
        }
    }
}