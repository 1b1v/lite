package com.almarel.orion.recolor.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

/**
 * Реестр блоков, исключённых из recolor по умолчанию (NBT-тяжёлые блоки).
 * Оптимизирован для частых вызовов isExcluded() на горячем пути.
 */
@Singleton
public class RecolorExclusionRegistry {

    private static final BlockCategory[] EXCLUDED_CATEGORIES = {
            BlockCategories.BEDS,
            BlockCategories.BANNERS,
            BlockCategories.SHULKER_BOXES,
            BlockCategories.ALL_SIGNS,
            BlockCategories.ALL_HANGING_SIGNS
    };

    private final JavaPlugin plugin;

    /** excludedById[BlockType.internalId] -> true */
    private volatile boolean[] excludedById = new boolean[0];

    @Inject
    public RecolorExclusionRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        Set<BlockType> all = new HashSet<>();
        for (BlockCategory category : EXCLUDED_CATEGORIES) {
            try {
                all.addAll(category.getAll());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load exclusion category " + category.getId() + ": " + e.getMessage());
            }
        }

        this.excludedById = buildFlatMap(all);
    }

    public boolean isExcluded(BlockType type) {
        int id = type.getInternalId();
        boolean[] map = excludedById;
        return id >= 0 && id < map.length && map[id];
    }

    private static boolean[] buildFlatMap(Set<BlockType> types) {
        int maxId = -1;
        for (BlockType t : types) {
            maxId = Math.max(maxId, t.getInternalId());
        }
        if (maxId < 0) {
            return new boolean[0];
        }

        boolean[] flat = new boolean[maxId + 1];
        for (BlockType t : types) {
            int id = t.getInternalId();
            if (id >= 0 && id < flat.length) {
                flat[id] = true;
            }
        }
        return flat;
    }
}