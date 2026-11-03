package com.almarel.orion.recolor.palette;

import com.almarel.orion.mapping.visual.HslColor;
import com.almarel.orion.mapping.visual.LabColor;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Set;

/**
 * Один "смысловой цвет" постройки — кластер визуально похожих BlockType,
 * взвешенный по объёму (сколько реальных блоков в регионе принадлежит
 * этому кластеру), а не по числу уникальных типов.
 * <p>
 * representativeColor/representativeHsl берутся от САМОГО ТЯЖЁЛОГО члена
 * кластера (не усредняются!) — усреднение Hue математически некорректно
 * из-за цикличности круга (hue=10° и hue=350° усреднились бы в ~180°,
 * хотя визуально это соседние оттенки красного). См. DominantColorExtractor.
 *
 * @param representativeColor  Lab-цвет самого тяжёлого члена кластера
 * @param representativeHsl    HSL-цвет того же члена — основа для гармоничного поворота
 * @param representativeBlockType исходный BlockType, давший представительский цвет
 * @param blockCount            суммарный объём (кол-во блоков) всех членов кластера
 * @param weightFraction        доля этого кластера от всех пригодных для палитры блоков региона (0..1)
 * @param memberBlockTypes      все исходные BlockType, вошедшие в этот кластер
 */
public record DominantColorGroup(
        LabColor representativeColor,
        HslColor representativeHsl,
        BlockType representativeBlockType,
        long blockCount,
        double weightFraction,
        Set<BlockType> memberBlockTypes
) {
}