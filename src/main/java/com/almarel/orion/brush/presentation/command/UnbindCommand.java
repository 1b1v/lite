package com.almarel.orion.brush.presentation.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.command.AbstractCommand; // [CHANGED]
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider; // [NEW]
import com.almarel.orion.framework.message.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Singleton
public class UnbindCommand extends AbstractCommand { // [CHANGED] Наследуем

    private final BrushStateManager stateManager;
    private final MessageManager messageManager;

    private static final List<String> COMMANDS = List.of("/none", "//none");

    @Inject
    public UnbindCommand(BrushStateManager stateManager,
                         MessageManager messageManager,
                         FaweSuggestionProvider faweService) { // [NEW] Inject service
        super(faweService); // Передаем родителю
        this.stateManager = stateManager;
        this.messageManager = messageManager;
    }

    // --- Tab Completion Logic ---
    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        // У команды /none нет аргументов
        return List.of();
    }

    // --- Execution Logic (Старая) ---
    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        // 1. Снимаем все кисти Orion
        handleUnbindCommand(player);

        // 2. Явно снимаем кисть FAWE (чтобы очистить метаданные)
        Material tool = player.getInventory().getItemInMainHand().getType();
        stateManager.removeBrushSettings(player, tool);

        // Возвращаем false, чтобы позволить FAWE/WorldEdit также обработать эту команду, если нужно
        return false;
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return COMMANDS;
    }

    private void handleUnbindCommand(Player p) {
        Material heldTool = p.getInventory().getItemInMainHand().getType();
        if (heldTool == Material.AIR) return;

        String toolName = heldTool.name().toLowerCase(Locale.ROOT);
        boolean any = false;

        // Получаем текущее состояние ДО удаления, чтобы узнать, какую кисть мы отвязываем
        BrushState state = stateManager.resolveActiveState(p, heldTool);

        if (state != null) {
            String typeName = state.getBrushType().name().toLowerCase(Locale.ROOT);

            if (stateManager.removeBrushSettings(p, heldTool)) {
                messageManager.send(p, com.almarel.orion.framework.message.OrionMessage.BRUSH_UNBIND, typeName, toolName);
                any = true;
            }
        }

        // Если нужно отвязать ещё и FAWE/Arceon сессии:
        stateManager.unbindExternalBrushes(p, heldTool);
    }
}