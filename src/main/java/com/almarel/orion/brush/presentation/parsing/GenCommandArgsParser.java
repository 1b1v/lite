package com.almarel.orion.brush.presentation.parsing;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Arrays;

/**
 * Argument parser specifically for the /br gen command.
 * Parses a string of the format: [Pattern?] [Radius?] [Formula...]
 */
@Singleton
public class GenCommandArgsParser {

    private static final String DEFAULT_FORMULA = "x^2 + y^2 + z^2 < 1";

    @Inject
    public GenCommandArgsParser() {
        // Конструктор теперь пустой
    }

    /**
     * Parsing result.
     * pattern and radius can be null if they were not specified.
     * formula is never null (falls back to default if not specified).
     */
    public record GenArguments(String pattern, Double radius, String formula) {}

    public GenArguments parse(String[] args) {
        if (args.length <= 1) {
            return new GenArguments(null, null, extractFormula(args, 1));
        }

        String firstArg = args[1];
        String pattern = null;
        Double radius = null;
        int formulaStartIndex = 1;

        Double parsedFirst = tryParseDouble(firstArg);

        if (parsedFirst != null) {
            // Case 1: [Radius] [Formula]
            radius = parsedFirst;
            formulaStartIndex = 2;
        } else {
            // firstArg is definitely NOT a radius. Let's check the next argument.
            if (args.length > 2) {
                Double parsedSecond = tryParseDouble(args[2]);
                if (parsedSecond != null) {
                    // Case 2: [Pattern] [Radius] [Formula]
                    pattern = firstArg;
                    radius = parsedSecond;
                    formulaStartIndex = 3;
                } else {
                    // Case 3: [Pattern] [Formula] OR [Formula spaces]
                    // Next arg is not a number. We use a safe heuristic.
                    if (!isLikelyFormula(firstArg)) {
                        pattern = firstArg;
                        formulaStartIndex = 2;
                    }
                }
            } else {
                // Only 1 argument provided after "gen". Is it Pattern or Formula?
                if (!isLikelyFormula(firstArg)) {
                    pattern = firstArg;
                    formulaStartIndex = 2;
                }
            }
        }

        return new GenArguments(pattern, radius, extractFormula(args, formulaStartIndex));
    }

    private Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isLikelyFormula(String arg) {
        // Убрали знак "=", так как FAWE использует его в BlockStates (например: waterlogged=true)
        // Формула почти наверняка содержит неравенства, скобки или математические степени
        return arg.matches(".*[<>()^].*");
    }

    private String extractFormula(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return DEFAULT_FORMULA;
        }

        String joinedArgs = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
        return joinedArgs.replace("\"", "").trim();
    }
}