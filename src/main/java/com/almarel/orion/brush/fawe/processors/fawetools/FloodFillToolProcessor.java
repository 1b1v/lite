package com.almarel.orion.brush.fawe.processors.fawetools;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.FloodFillTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.HandSide;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;

public class FloodFillToolProcessor implements ToolProcessor {

    private static final int DEFAULT_RANGE = 5;

    @Override
    public boolean process(Player player, String[] args) {
        if (args.length == 0) {
            return false; // Паттерн обязателен
        }

        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            Pattern pattern = parsePattern(player, session, args[0]);
            int range = parseRange(args);

            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new FloodFillTool(range, pattern));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Pattern parsePattern(Player player, LocalSession session, String patternInput) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(player);
        context.setSession(session);
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(patternInput, context);
    }

    private int parseRange(String[] args) {
        if (args.length > 1) {
            try {
                return Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_RANGE;
    }

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_FLOOD_FILL;
    }
}