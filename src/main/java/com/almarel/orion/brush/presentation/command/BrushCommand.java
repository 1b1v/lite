package com.almarel.orion.brush.presentation.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.fawe.ExternalStateFactory;
import com.almarel.orion.brush.fawe.builtins.BrushProcessor;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.brush.presentation.command.handler.BrushSubCommandHandler;
import com.almarel.orion.framework.command.infrastructure.CommandArgsSupport;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import com.almarel.orion.brush.fawe.NativeBrushService;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Singleton
public class BrushCommand extends AbstractCommand {

    private final BrushStateManager stateManager;
    private final NativeBrushService nativeBrushService;
    private final MessageManager messageManager;
    private final FaweBrushRegistry aliasManager;
    private final ExternalStateFactory stateFactory;
    private final Set<BrushSubCommandHandler> brushHandlers;

    private static final List<String> COMMANDS = List.of(
            "/br", "//br", "/brush", "//brush",
            "/arc", "/arcbrush", "/arceon",
            "//arc", "//arcbrush", "//arceon"
    );

    private static final Set<String> UNBIND_KEYWORDS = Set.of("none", "unbind", "off", "clear");
    private static final String PLUGIN_ARCEON = "Arceon";
    private static final String PLUGIN_EZ_EDITS = "EzEdits";
    private static final String TEMPLATE_BRUSH_COMMAND = "/brush sphere";

    @Inject
    public BrushCommand(BrushStateManager stateManager,
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

    // ========================================================================
    // TAB COMPLETION
    // ========================================================================

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
        String token = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>(collectHandlerSuggestions(player, token));
        suggestions.addAll(collectExternalSuggestions(player, args));
        return suggestions;
    }

    private List<String> collectHandlerSuggestions(Player player, String token) {
        List<String> allowedBrushes = new ArrayList<>();
        for (BrushSubCommandHandler handler : brushHandlers) {
            if (player.hasPermission(handler.getRequiredPermission())) {
                allowedBrushes.addAll(handler.getAliases());
            }
        }
        return CommandArgsSupport.filterSuggestions(allowedBrushes, token);
    }

    private List<String> collectExternalSuggestions(Player player, String[] args) {
        List<String> suggestions = new ArrayList<>(delegateToFawe(player, args, "/brush"));

        if (isPluginEnabled(PLUGIN_ARCEON)) {
            suggestions.addAll(delegateToFawe(player, args, "/arcbrush"));
        }
        if (isPluginEnabled(PLUGIN_EZ_EDITS)) {
            suggestions.addAll(delegateToFawe(player, args, "/ezbrush"));
        }

        return suggestions;
    }

    private boolean isPluginEnabled(String pluginName) {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    private List<String> resolveSubCommandSuggestions(Player player, String[] args) {
        String brushType = args[0].toLowerCase(Locale.ROOT);
        for (BrushSubCommandHandler handler : brushHandlers) {
            if (handler.canHandle(brushType)) {
                return handler.onTabComplete(player, brushType, args);
            }
        }
        return delegateToFawe(player, args, TEMPLATE_BRUSH_COMMAND);
    }

    // ========================================================================
    // EXECUTION PIPELINE
    // ========================================================================

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }

        CommandInvocation invocation = resolveInvocation(label, args);
        if (invocation == null) {
            return false;
        }

        if (isUnbindCommand(invocation.subCommand())) {
            unbindAll(player);
            return false;
        }

        if (tryExecuteSubHandler(player, invocation)) {
            return true;
        }

        return tryEquipNativeBrush(player, label, invocation);
    }

    private CommandInvocation resolveInvocation(String label, String[] args) {
        int offset = calculateInvocationOffset(label, args);
        if (offset == -1) {
            return null;
        }

        String sub = args[offset].toLowerCase(Locale.ROOT);
        String[] shiftedArgs = (offset > 0) ? Arrays.copyOfRange(args, offset, args.length) : args;

        return new CommandInvocation(sub, shiftedArgs);
    }

    private int calculateInvocationOffset(String label, String[] args) {
        String lowerLabel = label.toLowerCase(Locale.ROOT);
        if (isArceonBrushAlias(lowerLabel) && args[0].equalsIgnoreCase("brush")) {
            return (args.length < 2) ? -1 : 1;
        }
        return 0;
    }

    private boolean isArceonBrushAlias(String lowerLabel) {
        return lowerLabel.equals("/arc") || lowerLabel.equals("//arc");
    }

    private boolean isUnbindCommand(String sub) {
        return UNBIND_KEYWORDS.contains(sub);
    }

    private boolean tryExecuteSubHandler(Player player, CommandInvocation invocation) {
        for (BrushSubCommandHandler handler : brushHandlers) {
            if (handler.canHandle(invocation.subCommand())) {
                handler.handle(player, invocation.subCommand(), invocation.args());
                return true;
            }
        }
        return false;
    }

    private boolean tryEquipNativeBrush(Player player, String label, CommandInvocation invocation) {
        String sub = invocation.subCommand();
        if (!aliasManager.hasProcessor(sub)) {
            return false;
        }

        if (!checkAndNotifyBindPermission(player, sub)) {
            return true;
        }

        String rawCommand = formatRawCommand(label, invocation.args());
        if (!nativeBrushService.equipNativeBrush(player, rawCommand)) {
            return false;
        }

        bindNativeState(player, label, sub, invocation.args());
        return true;
    }

    private boolean checkAndNotifyBindPermission(Player player, String sub) {
        if (hasBindPermission(player, sub)) {
            return true;
        }
        messageManager.send(player, OrionMessage.CMD_NO_PERM);
        return false;
    }

    private String formatRawCommand(String label, String[] args) {
        return label + " " + String.join(" ", args);
    }

    private boolean hasBindPermission(Player player, String sub) {
        BrushProcessor brush = aliasManager.getBrushProcessor(sub);
        if (brush != null) {
            return player.hasPermission(brush.getPermission());
        }
        ToolProcessor tool = aliasManager.getToolProcessor(sub);
        return tool == null || player.hasPermission(tool.getPermission());
    }

    private void bindNativeState(Player player, String label, String sub, String[] args) {
        Material tool = player.getInventory().getItemInMainHand().getType();
        unbindAll(player);

        String[] cleanArgs = extractTrailingArgs(args);
        persistState(player, tool, label, sub, cleanArgs);

        messageManager.send(player, OrionMessage.BRUSH_BOUND, sub, tool.name());
    }

    private String[] extractTrailingArgs(String[] args) {
        return (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
    }

    private void persistState(Player player, Material tool, String label, String sub, String[] cleanArgs) {
        BrushState newState = stateFactory.createState(tool, label, sub, cleanArgs);
        // Нативная кисть уже лежит в слоте FAWE: фиксируем квитанцию, а не pending
        stateManager.putSyncedBrushSettings(player, tool, newState);
    }

    private void unbindAll(Player player) {
        Material heldTool = player.getInventory().getItemInMainHand().getType();
        if (heldTool != Material.AIR) {
            stateManager.removeBrushSettings(player, heldTool);
        }
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return COMMANDS;
    }

    private record CommandInvocation(String subCommand, String[] args) {}
}