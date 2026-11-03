package com.almarel.orion.brush.data;

import com.almarel.orion.brush.data.enums.OrionBrushType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseBrushState implements BrushState {

    public Material tool;
    public boolean persistent = false;
    public int orderIndex = 0;
    public String id;

    public BaseBrushState(Material tool) {
        this.tool = tool;
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    @Override
    public String getStorageKey() {
        String prefix = getBrushType().name().toLowerCase() + "_";

        if (tool == null) {
            return prefix + (id != null ? id : "unbound");
        }

        return prefix + tool.name().toLowerCase();
    }

    @Override
    public OrionBrushType getBrushType() {
        return OrionBrushType.GENERIC;
    }

    @Override
    public void cachePlayerOrientation(Player player) {
        // Пустая реализация. Подклассы переопределят при необходимости.
    }

    @Override
    public boolean needsRecreationOnDesync() {
        return false;
    }
}