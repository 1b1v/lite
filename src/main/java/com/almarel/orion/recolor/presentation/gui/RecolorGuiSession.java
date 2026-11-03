package com.almarel.orion.recolor.presentation.gui;

import com.almarel.orion.recolor.palette.DominantColorGroup;
import com.almarel.orion.recolor.palette.QuickPalettePreviewOption;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecolorGuiSession {

    private final World regionWorld;
    private final BlockVector3 regionMin;
    private final BlockVector3 regionMax;
    private final List<RecolorEntry> entries;
    private final List<DominantColorGroup> dominantGroups;
    private final List<QuickPalettePreviewOption> quickPaletteOptions;
    private final AtomicBoolean quickPaletteResolving = new AtomicBoolean(false);

    /**
     * CAS-попытка "занять" сессию для тяжёлого резолва быстрой палитры.
     * Защищает от гонки при двойном клике по иконкам до завершения
     * предыдущего async-резолва (см. обсуждение race condition).
     */
    public boolean tryBeginQuickPaletteResolve() {
        return quickPaletteResolving.compareAndSet(false, true);
    }

    public void endQuickPaletteResolve() {
        quickPaletteResolving.set(false);
    }

    private volatile int currentPage = 0;

    public RecolorGuiSession(World regionWorld,
                             BlockVector3 regionMin,
                             BlockVector3 regionMax,
                             List<RecolorEntry> entries,
                             List<DominantColorGroup> dominantGroups,
                             List<QuickPalettePreviewOption> quickPaletteOptions) {
        this.regionWorld = regionWorld;
        this.regionMin = regionMin;
        this.regionMax = regionMax;
        this.entries = entries;
        this.dominantGroups = dominantGroups;
        this.quickPaletteOptions = quickPaletteOptions;
    }

    public boolean matchesRegion(World world, BlockVector3 min, BlockVector3 max) {
        return regionWorld.equals(world) && regionMin.equals(min) && regionMax.equals(max);
    }

    public List<RecolorEntry> getEntries() {
        return entries;
    }

    public List<DominantColorGroup> getDominantGroups() {
        return dominantGroups;
    }

    public List<QuickPalettePreviewOption> getQuickPaletteOptions() {
        return quickPaletteOptions;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
}