package com.almarel.orion.recolor.palette;

import java.util.List;

/**
 * Фиксированный набор классических схем круга Иттена, используемых в
 * режиме "предложи варианты без клика" (справка, раздел 6.7).
 * Отрицательные варианты (Triadic -120° и т.п.) дают зеркальную версию
 * той же схемы — обе стороны круга одинаково валидны.
 */
public final class HarmonySchemePresets {

    public static final List<HarmonyPreset> ALL = List.of(
            new HarmonyPreset("Complementary", 180.0),
            new HarmonyPreset("Triadic (+)", 120.0),
            new HarmonyPreset("Triadic (-)", -120.0),
            new HarmonyPreset("Split-Complementary (+)", 150.0),
            new HarmonyPreset("Split-Complementary (-)", -150.0),
            new HarmonyPreset("Analogous (+)", 30.0),
            new HarmonyPreset("Analogous (-)", -30.0)
    );

    private HarmonySchemePresets() {
        // Static holder, no instances.
    }
}