package com.almarel.orion.brush.fawe.processors.fawetools;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.DistanceWand;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.HandSide;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;

public class FarwandToolProcessor implements ToolProcessor {

    @Override
    public boolean process(Player player, String[] args) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            // Инструмент вешается напрямую на предмет, заменяя BrushTool (если он там был)
            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new DistanceWand());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_FARWAND;
    }
}