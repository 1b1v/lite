// TYPE B: FULL FILE
package com.almarel.orion.brush.presentation.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.fawe.ExternalStateFactory;
import com.almarel.orion.brush.fawe.builtins.BrushProcessor;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import com.almarel.orion.brush.fawe.NativeBrushService;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.brush.presentation.command.handler.BrushSubCommandHandler;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Singleton
public class ToolCommand extends AbstractCommand {

    private static final List<String> COMMANDS = List.of("/tool", "//tool");
    private static final Set<String> UNBIND_KEYWORDS = Set.of("none", "unbind", "off", "clear");
    private static final String[] EMPTY_ARGS = new String[0];

    private final BrushStateManager stateManager;
    private final NativeBrushService nativeBrushService;
    private final FaweBrushRegistry aliasManager;
    private final MessageManager messageManager;
    private final ExternalStateFactory stateFactory;
    private final Set<BrushSubCommandHandler> brushHandlers;

    @Inject
    public ToolCommand(BrushStateManager stateManager,
                       NativeBrushService nativeBrushService,
                       FaweBrushRegistry aliasManager,
                       Set<BrushSubCommandHandler> brushHandlers,
                       FaweSuggestionProvider faweService,
                       MessageManager messageManager,
                       ExternalStateFactory stateFactory) {
        super(faweService);
        this.stateManager = stateManager;
        this.nativeBrushService = nativeBrushService;
        this.aliasManager = aliasManager;
        this.brushHandlers = brushHandlers;
        this.messageManager = messageManager;
        this.stateFactory = stateFactory;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            return resolveFirstArgSuggestions(player, args);
        }

        return resolveSubCommandSuggestions(player, args);
    }

    private List<String> resolveFirstArgSuggestions(Player player, String[] args) {
        // Orion-кисти намеренно НЕ показываются в подсказках первого уровня /tool —
        // команда предназначена для нативных FAWE-тулов. /tool plump по-прежнему РАБОТАЕТ
        // (см. process() → tryExecuteSubHandler), просто не подсказывается явно.
        return delegateToFawe(player, args, "/tool");
    }

    private List<String> resolveSubCommandSuggestions(Player player, String[] args) {
        String toolType = args[0].toLowerCase(Locale.ROOT);
        for (BrushSubCommandHandler handler : brushHandlers) {
            if (handler.canHandle(toolType)) {
                return handler.onTabComplete(player, toolType, args);
            }
        }
        return delegateToFawe(player, args, "/tool " + toolType);
    }

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (isUnbindCommand(sub)) {
            unbindCurrentTool(player);
            return false;
        }

        if (tryExecuteSubHandler(player, sub, args)) {
            return true;
        }

        return tryEquipNativeTool(player, label, sub, args);
    }

    private boolean isUnbindCommand(String sub) {
        return UNBIND_KEYWORDS.contains(sub);
    }

    private boolean tryExecuteSubHandler(Player player, String sub, String[] args) {
        for (BrushSubCommandHandler handler : brushHandlers) {
            if (handler.canHandle(sub)) {
                handler.handle(player, sub, args);
                return true;
            }
        }
        return false;
    }

    private boolean tryEquipNativeTool(Player player, String label, String sub, String[] args) {
        if (!aliasManager.hasProcessor(sub)) {
            return false;
        }

        if (!hasBindPermission(player, sub)) {
            messageManager.send(player, OrionMessage.CMD_NO_PERM);
            return true;
        }

        String rawCommand = buildRawCommand(label, args);
        if (!nativeBrushService.equipNativeBrush(player, rawCommand)) {
            return false;
        }

        bindNativeState(player, label, sub, args);
        return true;
    }

    /**
     * P1-fix: право проверяется ДО бинда. См. комментарий в BrushCommand.hasBindPermission.
     */
    private boolean hasBindPermission(Player player, String sub) {
        BrushProcessor brush = aliasManager.getBrushProcessor(sub);
        if (brush != null) {
            return player.hasPermission(brush.getPermission());
        }

        ToolProcessor tool = aliasManager.getToolProcessor(sub);
        return tool == null || player.hasPermission(tool.getPermission());
    }

    private String buildRawCommand(String label, String[] args) {
        return label + " " + String.join(" ", args);
    }

    private void bindNativeState(Player player, String label, String sub, String[] args) {
        Material tool = player.getInventory().getItemInMainHand().getType();
        unbindCurrentTool(player);

        String[] cleanArgs = extractSubArguments(args);
        BrushState newState = stateFactory.createState(tool, label, sub, cleanArgs);

        stateManager.putSyncedBrushSettings(player, tool, newState);
        messageManager.send(player, OrionMessage.BRUSH_TOOL_BOUND, sub, tool.name());
    }

    private String[] extractSubArguments(String[] args) {
        return (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length) : EMPTY_ARGS;
    }

    private void unbindCurrentTool(Player player) {
        Material heldTool = player.getInventory().getItemInMainHand().getType();
        if (heldTool != Material.AIR) {
            stateManager.removeBrushSettings(player, heldTool);
        }
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return COMMANDS;
    }
}