package com.almarel.orion.recolor.shape;

/**
 * Геометрическое семейство формы блока, используемое для shape-aware
 * remap-движка (recolor-фича): при замене блока на новый материал форма
 * (ступенька/полублок/стена/...) должна сохраняться, меняется только материал.
 *
 * Каждая категория (кроме FULL_BLOCK, NON_CUBE_OTHER, GLASS_PANE и THIN_FOLIAGE)
 * резолвится из готового vanilla Minecraft/FAWE block-тега через
 * {@link com.sk89q.worldedit.world.block.BlockCategory} — это даёт авто-поддержку
 * новых блоков при обновлении Minecraft без правок кода, пока Mojang продолжает
 * относить новые блоки к тем же тегам.
 *
 * GLASS_PANE и THIN_FOLIAGE резолвятся из кастомных тегов (см. TagRegistry),
 * так как у них нет единого vanilla-тега формы.
 *
 * FULL_BLOCK и NON_CUBE_OTHER — автоматический геометрический дефолт для любого
 * блока, не входящего ни в одну из перечисленных выше явных категорий. Решение
 * принимается по {@link com.sk89q.worldedit.world.registry.BlockMaterial#isFullCube()}
 * (данные приходят из version-адаптера FAWE, не из нашего кода — корректны для
 * любых новых блоков будущих версий Minecraft автоматически, без пополнения
 * хардкод-списков):
 * - FULL_BLOCK — геометрически цельный куб (камень, дерево, шерсть и т.п.),
 *   не требует shape-aware логики при замене.
 * - NON_CUBE_OTHER — геометрически НЕ цельный куб, но без отдельного vanilla-тега
 *   формы (одиночные цветы, мелкие грибы-растения и т.п. decorative-блоки).
 *   Введена, чтобы такие блоки не путались с FULL_BLOCK при замене (иначе
 *   recolor может заменить стену на цветок или наоборот — геометрический баг).
 */
public enum BlockShapeCategory {

    STAIRS,
    SLAB,
    WALL,
    FENCE,
    FENCE_GATE,
    DOOR,
    TRAPDOOR,
    PRESSURE_PLATE,
    BUTTON,
    LOG,
    CARPET,
    CHAIN,
    LANTERN,
    LEAVES,
    GLASS_PANE,
    RAIL,
    BARS,
    SAPLING,
    SHELF,
    CROP,
    POTTED,
    THIN_FOLIAGE,
    NON_CUBE_OTHER,
    FULL_BLOCK
}