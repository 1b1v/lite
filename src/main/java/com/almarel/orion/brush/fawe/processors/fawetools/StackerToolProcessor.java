package com.almarel.orion.brush.fawe.processors.fawetools;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.StackTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.HandSide;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.data.OrionPermissions;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class StackerToolProcessor implements ToolProcessor {

    private static final int DEFAULT_RANGE = 10;
    private static final String DEFAULT_MASK = "!#existing";

    @Override
    public boolean process(Player player, String[] args) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            int range = parseRange(args);
            Mask mask = parseMask(player, session, args);
            session.setTool(player.getItemInHand(HandSide.MAIN_HAND), new StackTool(range, mask));
            return true;
        } catch (InputParseException e) {
            // Ошибка пользовательского ввода: сообщение игроку покажет нативная команда FAWE
            return false;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[1b1v] Failed to bind stacker tool", e);
            return false;
        }
    }

    private int parseRange(String[] args) {
        if (args.length == 0) {
            return DEFAULT_RANGE;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return DEFAULT_RANGE;
        }
    }

    private Mask parseMask(Player player, LocalSession session, String[] args) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(player);
        context.setSession(session);
        context.setWorld(player.getWorld()); // выставляет и extent (для масок вроде #existing)
        String maskInput = (args.length > 1) ? args[1] : DEFAULT_MASK;
        return WorldEdit.getInstance().getMaskFactory().parseFromInput(maskInput, context);
    }

    @Override
    public String getPermission() {
        return OrionPermissions.FAWE_TOOL_STACK;
    }
}