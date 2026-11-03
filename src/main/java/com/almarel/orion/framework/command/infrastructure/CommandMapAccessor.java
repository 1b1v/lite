package com.almarel.orion.framework.command.infrastructure;

import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * SSOT-доступ к внутреннему CommandMap сервера через рефлексию.
 * Вынесено отдельным сервисом, чтобы не дублировать хрупкую рефлексию
 * в BukkitCommandInjector и ForeignCommandRegistry (DRY).
 */
@Singleton
public class CommandMapAccessor {

    private volatile CommandMap cachedCommandMap;

    public CommandMap getCommandMap() {
        CommandMap local = cachedCommandMap;
        if (local != null) {
            return local;
        }
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            local = (CommandMap) commandMapField.get(Bukkit.getServer());
            cachedCommandMap = local;
            return local;
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().warning("Failed to retrieve CommandMap from server: " + e);
            return null;
        }
    }

    /**
     * Пересобирает клиентское дерево команд после ручной регистрации в CommandMap.
     * В Paper 1.21.8 метод публичный у CraftServer, но доступ через рефлексию
     * спасает от смены пакета/реализации при обновлении API.
     */
    public void syncCommands() {
        try {
            Class<?> craftServerClass = Bukkit.getServer().getClass();
            Method syncCommandsMethod = craftServerClass.getDeclaredMethod("syncCommands");
            syncCommandsMethod.setAccessible(true);
            syncCommandsMethod.invoke(Bukkit.getServer());
        } catch (Exception e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            Bukkit.getLogger().warning("syncCommands failed: " + e + " / cause: " + cause);
        }
    }
}