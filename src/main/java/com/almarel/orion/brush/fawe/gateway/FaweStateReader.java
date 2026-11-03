package com.almarel.orion.brush.fawe.gateway;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.fawe.data.FaweConstructBrushState;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@Singleton
public class FaweStateReader {
    private final JavaPlugin plugin;

    @Inject
    public FaweStateReader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void captureBrushState(UUID uuid, BrushState state) {
        if (uuid == null || state == null) return;

        try {
            LocalSession session = resolveSession(uuid);
            if (session == null) return;

            BrushTool tool = resolveBrushTool(session, state.getTool());
            if (tool == null) return;

            captureToolAttributes(tool, state);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Failed to capture brush state from FAWE for tool: " + state.getTool(), e);
        }
    }

    private LocalSession resolveSession(UUID uuid) {
        FaweSessionGateway.UuidSessionOwner lookupOwner = new FaweSessionGateway.UuidSessionOwner(uuid);
        return WorldEdit.getInstance().getSessionManager().getIfPresent(lookupOwner);
    }

    private BrushTool resolveBrushTool(LocalSession session, org.bukkit.Material material) {
        Tool tool = session.getTool(BukkitAdapter.asItemType(material));
        return (tool instanceof BrushTool bt) ? bt : null;
    }

    private void captureToolAttributes(BrushTool bt, BrushState state) {
        captureRange(bt, state);
        captureMask(bt, state);
        captureSourceMask(bt, state);
        // [ORION_CUT: FaweConstructBrushState]
        captureConstructSize(bt, state);
        // [ORION_CUT_END]
        capturePattern(bt, state);
    }

    // --- Extracted Capturers ---

    private void captureRange(BrushTool bt, BrushState state) {
        if (state instanceof Rangeable rangeable) {
            rangeable.setRange(bt.getRange());
        }
    }

    private void captureMask(BrushTool bt, BrushState state) {
        Mask mask = bt.getMask();
        if (mask != null && state instanceof Maskable maskable) {
            maskable.setMask(mask.toString());
        }
    }

    private void captureSourceMask(BrushTool bt, BrushState state) {
        if (state instanceof SourceMaskable sourceMaskable) {
            Mask srcMask = bt.getSourceMask();
            sourceMaskable.setSourceMask(srcMask != null ? srcMask.toString() : null);
        }
    }

    // [ORION_CUT: FaweConstructBrushState]
    private void captureConstructSize(BrushTool bt, BrushState state) {
        if (state instanceof FaweConstructBrushState && state instanceof Sizeable sizeable) {
            sizeable.setSize(bt.getSize());
        }
    }
    // [ORION_CUT_END]

    private void capturePattern(BrushTool bt, BrushState state) {
        Pattern pattern = bt.getMaterial();
        if (pattern != null && state instanceof Patternable patternable) {
            patternable.setPattern(BrushFormatter.formatPattern(pattern));
        }
    }
}