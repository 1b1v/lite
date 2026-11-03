package com.almarel.orion.brush.fawe.gateway;

import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.Constructable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@Singleton
public class FaweBrushInjector {

    private final FaweSessionGateway sessionGateway;
    private final JavaPlugin plugin;

    @Inject
    public FaweBrushInjector(
            FaweSessionGateway sessionGateway,
            JavaPlugin plugin
    ) {
        this.sessionGateway = sessionGateway;
        this.plugin = plugin;
    }

    public void injectAdapter(Player player, Material toolType, Brush adapter) {
        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);

            BrushTool tool = sessionGateway.acquirePureBrushTool(wePlayer);
            if (tool == null) return;

            BrushSettings settings = new BrushSettings();
            settings.setBrush(adapter);

            tool.setPrimary(settings);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to inject brush adapter for player " + player.getName(), e);
        }
    }

    public Brush getActiveAdapter(Player player, Material toolMaterial) {
        BrushTool tool = sessionGateway.getBrushTool(player, toolMaterial);
        return tool != null ? tool.getBrush() : null;
    }

    private void applyBrushToTool(BrushTool tool, Brush brush) {
        BrushSettings settings = tool.getPrimary();
        if (settings == null) {
            settings = new BrushSettings();
            tool.setPrimary(settings);
        }
        settings.setBrush(brush);
    }

    // --- State Backup Logic ---

    private BrushSettingsBackup backupSettings(BrushTool tool) {
        return new BrushSettingsBackup(
                tool.getSize(),
                tool.getMaterial(),
                tool.getMask(),
                tool.getSourceMask(),
                tool.getRange()
        );
    }

    private void restoreSettings(BrushTool tool, BrushSettingsBackup backup) {
        tool.setSize(backup.size());
        if (backup.pattern() != null) tool.setFill(backup.pattern());
        if (backup.mask() != null) tool.setMask(backup.mask());
        if (backup.sourceMask() != null) tool.setSourceMask(backup.sourceMask());
        tool.setRange(backup.range());
    }

    private record BrushSettingsBackup(
            double size,
            Pattern pattern,
            Mask mask,
            Mask sourceMask,
            int range
    ) {}
}