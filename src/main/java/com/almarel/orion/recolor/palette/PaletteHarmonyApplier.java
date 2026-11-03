package com.almarel.orion.recolor.palette;

import com.almarel.orion.mapping.visual.HslColor;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Пересчитывает целевые HSL-цвета для всех доминирующих групп постройки,
 * когда одна из них ("якорь") меняет цвет.
 * <p>
 * Не делает жёсткий перенос одной и той же дельты всем группам — это
 * сохраняло бы изначальные (возможно случайные) углы между цветами
 * постройки без улучшения гармонии. Вместо этого каждая не-якорная
 * группа мягко подтягивается к ближайшему каноническому углу гармонии
 * круга Иттена через {@link HueShiftCalculator#applyHarmonicShift}.
 * <p>
 * Saturation/lightness каждой группы намеренно не трогаются на этом
 * этапе — первая рабочая версия ограничена поворотом Hue. Лёгкая
 * коррекция светлоты ("сделать чуть темнее/светлее") — следующая
 * итерация поверх уже проверенного ядра.
 */
@Singleton
public class PaletteHarmonyApplier {

    private final HueShiftCalculator hueShiftCalculator;

    @Inject
    public PaletteHarmonyApplier(HueShiftCalculator hueShiftCalculator) {
        this.hueShiftCalculator = hueShiftCalculator;
    }

    /**
     * @param allGroups    все доминирующие группы постройки (включая якорь)
     * @param anchorGroup  группа, чей цвет был изменён (должна быть элементом allGroups)
     * @param newAnchorHsl новый цвет якоря — итоговый, без дополнительной обработки
     * @return новый целевой HSL-цвет для каждой группы, включая якорь
     */
    public Map<DominantColorGroup, HslColor> computeTargetColors(
            List<DominantColorGroup> allGroups,
            DominantColorGroup anchorGroup,
            HslColor newAnchorHsl) {

        double anchorDelta = hueShiftCalculator.shortestDelta(
                anchorGroup.representativeHsl().hue(), newAnchorHsl.hue());

        Map<DominantColorGroup, HslColor> result = new LinkedHashMap<>();
        result.put(anchorGroup, newAnchorHsl);

        for (DominantColorGroup group : allGroups) {
            if (group == anchorGroup) {
                continue;
            }
            result.put(group, computeShiftedColor(group, anchorGroup, anchorDelta, newAnchorHsl));
        }

        return result;
    }

    private HslColor computeShiftedColor(DominantColorGroup group,
                                         DominantColorGroup anchorGroup,
                                         double anchorDelta,
                                         HslColor newAnchorHsl) {
        double originalOffset = hueShiftCalculator.shortestDelta(
                anchorGroup.representativeHsl().hue(), group.representativeHsl().hue());

        double newOffset = hueShiftCalculator.applyHarmonicShift(originalOffset, anchorDelta);
        double newHue = hueShiftCalculator.resolveAbsoluteHue(newAnchorHsl.hue(), newOffset);

        return new HslColor(newHue, group.representativeHsl().saturation(), group.representativeHsl().lightness());
    }
}