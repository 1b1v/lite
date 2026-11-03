package com.almarel.orion.brush.custom.plump;

import com.almarel.orion.brush.custom.plump.data.PlumpBrushState;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.brush.handler.BaseBrushHandler;
import com.almarel.orion.brush.procedural.generator.PlumpGenerator; // [ИЗМЕНЕНИЕ]
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.message.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.EditSession;

import com.google.inject.Inject;

public class PlumpBrushHandler extends BaseBrushHandler {

    private final PlumpGenerator plumpGenerator;
    private final FaweParserService parserService;

    @Inject
    public PlumpBrushHandler(JavaPlugin plugin,
                             BrushStateManager stateManager,
                             PlumpGenerator plumpGenerator,
                             MessageManager messageManager,
                             FaweParserService parserService) {
        super(plugin, stateManager, messageManager);
        this.plumpGenerator = plumpGenerator;
        this.parserService = parserService;
    }

    @Override
    public boolean isActive(Player player, Material tool) {
        return stateManager.resolveActiveState(player, tool) instanceof PlumpBrushState;
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
        if (!(stateManager.resolveActiveState(player, tool) instanceof PlumpBrushState st)) return;
        if (!checkPermission(player, OrionPermissions.BRUSH_PLUMP_USE)) return;

        String originalPattern = st.pattern;
        double originalSize = st.size;

        try {
            Mask parsedMask = parserService.parseMask(player, st.mask);
            if (parsedMask != null) {
                session.setMask(parsedMask);
            }

            plumpGenerator.generateDirect(session, target, player, st, pattern);

        } catch (Exception e) {
            plugin.getLogger().severe("Error: " + e.getMessage());
        } finally {
            st.size = originalSize;
            st.pattern = originalPattern;
        }
    }
}