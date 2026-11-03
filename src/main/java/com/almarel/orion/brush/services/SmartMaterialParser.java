package com.almarel.orion.brush.services;

import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Изолированный сервис для парсинга материалов (современных и FAWE Legacy).
 */
@Singleton
public class SmartMaterialParser {

    public ParsedMaterials resolveIgnoreMaterials(List<String> rawNames) {
        Set<Material> valid = EnumSet.noneOf(Material.class);
        List<String> invalid = new ArrayList<>();

        if (rawNames == null || rawNames.isEmpty()) {
            return new ParsedMaterials(valid, invalid);
        }

        for (String rawName : rawNames) {
            Material mat = parseSingle(rawName);
            if (mat != null) {
                valid.add(mat);
            } else {
                invalid.add(rawName);
            }
        }
        return new ParsedMaterials(valid, invalid);
    }

    private Material parseSingle(String input) {
        if (input == null || input.isEmpty()) return null;

        // 1. Стандартный Bukkit (самый быстрый путь)
        Material mat = Material.matchMaterial(input);
        if (mat != null && mat.isBlock()) return mat;

        // 2. FAWE числовые ID (1, 2, 3:1 и т.д.)
        if (input.matches("^\\d+(:\\d+)?$")) {
            try {
                String[] parts = input.split(":");
                int id = Integer.parseInt(parts[0]);
                int data = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

                BlockState state = LegacyMapper.getInstance().getBlockFromLegacy(id, data);
                if (state != null) {
                    return BukkitAdapter.adapt(state.getBlockType());
                }
            } catch (Exception ignored) {
                // Игнорируем кривые ID
            }
        }

        return null;
    }

    public record ParsedMaterials(Set<Material> valid, List<String> invalid) {}
}