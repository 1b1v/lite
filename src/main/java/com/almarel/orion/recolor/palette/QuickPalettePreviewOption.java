package com.almarel.orion.recolor.palette;

import com.almarel.orion.mapping.visual.RgbColor;

/**
 * Одна "быстрая" опция палитры для верхней строки GUI recolor (3 иконки
 * кожаной брони). Содержит только превью-цвет якорной группы ПОСЛЕ
 * поворота Hue — без поиска реального блока-кандидата (это тяжёлый путь,
 * выполняется отдельно, только по клику, см. PaletteVariantGenerator.resolveVariantTargets).
 */
public record QuickPalettePreviewOption(HarmonyPreset preset, RgbColor previewColor) {
}