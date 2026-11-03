package com.almarel.orion.recolor.palette;

import com.google.inject.Singleton;

import java.util.List;

/**
 * Единая точка выбора "якоря" гармонической схемы — самой весомой группы
 * СРЕДИ ХРОМАТИЧЕСКИХ (saturation выше порога). Ахроматичные группы (белое/
 * серое/чёрное стекло, бетон и т.п. — saturation == 0 ровно, см.
 * ColorSpaceConverter.rgbToHsl: при max==min Hue/Saturation всегда 0.0)
 * не могут осмысленно участвовать в повороте Hue — вращать "оттенок серого"
 * не даёт визуального эффекта. До этого фикса анкором мог случайно стать
 * ахроматичный кластер просто потому, что занимал больше блоков в регионе
 * (например стеклянная банка тяжелее по объёму, чем её крышка) — тогда вся
 * гармоническая схема вырождалась: сам анкор визуально не менялся, а смещения
 * остальных групп считались от Hue=0 "заглушки", а не от реального цвета.
 * <p>
 * Используется консистентно во всех местах, где нужен анкор —
 * {@link PaletteVariantGenerator} (полный расчёт всех пресетов) и
 * {@code RecolorMenuPresenter} (резолв по клику на конкретную иконку).
 * Раньше оба места независимо брали {@code groups.get(0)} — дублирование
 * одной и той же (ошибочной) бизнес-логики в двух местах.
 */
@Singleton
public class DominantGroupAnchorSelector {

    /**
     * Порог, ниже которого группа считается "ахроматичной" (белый/серый/чёрный).
     * ColorSpaceConverter.rgbToHsl даёт РОВНО 0.0 для истинно серых цветов
     * (r==g==b) — небольшой запас взят для почти-серых материалов (светлый
     * камень и т.п.), которые формально чуть окрашены, но гармонию не несут.
     */
    private static final double ACHROMATIC_SATURATION_THRESHOLD = 0.05;

    /**
     * @param groups доминирующие группы постройки, отсортированные по убыванию веса
     *               (контракт {@link DominantColorExtractor#extract} — сортировка по totalCount)
     * @return самая весомая ХРОМАТИЧЕСКАЯ группа; если таких нет вовсе
     *         (вся постройка ахроматична) — самая весомая группа без разбора,
     *         гармонизировать в этом вырожденном случае нечего.
     */
    public DominantColorGroup selectAnchor(List<DominantColorGroup> groups) {
        return groups.stream()
                .filter(this::isChromatic)
                .findFirst() // groups уже отсортирован по убыванию веса — первый проходящий фильтр самый тяжёлый
                .orElseGet(() -> groups.get(0));
    }

    public boolean isChromatic(DominantColorGroup group) {
        return group.representativeHsl().saturation() >= ACHROMATIC_SATURATION_THRESHOLD;
    }

    public boolean isAchromatic(DominantColorGroup group) {
        return !isChromatic(group);
    }
}