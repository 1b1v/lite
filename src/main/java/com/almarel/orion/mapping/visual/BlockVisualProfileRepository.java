package com.almarel.orion.mapping.visual;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class BlockVisualProfileRepository {

    private final BlockVisualProfileLoader loader;
    private final BlockItemFormAvailability itemFormAvailability;
    private volatile Map<BlockType, BlockVisualProfile> profiles = Map.of();

    @Inject
    public BlockVisualProfileRepository(BlockVisualProfileLoader loader,
                                        BlockItemFormAvailability itemFormAvailability) {
        this.loader = loader;
        this.itemFormAvailability = itemFormAvailability;
    }

    /**
     * Загружает и мёрджит базовый датасет и оверрайды куратора, затем
     * применяет автоматическое обогащение: любой профиль, который ещё не
     * исключён куратором/экстрактором, но чей BlockType не имеет
     * рендерящейся item-формы (например COCOA), помечается
     * excluded=true/NO_ITEM_FORM. Это гарантирует, что ни один consumer,
     * уважающий {@link BlockVisualProfile#excluded()}, никогда не получит
     * такой блок как кандидат для GUI-иконки.
     */
    public void load() {
        this.profiles = enforceItemFormAvailability(loader.loadProfiles());
    }

    private Map<BlockType, BlockVisualProfile> enforceItemFormAvailability(
            Map<BlockType, BlockVisualProfile> source) {
        Map<BlockType, BlockVisualProfile> enriched = new HashMap<>(source.size());
        for (Map.Entry<BlockType, BlockVisualProfile> entry : source.entrySet()) {
            enriched.put(entry.getKey(), enrichProfile(entry.getKey(), entry.getValue()));
        }
        return Map.copyOf(enriched);
    }

    private BlockVisualProfile enrichProfile(BlockType type, BlockVisualProfile profile) {
        if (shouldExcludeDueToMissingItemForm(type, profile)) {
            return createNoItemFormProfile(profile);
        }
        return profile;
    }

    private boolean shouldExcludeDueToMissingItemForm(BlockType type, BlockVisualProfile profile) {
        return !profile.excluded() && !itemFormAvailability.hasRenderableItemForm(type);
    }

    private BlockVisualProfile createNoItemFormProfile(BlockVisualProfile profile) {
        return new BlockVisualProfile(
                profile.blockType(),
                profile.rgbColor(),
                profile.labColor(),
                profile.hslColor(),
                profile.noiseScore(),
                true,
                VisualExclusionReason.NO_ITEM_FORM
        );
    }

    public Optional<BlockVisualProfile> getProfile(BlockType blockType) {
        return Optional.ofNullable(profiles.get(blockType));
    }

    public Collection<BlockVisualProfile> getAllProfiles() {
        return profiles.values();
    }
}