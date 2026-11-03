package com.almarel.orion.brush.fawe.processors.fawetools;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BlockDataCyler;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.HandSide;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;

public class CyclerToolProcessor implements ToolProcessor {

    @Override
    public boolean process(Player player, String[] args) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            // Обрати внимание: в FAWE опечатка в названии класса (BlockDataCyler вместо Cycler), используем как есть
            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new BlockDataCyler());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_DATA_CYCLER;
    }
}