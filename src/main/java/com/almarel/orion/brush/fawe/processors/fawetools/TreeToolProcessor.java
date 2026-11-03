package com.almarel.orion.brush.fawe.processors.fawetools;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.TreePlanter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.generation.TreeType;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;
import org.bukkit.Bukkit;

import java.util.Locale;
import java.util.logging.Level;

/**
 * Процессор /tool tree. Тип дерева берётся из нативного реестра FAWE (TreeType.REGISTRY),
 * как и в штатной команде. Неизвестный тип -> false (ошибку покажет сам FAWE).
 */
public class TreeToolProcessor implements ToolProcessor {

    private static final String DEFAULT_TYPE = "tree";

    @Override
    public boolean process(Player player, String[] args) {
        TreeType type = resolveType(args);
        if (type == null) {
            return false;
        }
        return bind(player, type);
    }

    private TreeType resolveType(String[] args) {
        String id = (args.length > 0) ? args[0] : DEFAULT_TYPE;
        return TreeType.REGISTRY.get(id.toLowerCase(Locale.ROOT));
    }

    private boolean bind(Player player, TreeType type) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new TreePlanter(type));
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[1b1v] Failed to bind tree tool: " + type, e);
            return false;
        }
    }

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_TREE;
    }
}