package com.almarel.orion.brush.custom.crystal;

import com.almarel.orion.brush.custom.crystal.data.CrystalBrushState;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.handler.BaseBrushHandler;
import com.almarel.orion.brush.procedural.generator.CrystalGenerator;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.message.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Inject;

public class CrystalBrushHandler extends BaseBrushHandler {

    private final CrystalGenerator crystalGenerator;
    private final FaweParserService parserService;

    @Inject
    public CrystalBrushHandler(JavaPlugin plugin,
                               BrushStateManager stateManager,
                               CrystalGenerator crystalGenerator,
                               MessageManager messageManager,
                               FaweParserService parserService) {
        super(plugin, stateManager, messageManager);
        this.crystalGenerator = crystalGenerator;
        this.parserService = parserService;
    }

    @Override
    public boolean isActive(Player player, Material tool) {
        return stateManager.resolveActiveState(player, tool) instanceof CrystalBrushState;
    }

    @Override
    public boolean onLeftClick(Player player, PlayerInteractEvent event) {
        if (isActive(player, player.getInventory().getItemInMainHand().getType())) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onRightClick(Player player, PlayerInteractEvent event) {
        return false;
    }

    @Override
    public void onFaweBuild(Player player, EditSession session, BlockVector3 target, Pattern pattern, double size) {
        Material tool = player.getInventory().getItemInMainHand().getType();
        BrushState rawState = stateManager.resolveActiveState(player, tool);

        if (!(rawState instanceof CrystalBrushState st)) return;
        if (!checkPermission(player, OrionPermissions.BRUSH_CRYSTAL_USE)) return;

        try {
            Mask parsedMask = parserService.parseMask(player, st.mask);
            if (parsedMask != null) session.setMask(parsedMask);

            crystalGenerator.generateDirect(session, target, player, st, pattern);

        } catch (Exception e) {
            plugin.getLogger().severe("Error: " + e.getMessage());
        }
    }
}