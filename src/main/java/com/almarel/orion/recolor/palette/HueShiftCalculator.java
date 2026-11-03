package com.almarel.orion.recolor.palette;

import com.google.inject.Singleton;

/**
 * Чистая математика поворота по кругу Hue (0-360°, круговая метрика).
 * Сознательно не зависит ни от HslColor, ни от FAWE/WorldEdit — работает
 * с "сырыми" градусами. Это единственный компонент палитрового трека,
 * который не требует знания сигнатур других классов проекта, поэтому
 * реализован первым и может быть покрыт юнит-тестами без единого мока.
 *
 * Применяется в двух сценариях (см. справку, раздел 6.7):
 * 1. Игрок кликом меняет цвет одной группы ("якорь") — anchorDelta
 *    вычисляется из реального изменения хью якоря.
 * 2. Система сама предлагает N вариантов без участия игрока — anchorDelta
 *    берётся из фиксированного канонического угла (CANONICAL_HARMONY_ANGLES).
 * Оба сценария используют один и тот же applyHarmonicShift(...).
 */
@Singleton
public class HueShiftCalculator {

    /**
     * Канонические углы гармонии круга Иттена, в градусах.
     * Знак не хранится здесь намеренно — используется абсолютное
     * расстояние, сторона смещения (+/-) берётся от исходного offset.
     */
    private static final double[] CANONICAL_HARMONY_ANGLES = {30.0, 120.0, 150.0, 180.0};

    /**
     * Сила притяжения к канонической гармонии при блендинге:
     * 0.0 — чистый перенос относительного смещения без изменений
     *       (строго "как было", может быть негармонично);
     * 1.0 — полный снап к ближайшему каноническому углу (жёсткая схема,
     *       теряется индивидуальность исходной палитры).
     * 0.5 — компромисс по умолчанию. Финальная калибровка — эмпирика,
     * вынесена константой намеренно, не конфигом: тюнинг качества палитр
     * ожидается нечастым и точечным, не пользовательской настройкой.
     */
    private static final double DEFAULT_SNAP_STRENGTH = 0.5;

    /**
     * Кратчайшее знаковое расстояние между двумя хью на круге 0-360.
     * Результат в диапазоне (-180, 180].
     */
    public double shortestDelta(double fromHueDegrees, double toHueDegrees) {
        double raw = normalizeHueDegrees(toHueDegrees - fromHueDegrees);
        return raw > 180.0 ? raw - 360.0 : raw;
    }

    /**
     * Главный метод: считает новое относительное положение НЕ-якорной
     * группы на круге, после того как якорь повернулся на anchorDelta.
     *
     * Алгоритм намеренно НЕ делает жёсткий перенос той же дельты всем
     * группам (это сохраняло бы изначальные, возможно случайные, углы
     * между цветами построики без улучшения гармонии). Вместо этого:
     * relativeOffset группы подтягивается к ближайшему каноническому
     * углу гармонии (снэп), результат блендится с оригиналом в силе
     * snapStrength — отклонения от "идеала" это нормально и ожидаемо,
     * цель — выглядеть красиво, а не механически соблюсти формулу.
     *
     * @param relativeOffset исходное смещение хью группы относительно
     *                       хью якоря ДО изменения (signed, -180..180)
     * @param anchorDelta    на сколько градусов реально повернулся якорь
     * @return новое относительное смещение группы относительно НОВОГО
     *         положения якоря (signed, -180..180)
     */
    public double applyHarmonicShift(double relativeOffset, double anchorDelta) {
        return applyHarmonicShift(relativeOffset, anchorDelta, DEFAULT_SNAP_STRENGTH);
    }

    public double applyHarmonicShift(double relativeOffset, double anchorDelta, double snapStrength) {
        double snappedToCanonical = snapToNearestCanonicalAngle(relativeOffset);
        double blended = lerp(relativeOffset, snappedToCanonical, clamp01(snapStrength));
        return normalizeSigned(blended + anchorDelta);
    }

    /**
     * Абсолютный хью-угол (0-360) из относительного смещения и опорного
     * хью якоря. Удобный хелпер, чтобы вызывающий код не дублировал
     * normalize(anchorHue + offset) в каждом месте использования.
     */
    public double resolveAbsoluteHue(double anchorHueDegrees, double relativeOffset) {
        return normalizeHueDegrees(anchorHueDegrees + relativeOffset);
    }

    /**
     * Находит ближайший канонический угол гармонии к заданному смещению,
     * сохраняя знак исходного смещения (сторона круга не меняется —
     * группа, стоявшая "левее" якоря, после снэпа останется левее).
     */
    private double snapToNearestCanonicalAngle(double relativeOffset) {
        double absOffset = Math.abs(relativeOffset);
        double sign = Math.signum(relativeOffset);
        if (sign == 0.0) {
            sign = 1.0;
        }

        double nearest = CANONICAL_HARMONY_ANGLES[0];
        double smallestDiff = Math.abs(absOffset - nearest);

        for (double candidate : CANONICAL_HARMONY_ANGLES) {
            double diff = Math.abs(absOffset - candidate);
            if (diff < smallestDiff) {
                smallestDiff = diff;
                nearest = candidate;
            }
        }

        return sign * nearest;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Нормализует произвольный угол в диапазон [0, 360). Публичный —
     * переиспользуется другими компонентами палитрового трека
     * (PaletteVariantGenerator), чтобы не дублировать эту логику.
     */
    public double normalizeHueDegrees(double degrees) {
        double result = degrees % 360.0;
        return result < 0 ? result + 360.0 : result;
    }

    private double normalizeSigned(double degrees) {
        double result = normalizeHueDegrees(degrees);
        return result > 180.0 ? result - 360.0 : result;
    }
}