package com.almarel.orion.framework.command;

import com.google.inject.Inject;
import com.almarel.orion.framework.command.infrastructure.ForeignCommandIndexService;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.command.infrastructure.CommandLineFormat;
import com.almarel.orion.framework.command.preprocessing.CommandPreprocessorStep;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import com.almarel.orion.brush.fawe.NativeCaptureService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Центральный роутер входящих команд.
 * Перехватывает сообщения, проверяет их наличие в реестре Orion и выполняет.
 */
public class CommandInterceptor implements Listener {

    private final JavaPlugin plugin;

    private final ForeignCommandIndexService foreignCommandIndexService;




    private final CommandProcessorRegistry commandRegistry;
    private final NativeCaptureService nativeCaptureService;
    private final FaweBrushRegistry faweBrushRegistry;
    private final Set<CommandPreprocessorStep> preprocessorSteps;




    @Inject
    public CommandInterceptor(
            JavaPlugin plugin,
            CommandProcessorRegistry commandRegistry,
            NativeCaptureService nativeCaptureService,
            FaweBrushRegistry faweBrushRegistry,
            ForeignCommandIndexService foreignCommandIndexService,
            Set<CommandPreprocessorStep> preprocessorSteps
    ) {
        this.plugin = plugin;




        this.commandRegistry = commandRegistry;
        this.nativeCaptureService = nativeCaptureService;
        this.faweBrushRegistry = faweBrushRegistry;
        this.foreignCommandIndexService = foreignCommandIndexService;
        this.preprocessorSteps = preprocessorSteps;



    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();


        if (handleEarlyInterceptions(player, message, event)) {
            return;
        }

        message = runPreprocessingPipeline(player, message, event);
        CommandContext context = resolveCommandContext(player, message);


        dispatchCommand(player, message, context, event);
    }

    private String runPreprocessingPipeline(Player player, String message, PlayerCommandPreprocessEvent event) {
        message = applyGlobalAliases(player, message);


        for (CommandPreprocessorStep step : preprocessorSteps) {
            message = step.process(message);
        }

        if (!message.equals(event.getMessage())) {
            event.setMessage(message);
        }

        return message;
    }

    private void dispatchCommand(Player player, String message, CommandContext context, PlayerCommandPreprocessEvent event) {
        if (context.processor != null && context.processor.process(player, context.label, context.args)) {
            event.setCancelled(true);
            return;
        }

        if (context.isFawe) {
            nativeCaptureService.captureState(player, context.label, context.args, false);
        }
    }

    private CommandContext resolveCommandContext(Player player, String message) {
        String label = CommandLineFormat.extractLabel(message).toLowerCase();
        String[] args = CommandLineFormat.extractArgs(message, false);

        CommandProcessor processor = commandRegistry.getProcessor(label);
        boolean isFawe = nativeCaptureService.isExternalBrushCommand(label);

        if (processor == null && !isFawe) {
            CommandContext syntheticTool = tryResolveSyntheticTool(player, label, args);
            if (syntheticTool != null) {
                return syntheticTool;
            }
        }

        return new CommandContext(label, args, isFawe, processor);
    }

    private CommandContext tryResolveSyntheticTool(Player player, String label, String[] args) {
        // Floor-gate: игрок без базового доступа к 1b1v не должен получать перехват
        // нативной single-slash команды FAWE под видом синтетического /tool <alias>.
        // Без этого гейта отказ в праве показывается с брендингом "[1b1v]", хотя
        // игрок вообще не взаимодействует с плагином — команда должна провалиться
        // в штатную обработку Bukkit/FAWE нетронутой.
        if (!player.hasPermission(OrionPermissions.CORE_USE)) {
            return null;
        }

        // Anti-Shadowing: синтетический /tool <alias> собираем ТОЛЬКО из single-slash ввода.
        // Двойной слеш (//stack, //tree, //info...) — это нативный синтаксис Piston/FAWE,
        // он должен провалиться дальше нетронутым, а не быть перехвачен как shortcut инструмента.
        if (!CommandLineFormat.isSingleSlash(label)) {
            return null;
        }

        String aliasToCheck = CommandLineFormat.stripSlashes(label);

        if (foreignCommandIndexService.isForeignOwned(aliasToCheck)) {
            return null;
        }

        if (!faweBrushRegistry.isTool(aliasToCheck)) {
            return null;
        }

        String syntheticLabel = "/tool";
        String[] syntheticArgs = prependArgument(aliasToCheck, args);
        return new CommandContext(syntheticLabel, syntheticArgs, true, commandRegistry.getProcessor(syntheticLabel));
    }

    private String[] prependArgument(String firstArg, String[] remainingArgs) {
        String[] result = new String[remainingArgs.length + 1];
        result[0] = firstArg;
        System.arraycopy(remainingArgs, 0, result, 1, remainingArgs.length);
        return result;
    }

    // --- PRIVATE LOGIC BLOCKS ---

    private boolean handleEarlyInterceptions(Player player, String message, PlayerCommandPreprocessEvent event) {



        // [CLEAN ARCHITECTURE] Пассивный перехват делегирован изолированному сервису

        return false;
    }

    private String applyGlobalAliases(Player player, String message) {
        return message;
    }

    private record CommandContext(
            String label,
            String[] args,
            boolean isFawe,
            CommandProcessor processor
    ) {}
}