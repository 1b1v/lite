package com.almarel.orion.brush.fawe;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.Maskable;
import com.almarel.orion.brush.data.capabilities.Patternable;
import com.almarel.orion.brush.data.capabilities.Sizeable;
import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import com.almarel.orion.brush.state.BrushStateManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

/**
 * Сервис, отвечающий за "Шпионаж" за командами FAWE.
 * Он определяет, нужно ли Orion'у реагировать на внешнюю команду (например, /br sphere)
 * и сохранять её в состояние кисти.
 */
public class NativeCaptureService {

    private final TaskRunner taskRunner;
    private final FaweIntegrationService faweIntegrationService;
    private final BrushStateManager brushStateManager;
    private final FaweBrushRegistry aliasManager;
    private final ExternalCommandParser commandParser;
    private final ExternalStateFactory stateFactory;

    @Inject
    public NativeCaptureService(TaskRunner taskRunner,
                                FaweIntegrationService faweIntegrationService,
                                BrushStateManager brushStateManager,
                                FaweBrushRegistry aliasManager,
                                ExternalCommandParser commandParser,
                                ExternalStateFactory stateFactory) {
        this.taskRunner = taskRunner;
        this.faweIntegrationService = faweIntegrationService;
        this.brushStateManager = brushStateManager;
        this.aliasManager = aliasManager;
        this.commandParser = commandParser;
        this.stateFactory = stateFactory;
    }

    /**
     * Проверяет, является ли команда внешней командой настройки кисти (FAWE/Arceon/EzEdits).
     */
    public boolean isExternalBrushCommand(String label) {
        return commandParser.isExternalCommandPrefix(label.toLowerCase());
    }

    /**
     * Пытается захватить состояние кисти после выполнения команды.
     * @param immediate Если true, проверка выполняется мгновенно.
     */
    public void captureState(Player player, String label, String[] args, boolean immediate) {
        Material tool = player.getInventory().getItemInMainHand().getType();
        if (isInvalidTool(tool)) return;

        String rawCommand = label + " " + String.join(" ", args);
        ParsedCommand cmd = commandParser.parse(rawCommand);
        if (cmd == null || cmd.type() == ParsedCommand.CommandType.UNKNOWN) return;

        scheduleCapture(() -> performCapture(player, tool, label, cmd), immediate);
    }

    private boolean isInvalidTool(Material tool) {
        return tool == Material.AIR || tool.isBlock();
    }

    private void scheduleCapture(Runnable captureTask, boolean immediate) {
        if (immediate) {
            captureTask.run();
        } else {
            // Задержка 2 тика для надежной синхронизации с FAWE
            taskRunner.runSyncLater(captureTask, 2L);
        }
    }

    private void performCapture(Player player, Material tool, String label, ParsedCommand cmd) {
        if (!player.isOnline()) return;

        switch (cmd.type()) {
            case CLEAR -> brushStateManager.removeBrushSettings(player, tool);
            case CREATION -> handleCreation(player, tool, label, cmd);
            case MODIFICATION -> handleModification(player, tool, cmd);
        }
    }

    private void handleCreation(Player player, Material tool, String label, ParsedCommand cmd) {
        if (!isSupportedCommand(cmd)) return;

        if (faweIntegrationService.hasActiveBrush(player, tool)) {
            BrushState newState = stateFactory.createState(tool, label, cmd.alias(), cmd.args());
            brushStateManager.putSyncedBrushSettings(player, tool, newState);
        }
    }

    private boolean isSupportedCommand(ParsedCommand cmd) {
        String brushName = cmd.alias();
        return aliasManager.getBrushProcessor(brushName) != null
                || aliasManager.getToolProcessor(brushName) != null
                || cmd.source() == ParsedCommand.CommandSource.EZEDITS;
    }

    private void handleModification(Player player, Material tool, ParsedCommand cmd) {
        BrushState activeState = brushStateManager.resolveActiveState(player, tool);
        if (activeState == null) return;

        processModification(activeState, cmd.alias(), cmd.args());
    }

    /**
     * Единая точка обработки модификаций для ВСЕХ типов кистей.
     * Опирается на Feature-интерфейсы DTO (SRP). Если DTO не поддерживает фичу, изменение игнорируется.
     */
    private void processModification(BrushState state, String alias, String[] args) {
        if (args.length == 0) return;
        String argString = String.join(" ", args);

        switch (alias) {
            case "mat", "material" -> applyPattern(state, argString);
            case "mask" -> applyMask(state, argString);
            case "size", "range" -> applySize(state, args[0]);
        }
    }

    private void applyPattern(BrushState state, String pattern) {
        if (state instanceof Patternable patternable) {
            patternable.setPattern(pattern);
        }
    }

    private void applyMask(BrushState state, String mask) {
        if (state instanceof Maskable maskable) {
            maskable.setMask(mask);
        }
    }

    private void applySize(BrushState state, String rawSize) {
        try {
            double val = Double.parseDouble(rawSize);
            if (state instanceof Sizeable sizeable) {
                sizeable.setSize(val);
            }
        } catch (NumberFormatException ignored) {
        }
    }
}