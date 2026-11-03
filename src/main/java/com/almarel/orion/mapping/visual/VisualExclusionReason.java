package com.almarel.orion.mapping.visual;

/**
 * Reason a block is flagged unsuitable for visual automation
 * (nearest-color fallback, auto-palette generation). Distinct from
 * RecolorExclusionRegistry, which excludes blocks structurally (NBT-heavy),
 * not visually.
 */
public enum VisualExclusionReason {
    TRANSPARENT,
    EMITS_LIGHT,
    GRAVITY_AFFECTED,
    LIQUID,
    ANIMATED_TEXTURE,
    HIGH_NOISE,
    /**
     * Block has no renderable inventory item form (BukkitAdapter.adapt(type)
     * yields a Material for which Material.isItem() is false — e.g. COCOA,
     * ATTACHED_PUMPKIN_STEM, ATTACHED_MELON_STEM). Such blocks can be placed
     * in the world but cannot back an ItemStack, which would crash GUI icon
     * rendering (new ItemStack(material) → IllegalArgumentException).
     * Computed automatically at repository load time, not hand-curated.
     */
    NO_ITEM_FORM,
    /**
     * Not produced by the extraction pipeline. Set implicitly when a curator
     * marks {@code excluded: true} in the overrides file without specifying
     * a more precise reason, or explicitly if they write this value by hand.
     */
    CURATOR_OVERRIDE
}