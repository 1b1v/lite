package com.almarel.orion.framework.command.infrastructure;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.google.inject.Inject;
import com.almarel.orion.framework.command.CommandProcessorRegistry;
import com.almarel.orion.framework.command.CommandProcessor;
import com.almarel.orion.framework.data.OrionPermissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class GlobalCommandTabListener implements Listener {

    private final CommandProcessorRegistry commandRegistry;
    private final FaweSuggestionProvider faweSuggestionProvider;


    @Inject
    public GlobalCommandTabListener(
            CommandProcessorRegistry commandRegistry
            , FaweSuggestionProvider faweSuggestionProvider
    ) {
        this.commandRegistry = commandRegistry;
        this.faweSuggestionProvider = faweSuggestionProvider;
    }

    @EventHandler
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (buffer == null || buffer.isEmpty() || !buffer.startsWith("/")) return;
        if (!(event.getSender() instanceof Player player)) return;

        TabCommandContext ctx = parseAndNormalize(buffer, player);
        if (ctx == null) return;



        // 3. Стандартные команды Orion
        if (tryHandleOrionCommand(event, player, ctx)) return;

        // 4. Глобальный перехватчик FAWE (Piston)
        if (isFaweCommand(ctx.effectiveLabel())) {
            handleFaweCommand(event, player, ctx);
        }
    }

    // =========================================
    // PARSING & NORMALIZATION
    // =========================================

    private TabCommandContext parseAndNormalize(String buffer, Player player) {
        String rawLabel = CommandLineFormat.extractLabel(buffer).toLowerCase();
        String cleanLabel = CommandLineFormat.cleanAlias(rawLabel);

        if (isIgnoredArceonCommand(cleanLabel)) return null;

        String effectiveLabel = resolveEffectiveLabel(rawLabel, cleanLabel, player);
        String cmd = CommandLineFormat.stripSlashes(effectiveLabel);
        String[] args = CommandLineFormat.extractArgs(buffer, true);

        return new TabCommandContext(effectiveLabel, cmd, args);
    }

    private boolean isIgnoredArceonCommand(String cleanLabel) {
        return cleanLabel.equals("arcbrush") || cleanLabel.equals("arceon")
                || cleanLabel.equals("ezbr") || cleanLabel.equals("ezbrush");
    }

    private String resolveEffectiveLabel(String rawLabel, String cleanLabel, Player player) {
        return rawLabel;
    }

    // =========================================
    // HANDLERS
    // =========================================

    private boolean tryHandleOrionCommand(AsyncTabCompleteEvent event, Player player, TabCommandContext ctx) {
        CommandProcessor processor = commandRegistry.getProcessor(ctx.effectiveLabel());
        if (processor == null) return false;

        List<String> suggestions = processor.onTabComplete(player, ctx.cmd(), ctx.args());

        if ((suggestions == null || suggestions.isEmpty()) && processor.allowsForeignTabFallback()) {
            // Не трогаем event: Paper синхронно, в main thread, откатится на нативный
            // tabComplete() стороннего плагина, владеющего этим именем (см. Javadoc метода).
            return false;
        }

        event.setHandled(true);
        if (suggestions != null && !suggestions.isEmpty()) {
            for (String s : suggestions) {
                event.completions().add(AsyncTabCompleteEvent.Completion.completion(s));
            }
        }
        return true;
    }


    private void handleFaweCommand(AsyncTabCompleteEvent event, Player player, TabCommandContext ctx) {
        String pistonCmd = CommandLineFormat.formatForPiston(ctx.effectiveLabel());

        List<String> finalSuggestions = faweSuggestionProvider.getGlobalFaweSuggestions(player, pistonCmd, ctx.args());

        if (finalSuggestions != null && !finalSuggestions.isEmpty()) {
            event.setHandled(true);
            for (String s : finalSuggestions) {
                event.completions().add(AsyncTabCompleteEvent.Completion.completion(s));
            }
        }
    }

    private boolean isFaweCommand(String label) {
        return label.startsWith("//")
                || label.equals("/brush") || label.equals("/br")
                || label.equals("/tool") || label.equals("/mask") || label.equals("/gmask");
    }

    private record TabCommandContext(String effectiveLabel, String cmd, String[] args) {}
}