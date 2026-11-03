package com.almarel.orion.framework.command.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.command.CommandProcessor;
import com.almarel.orion.framework.command.CommandProcessorRegistry;
import com.almarel.orion.framework.data.OrionPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class BukkitCommandRegistrar {

    /**
     * Fallback-namespace для Bukkit CommandMap.register(prefix, cmd). Bukkit ВСЕГДА
     * дополнительно регистрирует команду под ключом "prefix:label" в knownCommands,
     * даже если чистое имя уже занято основной регистрацией. Эта строка была равна "1b1v"
     * (как и главная команда плагина), из-за чего "/1b1v" без пробела матчил ВСЕ dummy-команды
     * (1b1v:conf, 1b1v:replace, ...) по префиксу текста в Brigadier-дереве клиента.
     * Строка намеренно НЕ является префиксом "1b1v", чтобы разорвать это совпадение.
     */
    private static final String DUMMY_FALLBACK_PREFIX = "orion-dummy";

    /**
     * Имена, которые Orion намеренно забирает у других плагинов. Чужая запись удаляется
     * из CommandMap, и имя попадает в TakeoverRegistry. Работает только для имён,
     * чей процессор зарегистрирован (в Lite Mirror/Bridge отсутствуют, реестр пустой).
     * См. ADL №15 и TakeoverVisibilityListener (возврат имени в клиентский список после
     * фильтра FAWE в PlayerCommandSendEvent).
     * //mirror: FAWE-алиас //flip (отражение буфера), у Orion отражает регион.
     * //bridge: Arceon (arceon.tool.rope), у Arceon остаётся //rope.
     */
    private static final Set<String> TAKEOVER_NAMES = Set.of("/mirror", "/bridge");

    /**
     * Право видимости пустышки для команд, чей процессор не переопределил getPermission().
     * CORE_USE, а не ALIASES_USE: в Lite ALIASES_USE вырезан (P2-fix). Реальный гейтинг
     * делают процессоры. Список команд на этом fallback пишется в лог при старте.
     */
    private static final String DEFAULT_ALIAS_PERMISSION = OrionPermissions.CORE_USE;

    /**
     * Пустышки для нативных FAWE-команд, чтобы клиент красил одиночный слеш зелёным.
     * Двойной слеш не трогаем: без прав команда должна проваливаться в нативный FAWE.
     */
    private static final List<String> FAWE_PROXY_NAMES = List.of(
            "copy", "cut", "paste",
            "set", "replace", "replacenear", "center",
            "undo", "redo", "flip", "rotate", "expand", "contract"
    );

    private final JavaPlugin plugin;
    private final CommandProcessorRegistry commandProcessorRegistry;
    private final TakeoverRegistry takeoverRegistry;
    private final CommandMapAccessor commandMapAccessor;

    @Inject
    public BukkitCommandRegistrar(
            JavaPlugin plugin
            , CommandProcessorRegistry commandProcessorRegistry,
            TakeoverRegistry takeoverRegistry,
            CommandMapAccessor commandMapAccessor
    ) {
        this.plugin = plugin;
        this.commandProcessorRegistry = commandProcessorRegistry;
        this.takeoverRegistry = takeoverRegistry;
        this.commandMapAccessor = commandMapAccessor;
    }

    public void registerAll() {
        try {
            CommandMap commandMap = commandMapAccessor.getCommandMap();
            if (commandMap == null) {
                plugin.getLogger().warning("Failed to retrieve CommandMap from server.");
                return;
            }

            registerNativeOrionCommands(commandMap);


            stripOwnNamespacedDuplicates(commandMap);

            commandMapAccessor.syncCommands();
        } catch (Exception e) {
            plugin.getLogger().warning("Critical error registering 1b1v commands: " + e.getMessage());
            plugin.getLogger().severe("Error: " + e.getMessage());
        }
    }

    // ---------- регистрация ----------

    private void registerNativeOrionCommands(CommandMap commandMap) {
        Map<String, String> permissions = collectNativePermissions();


        permissions.forEach((name, permission) -> registerDynamicCommand(commandMap, name, permission));
    }

    private Map<String, String> collectNativePermissions() {
        Map<String, String> permissions = new HashMap<>();
        for (String rawCmd : commandProcessorRegistry.getRegisteredCommands()) {
            String clean = cleanCommandName(rawCmd);
            if (clean == null || clean.isEmpty()) {
                continue;
            }
            mergePermission(permissions, clean, permissionOf(rawCmd));
        }
        return permissions;
    }

    private String permissionOf(String rawCmd) {
        CommandProcessor processor = commandProcessorRegistry.getProcessor(rawCmd);
        String permission = (processor == null) ? null : processor.getPermission();
        return (permission != null) ? permission : DEFAULT_ALIAS_PERMISSION;
    }

    private void mergePermission(Map<String, String> permissions, String name, String permission) {
        String existing = permissions.putIfAbsent(name, permission);
        if (existing != null && !existing.equals(permission)) {
            plugin.getLogger().warning("Command '" + name + "' maps to different permissions ("
                    + existing + " / " + permission + "); falling back to " + DEFAULT_ALIAS_PERMISSION);
            permissions.put(name, DEFAULT_ALIAS_PERMISSION);
        }
    }


    private void registerDynamicCommand(CommandMap commandMap, String name, String permission) {
        // "name" → /name, "/name" → //name. Вариант "//name" (= ///name) намеренно не регистрируем.
        registerVariant(commandMap, name, permission);
        registerVariant(commandMap, "/" + name, permission);
    }

    private void registerVariant(CommandMap commandMap, String fullName, String permission) {
        Command existing = commandMap.getCommand(fullName);
        boolean tookOver = false;
        if (existing != null) {
            // Защита от перезаписи нативных команд (FAWE, Essentials, Vanilla): пустышка не нужна,
            // команда проваливается в плагин-владелец. Исключение: явный белый список TAKEOVER_NAMES.
            if (!TAKEOVER_NAMES.contains(fullName)) {
                return;
            }
            commandMap.getKnownCommands().remove(fullName);
            tookOver = true;
        }

        Command cmd = createDummyCommand(fullName, permission);
        commandMap.register(DUMMY_FALLBACK_PREFIX, cmd);
        if (tookOver) {
            takeoverRegistry.register(fullName, cmd);
        }
    }

    private Command createDummyCommand(String fullName, String permission) {
        Command cmd = new OrionDummyCommand(fullName);
        cmd.setPermission(permission);
        cmd.setPermissionMessage(null);
        return cmd;
    }

    /**
     * Маркерный класс пустышек Orion в CommandMap. Named-класс (не анонимный) — чтобы
     * ForeignCommandIndexService мог достоверно отличать "реальная команда любого
     * плагина" от "это наша же пустышка" через instanceof. Это НЕ зависит от того,
     * КАК чужой плагин регистрирует свои команды (classic PluginCommand, кастомный
     * Command-подкласс вроде DynamicPluginCommand у WorldEdit/FAWE, или будущий
     * Brigadier-based механизм) — любой чужой объект в CommandMap автоматически
     * считается "реальным", пока это не наш собственный маркер.
     */
    static final class OrionDummyCommand extends Command {
        OrionDummyCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return true;
        }
    }

    private void stripOwnNamespacedDuplicates(CommandMap commandMap) {
        Command mainCommand = plugin.getCommand(plugin.getName());
        if (mainCommand == null) {
            return;
        }

        String prefix = plugin.getName().toLowerCase();
        Map<String, Command> knownCommands = commandMap.getKnownCommands();

        knownCommands.remove(prefix + ":" + mainCommand.getName().toLowerCase());
        for (String alias : mainCommand.getAliases()) {
            knownCommands.remove(prefix + ":" + alias.toLowerCase());
        }
    }

    // ---------- служебное ----------

    /**
     * Очищает имя команды от слешей для корректной регистрации в Bukkit CommandMap.
     * Bukkit не принимает имена команд, начинающиеся с '/'.
     */
    private String cleanCommandName(String rawName) {
        if (rawName == null) return null;
        return rawName.replace("/", "").toLowerCase();
    }
}