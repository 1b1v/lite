package com.almarel.orion.framework.command.infrastructure;

import com.google.inject.Inject;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import org.bukkit.entity.Player;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;
import org.enginehub.piston.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сервис-посредник для взаимодействия с командным движком FAWE (Piston).
 * Изолирует логику получения подсказок (TabComplete) от WorldEdit.
 */
public class FaweSuggestionProvider {

    private final WorldEditPlugin worldEditPlugin;


    @Inject
    public FaweSuggestionProvider(WorldEditPlugin worldEditPlugin
    ) {
        this.worldEditPlugin = worldEditPlugin;
    }

    // =========================================
    // PUBLIC API
    // =========================================

    /**
     * Используется для маскировки под другие команды (например, внутри AbstractCommand).
     */
    public List<String> getFaweSuggestions(Player player, String[] args, String fakeCommandTemplate) {
        return dispatchSuggestions(player, buildTemplateArguments(fakeCommandTemplate, args));
    }

    /**
     * Используется глобальным перехватчиком (GlobalCommandTabListener) для прямых FAWE команд.
     */
    public List<String> getGlobalFaweSuggestions(Player player, String pistonCmd, String[] args) {
        return dispatchSuggestions(player, buildGlobalArguments(pistonCmd, args));
    }

    private List<String> dispatchSuggestions(Player player, List<String> commandArguments) {
        if (worldEditPlugin == null) return Collections.emptyList();

        try {
            InjectedValueStore store = createPistonStore(player);


            return executePistonQuery(store, commandArguments);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // =========================================
    // PRIVATE HELPERS
    // =========================================

    private List<String> buildGlobalArguments(String pistonCmd, String[] args) {
        List<String> commandArguments = new ArrayList<>(args.length + 1);
        commandArguments.add(pistonCmd);
        Collections.addAll(commandArguments, args);
        return commandArguments;
    }

    private List<String> executePistonQuery(InjectedValueStore store, List<String> mutatedArgs) {
        try {
            CommandManager pistonManager = getPistonCommandManager();
            Set<Suggestion> suggestions = pistonManager.getSuggestions(store, mutatedArgs);
            return extractSuggestionStrings(suggestions);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private CommandManager getPistonCommandManager() {
        PlatformCommandManager pcm = WorldEdit.getInstance().getPlatformManager().getPlatformCommandManager();
        return pcm.getCommandManager();
    }

    private List<String> extractSuggestionStrings(Set<Suggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>(suggestions.size());
        for (Suggestion s : suggestions) {
            result.add(s.getSuggestion());
        }
        return result;
    }

    private InjectedValueStore createPistonStore(Player player) {
        InjectedValueStore store = MapBackedValueStore.create();

        Actor actor = worldEditPlugin.wrapCommandSender(player);
        store.injectValue(Key.of(Actor.class), context -> Optional.of(actor));

        com.sk89q.worldedit.entity.Player wePlayer = worldEditPlugin.wrapPlayer(player);
        store.injectValue(Key.of(com.sk89q.worldedit.entity.Player.class), context -> Optional.of(wePlayer));

        return store;
    }

    private List<String> buildTemplateArguments(String template, String[] args) {
        List<String> commandArguments = new ArrayList<>();
        String[] templateWords = template.split(" ");
        Collections.addAll(commandArguments, templateWords);

        // Шаблон вида "/tool" (1 слово) не содержит ничего из args — нужно добавить ВСЕ args,
        // начиная с args[0] (частично введённый текст для фильтрации Piston).
        // Шаблон вида "/tool farwand" (2 слова) уже содержит args[0] литералом внутри строки —
        // его нужно пропустить, добавляются только args[1..] (аргументы ПОСЛЕ имени тула).
        // Общая формула: consumedArgsCount = количество слов в template сверх самой команды.
        int consumedArgsCount = templateWords.length - 1;
        for (int i = consumedArgsCount; i < args.length; i++) {
            commandArguments.add(args[i]);
        }
        return commandArguments;
    }
}