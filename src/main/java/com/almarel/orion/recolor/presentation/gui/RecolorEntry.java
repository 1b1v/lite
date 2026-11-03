package com.almarel.orion.recolor.presentation.gui;

import com.sk89q.worldedit.world.block.BlockType;
import org.jetbrains.annotations.Nullable;

/**
 * Одна пара "было -> станет" в GUI recolor.
 *
 * original — исходный тип блока, найденный сканером региона. Неизменяем
 * на весь срок жизни сессии (строки 2/4 в GUI, read-only слоты).
 *
 * desiredTarget — сырой материал, который игрок поместил в правый слот
 * ("станет"), БЕЗ shape-резолва. null означает "exclude": пустой слот,
 * блок не будет тронут при Apply. Резолв через ShapeAwareVariantResolver
 * выполняется отдельно, на этапе рендера/Apply (см. RecolorMenuPresenter),
 * здесь хранится только исходное пользовательское намерение — это даёт
 * Preview и Apply единый источник истины и позволяет пересчитать резолв
 * заново, если в будущем изменится логика подбора (например, появится
 * NearestBlockByColorFinder fallback).
 *
 * Мутируется строго в main thread (обработчики кликов GUI).
 */
public final class RecolorEntry {

    private final BlockType original;
    private BlockType desiredTarget;

    public RecolorEntry(BlockType original) {
        this.original = original;
        this.desiredTarget = null;
    }

    public BlockType getOriginal() {
        return original;
    }

    @Nullable
    public BlockType getDesiredTarget() {
        return desiredTarget;
    }

    public void setDesiredTarget(@Nullable BlockType desiredTarget) {
        this.desiredTarget = desiredTarget;
    }

    public void clearDesiredTarget() {
        this.desiredTarget = null;
    }

    public boolean hasDesiredTarget() {
        return desiredTarget != null;
    }
}