package com.almarel.orion.recolor.palette;

import com.almarel.orion.mapping.visual.ColorSpaceConverter;
import com.almarel.orion.mapping.visual.HslColor;
import com.almarel.orion.mapping.visual.RgbColor;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Лёгкий ("превью") путь палитрового трека: только поворот Hue якорной
 * группы + конвертация в RGB, БЕЗ обращения к NearestBlockByColorFinder.
 * Предназначен для мгновенного расчёта при открытии GUI (иконки быстрой
 * палитры в RecolorMenuView) — в отличие от
 * {@link PaletteVariantGenerator#resolveVariantTargets}, который делает
 * тяжёлый поиск реальных блоков-кандидатов и вызывается только по клику.
 * <p>
 * Якорь выбирается через {@link DominantGroupAnchorSelector} — самая
 * весомая ХРОМАТИЧЕСКАЯ группа, а не просто самая тяжёлая по объёму.
 * Это та же логика, что использует {@link PaletteVariantGenerator} и
 * {@code RecolorMenuPresenter} при реальном резолве по клику — без этой
 * консистентности иконка могла бы показывать один превью-цвет (от
 * ошибочно взятого ахроматичного анкора вроде белого стекла), а клик по
 * ней резолвить совершенно другой результат (см. справку, Проблема 3).
 * <p>
 * Чистая CPU-математика, без Bukkit-вызовов — безопасно вызывать
 * синхронно (на том же треде, что и сканирование региона).
 */
@Singleton
public class QuickPalettePreviewCalculator {

    private final HueShiftCalculator hueShiftCalculator;
    private final DominantGroupAnchorSelector groupAnchorSelector;

    @Inject
    public QuickPalettePreviewCalculator(HueShiftCalculator hueShiftCalculator,
                                         DominantGroupAnchorSelector groupAnchorSelector) {
        this.hueShiftCalculator = hueShiftCalculator;
        this.groupAnchorSelector = groupAnchorSelector;
    }

    /**
     * @param groups  доминирующие группы региона (см. DominantColorExtractor);
     *                если пусто (регион без визуально пригодных блоков) —
     *                возвращается пустой список, иконки просто не рисуются
     * @param presets схемы, для которых нужно посчитать превью (порядок сохраняется)
     */
    public List<QuickPalettePreviewOption> calculate(List<DominantColorGroup> groups, List<HarmonyPreset> presets) {
        if (groups.isEmpty()) {
            return List.of();
        }

        DominantColorGroup anchor = groupAnchorSelector.selectAnchor(groups);
        List<QuickPalettePreviewOption> result = new ArrayList<>(presets.size());

        for (HarmonyPreset preset : presets) {
            RgbColor previewColor = computePreviewColor(anchor, preset);
            result.add(new QuickPalettePreviewOption(preset, previewColor));
        }

        return result;
    }

    private RgbColor computePreviewColor(DominantColorGroup anchor, HarmonyPreset preset) {
        double newHue = hueShiftCalculator.normalizeHueDegrees(
                anchor.representativeHsl().hue() + preset.anchorHueDeltaDegrees());

        HslColor previewHsl = new HslColor(
                newHue,
                anchor.representativeHsl().saturation(),
                anchor.representativeHsl().lightness());

        return ColorSpaceConverter.hslToRgb(previewHsl);
    }
}