package com.almarel.orion.recolor.palette;

import com.sk89q.worldedit.world.block.BlockType;

import java.util.Map;

/**
 * Один готовый вариант палитровой перекраски всей постройки.
 * <p>
 * desiredTargets имеет ТОЧНО ТОТ ЖЕ формат, что и ручной выбор игрока
 * в GUI recolor ({@code RecolorMenuPresenter.collectDesiredTargets}) —
 * это позволяет скормить его напрямую в уже существующий
 * {@code RecolorMapBuilder.build(...)} без каких-либо адаптеров.
 *
 * @param schemeName      имя схемы гармонии (для будущего отображения в GUI)
 * @param desiredTargets  сырые (не shape-резолвленные) целевые материалы по исходным BlockType
 */
public record PaletteVariant(String schemeName, Map<BlockType, BlockType> desiredTargets) {
}