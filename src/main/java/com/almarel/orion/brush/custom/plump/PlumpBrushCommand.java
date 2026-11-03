package com.almarel.orion.brush.custom.plump;

import com.almarel.orion.brush.custom.plump.data.PlumpBrushState;
import com.google.inject.Inject;
import com.almarel.orion.brush.presentation.command.handler.BrushSubCommandHandler;
import com.almarel.orion.framework.command.infrastructure.CommandArgsSupport;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.brush.BrushManager;
import com.almarel.orion.brush.services.BrushPreconditionService;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PlumpBrushCommand implements BrushSubCommandHandler {

    private static final List<String> RADIUS_SUGGESTIONS = Arrays.asList("5", "10", "15", "20", "30");

    private final BrushStateManager stateManager;
    private final MessageManager messageManager;
    private final BrushPreconditionService preconditionService;
    private final FaweIntegrationService faweIntegrationService;
    private final BrushManager brushManager;

    @Inject
    public PlumpBrushCommand(BrushStateManager stateManager,
                             MessageManager messageManager,
                             BrushPreconditionService preconditionService,
                             FaweIntegrationService faweIntegrationService,
                             BrushManager brushManager) {
        this.stateManager = stateManager;
        this.messageManager = messageManager;
        this.preconditionService = preconditionService;
        this.faweIntegrationService = faweIntegrationService;
        this.brushManager = brushManager;
    }

    @Override
    public boolean canHandle(@NotNull String subCommand) {
        return "plump".equalsIgnoreCase(subCommand);
    }

    @Override
    public void handle(@NotNull Player player, @NotNull String subCommand, @NotNull String[] args) {
        Material tool = preconditionService.prepareTool(player, OrionPermissions.BRUSH_PLUMP_USE, false);
        if (tool == null) return;

        var st = stateManager.getOrCreateBrushSettings(player, tool, PlumpBrushState.class, () -> new PlumpBrushState(tool));

        if (args.length > 1) st.pattern = args[1];
        if (args.length > 2) {
            try {
                st.size = Double.parseDouble(args[2]);
            } catch (Exception ignored) {}
        }

        messageManager.send(player, OrionMessage.BRUSH_PLUMP_ACTIVE, String.valueOf(st.size), tool.toString());
        faweIntegrationService.injectOrionBrush(player, brushManager, OrionBrushType.PLUMP, null);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String subCommand, @NotNull String[] args) {
        if (args.length == 3) {
            String token = args[args.length - 1].toLowerCase();
            return CommandArgsSupport.filterSuggestions(RADIUS_SUGGESTIONS, token);
        }
        return List.of();
    }

    @Override
    public @NotNull Collection<String> getAliases() {
        return List.of("plump");
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return OrionPermissions.BRUSH_PLUMP_USE;
    }
}