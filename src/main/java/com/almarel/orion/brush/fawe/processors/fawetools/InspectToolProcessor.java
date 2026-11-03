package com.almarel.orion.brush.fawe.processors.fawetools;

import com.fastasyncworldedit.core.command.tool.brush.InspectBrush;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.HandSide;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;

public class InspectToolProcessor implements ToolProcessor {

    @Override
    public boolean process(Player player, String[] args) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new InspectBrush());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_INSPECT;
    }
}