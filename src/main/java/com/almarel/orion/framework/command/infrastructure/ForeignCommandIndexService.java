package com.almarel.orion.framework.command.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Core-сервис (не вырезается). Динамический детектор коллизий с командами сторонних
 * плагинов (Multiverse /mv, Factions /f, EssentialsX /tree, WildStacker /stack и т.д.).
 *
 * Потребители:
 *  - CommandInterceptor.tryResolveSyntheticTool() — безусловная защита от перехвата
 *    чужих команд как синтетических /tool <alias> (FaweBrushRegistry.isTool()).
 *  - [Shortcut System, опционально] AliasRoutingService, GlobalCommandTabListener —
 *    защита статических алиасов и Smart Auto-Slash.
 *
 * Расположен в core, а не в com.almarel.orion.shortcut, т.к. защищает в том числе
 * CommandInterceptor — всегда включённый движок, не зависящий от прав
 * 1b1v.aliases.use / 1b1v.singleslash.use.
 *
 * "Не чужой" считается:
 *  - команда, которой нет в CommandMap вообще (свободное имя);
 *  - команда, зарегистрированная WorldEdit/FAWE (ожидаемый нативный прокси);
 *  - собственные пустышки Orion — НЕ PluginCommand (BukkitCommandRegistrar.createDummyCommand()).
 *    Это же защищает команды, захваченные через TAKEOVER_NAMES (//bridge у Arceon):
 *    после захвата объект в CommandMap — уже не PluginCommand.
 *
 * Снэпшот строится на ServerLoadEvent (гарантированно ПОСЛЕ onEnable() всех плагинов,
 * включая захват TAKEOVER_NAMES в BukkitCommandRegistrar) и обновляется на
 * PluginEnableEvent для горячей догрузки плагинов после старта сервера.
 */
@Singleton
public class ForeignCommandIndexService implements Listener {

    private final CommandMapAccessor commandMapAccessor;
    private final JavaPlugin plugin;
    private final WorldEditPlugin worldEditPlugin;

    private volatile Set<String> foreignLabels = Set.of();
    private volatile Set<String> nativelyRegisteredLabels = Set.of();

    @Inject
    public ForeignCommandIndexService(CommandMapAccessor commandMapAccessor,
                                      JavaPlugin plugin,
                                      WorldEditPlugin worldEditPlugin) {
        this.commandMapAccessor = commandMapAccessor;
        this.plugin = plugin;
        this.worldEditPlugin = worldEditPlugin;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        rebuildSnapshot();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin() == plugin) {
            return;
        }
        rebuildSnapshot();
    }

    private void rebuildSnapshot() {
        CommandMap commandMap = commandMapAccessor.getCommandMap();
        if (commandMap == null) {
            return;
        }

        Set<String> foreign = new HashSet<>();
        Set<String> natively = new HashSet<>();

        for (Map.Entry<String, Command> entry : commandMap.getKnownCommands().entrySet()) {
            Command command = entry.getValue();
            String key = entry.getKey().toLowerCase();

            if (isAnyRealCommand(command)) {
                natively.add(key);
            }
            if (isForeignCommand(command)) {
                foreign.add(key);
            }
        }

        this.foreignLabels = Set.copyOf(foreign);
        this.nativelyRegisteredLabels = Set.copyOf(natively);
    }

    /**
     * true, если слот в CommandMap занят ЛЮБЫМ реальным объектом команды — чьим угодно,
     * включая саму FAWE (DynamicPluginCommand), — кроме нашей собственной пустышки
     * (BukkitCommandRegistrar.OrionDummyCommand). Используется там, где важен сам факт
     * "это имя уже реально работает" (AliasRoutingService.tryAutoDoubleSlash), а не то,
     * какому именно плагину оно принадлежит.
     */
    private boolean isAnyRealCommand(Command command) {
        if (command == null) {
            return false;
        }
        return !(command instanceof BukkitCommandRegistrar.OrionDummyCommand);
    }

    private boolean isForeignCommand(Command command) {
        if (command instanceof BukkitCommandRegistrar.OrionDummyCommand) {
            return false; // Наша же пустышка — никогда не чужая.
        }

        if (command instanceof PluginCommand pluginCommand) {
            return pluginCommand.getPlugin() != plugin && pluginCommand.getPlugin() != worldEditPlugin;
        }

        // Реальный объект команды, НЕ являющийся стандартным PluginCommand (пример:
        // FAWE регистрирует свои команды через com.sk89q.bukkit.util.DynamicPluginCommand —
        // подтверждено эмпирически при расследовании //stack). Namespaced-дубликат
        // "plugin:label" в CommandMap НЕ гарантирован для таких кастомных регистраций
        // (подтверждено Kimi) — полагаться на текст ключа карты нельзя.
        //
        // Fail-CLOSED по умолчанию: если это не наша пустышка и не узнаваемо как "свой"
        // партнёр (FAWE/WorldEdit) — считаем команду чужой, НЕ трогаем. Это защищает
        // от любых будущих нестандартных механизмов регистрации сторонних плагинов
        // (например, Multiverse-Core через Paper Brigadier LifecycleEventManager на
        // новых версиях MC), даже если мы никогда явно не видели их класс.
        //
        // FAWE/WorldEdit — единственный официально узнаваемый carve-out: все его
        // классы живут в пакете com.sk89q. (пространство имён проекта), это
        // характеристика зависимости, на которой компилируется сам Orion, а не догадка
        // о внутренностях произвольного стороннего плагина.
        return !command.getClass().getName().startsWith("com.sk89q.");
    }

    /**
     * @param cleanLabel имя команды без слешей, любой регистр (например "tree").
     * @return true, если это имя реально занято сторонним плагином на этом сервере.
     */
    public boolean isForeignOwned(String cleanLabel) {
        return foreignLabels.contains(cleanLabel.toLowerCase());
    }

    /**
     * @param cleanLabel имя команды без слешей, любой регистр.
     * @return true, если это имя УЖЕ реально зарегистрировано как рабочая команда любым
     *         плагином (включая сам FAWE/WorldEdit) — не дамми Orion и не свободное место.
     */
    public boolean isNativelyRegistered(String cleanLabel) {
        return nativelyRegisteredLabels.contains(cleanLabel.toLowerCase());
    }
}