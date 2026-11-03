package com.almarel.orion.mapping.block;

import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Material;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Глобальный Lock-Free кэш-адаптер для конвертации Bukkit Material -> FAWE BlockType.
 *
 * Отвечает за применение инварианта выделения:
 * "Воздух ВСЕГДА игнорируется при выделении (BFS)".
 * Мутирует DTO строго запрещено — адаптация происходит только внутри кэша.
 */
@Singleton
public class BlockTypeCacheService {

    private final ConcurrentHashMap<String, Set<BlockType>> cache = new ConcurrentHashMap<>();

    /**
     * Возвращает иммутабельный Set<BlockType>, адаптированный под XOR-логику BFS.
     *
     * @param materials Сырой сет из DTO (IIgnoreListable)
     * @param isWhitelist Режим работы игнор-листа
     */
    public Set<BlockType> getBlockTypes(Set<Material> materials, boolean isWhitelist) {
        String key = generateKey(materials, isWhitelist);
        return cache.computeIfAbsent(key, k -> convertAndAdapt(materials, isWhitelist));
    }

    private String generateKey(Set<Material> materials, boolean isWhitelist) {
        String prefix = isWhitelist ? "WL_" : "BL_";
        if (materials == null || materials.isEmpty()) {
            return prefix + "EMPTY";
        }
        // Сортировка гарантирует детерминированность ключа
        String matKey = materials.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
        return prefix + matKey;
    }

    private Set<BlockType> convertAndAdapt(Set<Material> materials, boolean isWhitelist) {
        Set<Material> adapted = applyAirInvariant(materials, isWhitelist);
        Set<BlockType> result = convertToBlockTypes(adapted);
        return Collections.unmodifiableSet(result);
    }

    private Set<Material> applyAirInvariant(Set<Material> materials, boolean isWhitelist) {
        Set<Material> adapted = new HashSet<>(materials != null ? materials : Collections.emptySet());
        if (isWhitelist) {
            adapted.remove(Material.AIR);
        } else {
            adapted.add(Material.AIR);
        }
        return adapted;
    }

    private Set<BlockType> convertToBlockTypes(Set<Material> materials) {
        Set<BlockType> result = new HashSet<>(materials.size());
        for (Material m : materials) {
            if (m.isBlock()) {
                try {
                    result.add(BukkitAdapter.asBlockType(m));
                } catch (Exception ignored) {
                    // Graceful skip: моды или неподдерживаемые типы
                }
            }
        }
        return result;
    }

    public Set<BlockType> getEffectiveIgnoreTypes(com.almarel.orion.brush.data.capabilities.IgnoreListable state, org.bukkit.block.Block targetBlock) {
        Set<BlockType> effectiveIgnore = new HashSet<>();

        if (state.getIgnoreMaterials() != null) {
            effectiveIgnore.addAll(getBlockTypes(state.getIgnoreMaterials(), state.isIgnoreWhitelist()));
        }

        if (state.isUseTargetBlock() && targetBlock != null) {
            BlockType clickedType = extractTargetBlockType(targetBlock);
            effectiveIgnore.add(clickedType);
        }

        return effectiveIgnore;
    }

    private BlockType extractTargetBlockType(org.bukkit.block.Block targetBlock) {
        return BukkitAdapter.adapt(targetBlock.getBlockData()).getBlockType();
    }
}