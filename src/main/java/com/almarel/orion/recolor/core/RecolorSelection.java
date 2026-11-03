package com.almarel.orion.recolor.core;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

/**
 * Снимок текущего WorldEdit-выделения игрока: мир + регион.
 * Используется и GUI (Apply, открытие меню), и debug-командой — единая точка
 * получения selection, чтобы не дублировать работу с LocalSession в двух местах.
 */
public record RecolorSelection(World world, Region region) {
}