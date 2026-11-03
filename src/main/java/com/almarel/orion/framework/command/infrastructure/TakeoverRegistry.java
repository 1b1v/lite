package com.almarel.orion.framework.command.infrastructure;

import com.google.inject.Singleton;
import org.bukkit.command.Command;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Имена, фактически забранные Orion у других плагинов (ADL №15), и наши пустышки под ними.
 * Заполняется BukkitCommandInjector в onEnable, читается слушателем на main thread.
 * В Lite остаётся пустым.
 */
@Singleton
public class TakeoverRegistry {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(String name, Command command) {
        commands.put(name, command);
    }

    public Map<String, Command> view() {
        return Collections.unmodifiableMap(commands);
    }
}