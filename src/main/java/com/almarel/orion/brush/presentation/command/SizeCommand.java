package com.almarel.orion.brush.presentation.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.Sizeable;
import com.almarel.orion.brush.fawe.data.FaweConstructBrushState;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
public class SizeCommand extends AbstractCommand {

    private final BrushStateManager brushStateManager;
    private final FaweIntegrationService faweIntegrationService;
    private final MessageManager messageManager; // [NEW]

    private static final List<String> COMMANDS = List.of("//size", "/size", "/sz", "/radius");

    @Inject
    public SizeCommand(BrushStateManager brushStateManager,
                       FaweSuggestionProvider faweSuggestionProvider,
                       FaweIntegrationService faweIntegrationService,
                       MessageManager messageManager) { // [NEW]
        super(faweSuggestionProvider);
        this.brushStateManager = brushStateManager;
        this.faweIntegrationService = faweIntegrationService;
        this.messageManager = messageManager; // [NEW]
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        return delegateToFawe(player, args, "/size");
    }

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return false;
        }

        Double size = parseSize(args[0]);
        if (size == null) {
            return false;
        }

        Material tool = getValidToolMaterial(player);
        if (tool == null) {
            return false;
        }

        BrushState state = brushStateManager.resolveActiveState(player, tool);
        if (state == null) {
            return false;
        }

        return applyBrushSize(player, state, size);
    }

    private Double parseSize(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Material getValidToolMaterial(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getType().isBlock()) {
            return null;
        }
        return item.getType();
    }

    private boolean applyBrushSize(Player player, BrushState state, double size) {
        // [ORION_CUT: FaweConstructBrushState]
        if (isFaweDelegatedState(state)) {
            return false; // Делегируем выполнение в FAWE
        }
        // [ORION_CUT_END]

        boolean updated = updateSizeableState(state, size);
        notifySizeResult(player, updated, size);
        return true;
    }

    // [ORION_CUT: FaweConstructBrushState]
    private boolean isFaweDelegatedState(BrushState state) {
        return state instanceof FaweConstructBrushState;
    }
    // [ORION_CUT_END]

    private boolean updateSizeableState(BrushState state, double size) {
        if (state instanceof Sizeable sizeable) {
            sizeable.setSize(size);
            return true;
        }
        return false;
    }

    private void notifySizeResult(Player player, boolean updated, double size) {
        if (updated) {
            messageManager.send(player, OrionMessage.BRUSH_SIZE_SET, size);
        } else {
            messageManager.send(player, OrionMessage.BRUSH_NO_SIZE);
        }
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return COMMANDS;
    }
}