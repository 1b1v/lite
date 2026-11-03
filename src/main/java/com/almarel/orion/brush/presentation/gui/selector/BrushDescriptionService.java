package com.almarel.orion.brush.presentation.gui.selector;

import com.google.inject.Inject;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.Anchorable;
import com.almarel.orion.brush.data.capabilities.Constructable;
import com.almarel.orion.brush.data.presentation.Formulaable;
import com.almarel.orion.brush.data.capabilities.Groupable;
import com.almarel.orion.brush.data.capabilities.Maskable;
import com.almarel.orion.brush.data.capabilities.Patternable;
import com.almarel.orion.brush.data.capabilities.Rangeable;
import com.almarel.orion.brush.data.capabilities.Sizeable;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import com.almarel.orion.framework.gui.configurationlegacy.BrushListGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BrushDescriptionService {

    private static final double DEFAULT_RANGE_DISTANCE = 200.0;
    private static final int FORMULA_CHUNK_SIZE = 40;
    private static final int FORMULA_PREVIEW_LIMIT = 25;

    private static final Pattern PATTERN_GENERATORS = Pattern.compile("\\b(perlin|voronoi|ridgedMulti)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_MATH = Pattern.compile("\\b(rotate|gmegabuf|sin|cos|tan|abs|max|min|sqrt|pow|floor|ceil|round)\\b", Pattern.CASE_INSENSITIVE);

    private final BrushListGuiConfig brushListGuiConfig;

    @Inject
    public BrushDescriptionService(BrushListGuiConfig brushListGuiConfig) {
        this.brushListGuiConfig = brushListGuiConfig;
    }

    public List<String> getParameters(BrushState state, Player player, boolean isExpanded) {
        List<String> lore = new ArrayList<>();

        appendCommonParameters(lore, state);
        appendSpecificParameters(lore, state, isExpanded);

        return lore;
    }

    private void appendCommonParameters(List<String> lore, BrushState state) {
        appendPatternParameter(lore, state);
        appendSizeParameter(lore, state);
        appendMaskParameter(lore, state);
        appendRangeParameter(lore, state);
    }

    private void appendPatternParameter(List<String> lore, BrushState state) {
        if (!(state instanceof Patternable patternable)) {
            return;
        }

        String patternVal = patternable.getPattern();
        if (patternVal == null || patternVal.isEmpty()) {
            patternVal = "stone";
        } else {
            patternVal = BrushFormatter.cleanPatternName(patternVal);
        }

        lore.add(formatTech("Pattern", patternVal));
    }

    private void appendSizeParameter(List<String> lore, BrushState state) {
        if (state instanceof Sizeable sizeable) {
            lore.add(formatTech("Radius", formatDouble(sizeable.getSize())));
        }
    }

    private void appendMaskParameter(List<String> lore, BrushState state) {
        if (!(state instanceof Maskable maskable)) {
            return;
        }

        String maskVal = (maskable.getMask() == null || maskable.getMask().isEmpty())
                ? "none"
                : maskable.getMask();

        lore.add(formatTech("Mask", maskVal));
    }

    private void appendRangeParameter(List<String> lore, BrushState state) {
        if (!(state instanceof Rangeable rangeable)) {
            return;
        }

        double rangeDistance = (rangeable.getRange() > 0)
                ? rangeable.getRange()
                : DEFAULT_RANGE_DISTANCE;

        lore.add(formatTech("Range", (int) rangeDistance));
    }

    private void appendSpecificParameters(List<String> lore, BrushState state, boolean isExpanded) {
        if (state instanceof Formulaable formulaable) {
            addFormula(lore, formulaable.getFormula(), isExpanded);
            return;
        }

        if (state instanceof Constructable constructable && constructable.isConstructMode() && state instanceof Groupable groupable) {
            String group = (groupable.getActiveGroup() != null) ? groupable.getActiveGroup() : "None";
            lore.add("  " + Palette.LORE_META + "Group: " + Palette.BTN_NAV + group);
            return;
        }

        if (state instanceof Anchorable anchorable && anchorable.getAnchorMode() != null) {
            lore.add("  " + brushListGuiConfig.items().getLoreAnchor(anchorable.getAnchorMode()));
        }
    }

    private void addFormula(List<String> lore, String formula, boolean isExpanded) {
        if (formula == null) {
            return;
        }

        if (isExpanded) {
            addExpandedFormula(lore, formula);
        } else {
            addPreviewFormula(lore, formula);
        }
    }

    private void addExpandedFormula(List<String> lore, String formula) {
        lore.add("  " + Palette.LORE_META + "Formula:");
        for (int i = 0; i < formula.length(); i += FORMULA_CHUNK_SIZE) {
            String part = formula.substring(i, Math.min(formula.length(), i + FORMULA_CHUNK_SIZE));
            lore.add("    " + colorizeFormula(part));
        }
    }

    private void addPreviewFormula(List<String> lore, String formula) {
        String preview = formula.length() > FORMULA_PREVIEW_LIMIT
                ? formula.substring(0, FORMULA_PREVIEW_LIMIT) + "..."
                : formula;

        lore.add(formatTech("Formula", colorizeFormula(preview)));
    }

    public String colorizeFormula(String formula) {
        String res = Palette.LORE_TEXT + formula;
        res = PATTERN_GENERATORS.matcher(res).replaceAll(Palette.BTN_NAV + "$1" + Palette.LORE_TEXT);
        res = PATTERN_MATH.matcher(res).replaceAll(Palette.LORE_VAL + "$1" + Palette.LORE_TEXT);

        return res.replace("(", Palette.LORE_META + "(" + Palette.LORE_TEXT)
                .replace(")", Palette.LORE_META + ")" + Palette.LORE_TEXT);
    }

    private String formatTech(String key, Object value) {
        return "  " + Palette.LORE_META + key + ": " + Palette.LORE_TEXT + value;
    }

    private String formatDouble(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        }
        return String.format("%s", val);
    }
}