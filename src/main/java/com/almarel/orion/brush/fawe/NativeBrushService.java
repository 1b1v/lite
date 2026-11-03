package com.almarel.orion.brush.fawe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.almarel.orion.brush.fawe.gateway.FaweSessionGateway;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import com.almarel.orion.brush.fawe.builtins.BrushProcessor;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Исполнитель команд для нативных кистей FAWE и Arceon.
 * Парсит текстовые команды и передает управление соответствующему процессору.
 */
@Singleton
public class NativeBrushService {

    private final FaweBrushRegistry aliasManager;
    private final ExternalCommandParser commandParser;
    private final FaweSessionGateway sessionGateway;

    @Inject
    public NativeBrushService(FaweBrushRegistry aliasManager,
                              ExternalCommandParser commandParser,
                              FaweSessionGateway sessionGateway) {
        this.aliasManager = aliasManager;
        this.commandParser = commandParser;
        this.sessionGateway = sessionGateway;
    }

    /**
     * Пытается применить нативную кисть или инструмент FAWE/Arceon/EzEdits.
     */
    public boolean equipNativeBrush(Player player, String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) return false;

        ParsedCommand cmd = commandParser.parse(rawCommand);
        if (cmd == null || cmd.type() != ParsedCommand.CommandType.CREATION) {
            return false;
        }

        return executeCommandSafely(player, cmd);
    }

    private boolean executeCommandSafely(Player player, ParsedCommand cmd) {
        String aliasName = cmd.alias();
        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            return dispatchProcessor(wePlayer, aliasName, cmd.args());
        } catch (com.sk89q.worldedit.util.auth.AuthorizationException e) {
            // У игрока нет прав на эту кисть (FAWE валидация)
            return false;
        } catch (LinkageError e) {
            // Несовместимость API FAWE (NoSuchMethodError и т.п.) — не должна ломать обработку события
            Bukkit.getLogger().log(Level.SEVERE, "[1b1v] FAWE API mismatch in processor for: " + aliasName, e);
            return false;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[1b1v] Failed to process native command: " + aliasName, e);
            return false;
        }
    }

    private boolean dispatchProcessor(com.sk89q.worldedit.entity.Player wePlayer, String aliasName, String[] args) {
        ToolProcessor toolProcessor = aliasManager.getToolProcessor(aliasName);
        if (toolProcessor != null) {
            return toolProcessor.process(wePlayer, args);
        }

        BrushProcessor brushProcessor = aliasManager.getBrushProcessor(aliasName);
        if (brushProcessor != null) {
            return executeBrushProcessor(brushProcessor, wePlayer, args);
        }

        return false;
    }

    private boolean executeBrushProcessor(BrushProcessor processor, com.sk89q.worldedit.entity.Player wePlayer, String[] args) {
        BrushTool tool = resolveBrushTool(wePlayer);
        if (tool == null) {
            return false;
        }
        return processor.process(wePlayer, tool, args);
    }

    private BrushTool resolveBrushTool(com.sk89q.worldedit.entity.Player wePlayer) {
        return sessionGateway.acquirePureBrushTool(wePlayer);
    }
}