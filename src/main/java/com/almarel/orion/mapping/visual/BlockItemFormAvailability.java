package com.almarel.orion.mapping.visual;

import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Material;

/**
 * Pure leaf-level check: does this BlockType have a renderable inventory
 * item form. Used exclusively by {@link BlockVisualProfileRepository} to
 * enrich loaded profiles with {@link VisualExclusionReason#NO_ITEM_FORM}.
 * Do NOT inject this elsewhere — any other consumer needing this
 * information should query {@code BlockVisualProfile.excluded()} instead,
 * keeping exclusion knowledge centralized in one dataset.
 */
@Singleton
public class BlockItemFormAvailability {

    public boolean hasRenderableItemForm(BlockType type) {
        try {
            Material material = BukkitAdapter.adapt(type);
            return material != null && material.isItem();
        } catch (Exception e) {
            return false;
        }
    }
}