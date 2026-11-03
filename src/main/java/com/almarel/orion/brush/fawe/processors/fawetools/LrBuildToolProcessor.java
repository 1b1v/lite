package com.almarel.orion.brush.fawe.processors.fawetools;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.LongRangeBuildTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.HandSide;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;

public class LrBuildToolProcessor implements ToolProcessor {

    @Override
    public boolean process(Player player, String[] args) {
        if (args.length < 2) {
            return false; // Требуется минимум 2 паттерна
        }

        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);

            // В FAWE LrBuildTool принимает аргументы в порядке: primary, secondary
            Pattern primary = parsePattern(player, session, args[0]);
            Pattern secondary = parsePattern(player, session, args[1]);

            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new LongRangeBuildTool(primary, secondary));
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

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_LRBUILD;
    }
}