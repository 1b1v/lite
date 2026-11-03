package com.almarel.orion.framework.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class CommandProcessorRegistry {

    private final Map<String, CommandProcessor> commandMap = new HashMap<>();

    @Inject
    public CommandProcessorRegistry() {
    }

    public void register(CommandProcessor processor) {
        for (String command : processor.getCommands()) {
            commandMap.put(command.toLowerCase(), processor);
        }
    }

    @Nullable
    public CommandProcessor getProcessor(String label) {
        if (label == null) return null;
        return commandMap.get(label.toLowerCase());
    }

    public Set<String> getRegisteredCommands() {
        return commandMap.keySet();
    }
}