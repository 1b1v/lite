package com.almarel.orion.math;

import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.SlotTable;
import com.sk89q.worldedit.internal.expression.LocalSlot;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaUtils {

    private static final Pattern NOISE_COMPLEX_PATTERN = Pattern.compile("(perlin|voronoi|ridgedMulti)\\s*\\(\\s*([^,]+),", Pattern.CASE_INSENSITIVE);

    public static String injectRandomSeeds(String formula) {
        Matcher matcher = NOISE_COMPLEX_PATTERN.matcher(formula);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String funcName = matcher.group(1);
            String firstArg = matcher.group(2);
            String fullMatch = matcher.group(0);

            if (firstArg.toLowerCase().contains("gmegabuf")) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
            } else {
                int newSeed = ThreadLocalRandom.current().nextInt(100000);
                String replacement = funcName + "(" + newSeed + ",";
                matcher.appendReplacement(sb, replacement);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static ParsingResult prepare(String formula) {
        String processed = formula.trim();
        boolean hollow = false;
        boolean normalized = true;

        if (processed.contains("-r")) {
            normalized = false;
            processed = processed.replace("-r", "").trim();
        }
        if (processed.contains("-h")) {
            hollow = true;
            processed = processed.replace("-h", "").trim();
        }

        StringBuilder finalExpr = new StringBuilder();
        if (normalized) {
            finalExpr.append("x=x/r y=y/r z=z/r ");
        }
        finalExpr.append(processed);

        return new ParsingResult(finalExpr.toString(), hollow);
    }

    /**
     * [SAFE] Получение значения переменной через SlotTable (FAWE API).
     */
    public static double getVariableSafe(Expression expr, String name) {
        try {
            SlotTable slots = expr.getSlots();
            if (slots == null) return 0;

            // Используем value() вместо getValue(), так как это Interface в твоей версии
            Optional<? extends LocalSlot> slotOpt = slots.getVariable(name);
            if (slotOpt.isPresent()) {
                LocalSlot slot = slotOpt.get();
                return slot.value();
            }
        } catch (Exception e) {
            // Если что-то пошло не так (например, нет переменной), возвращаем 0
        }
        return 0.0;
    }

    public static class ParsingResult {
        public final String expression;
        public final boolean isHollow;

        public ParsingResult(String expression, boolean isHollow) {
            this.expression = expression;
            this.isHollow = isHollow;
        }
    }
}