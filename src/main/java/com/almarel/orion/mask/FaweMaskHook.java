package com.almarel.orion.mask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.WorldEdit;

@Singleton
public class FaweMaskHook {

    private final WorldEdit worldEdit;

    @Inject
    public FaweMaskHook(
            WorldEdit worldEdit
    ) {
        this.worldEdit = worldEdit;
    }

    public void registerAll() {
        worldEdit.getMaskFactory().register(new AroundMaskParser(worldEdit));
    }
}