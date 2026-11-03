package com.almarel.orion.framework.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
/**
 * Базовый контракт для всех обработчиков команд Orion.
 * Объединяет логику выполнения (Execute) и автодополнения (TabComplete).
 */
public interface CommandProcessor {

    /**
     * Основной метод выполнения команды.
     * @param player Игрок, вызвавший команду
     * @param label Метка команды (например, "copy", без слеша)
     * @param args Аргументы команды (без самой команды)
     * @return true, если команда была обработана (и нужно отменить ванильное событие)
     */
    boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args);

    /**
     * Метод для генерации подсказок (Tab Complete).
     * @param player Игрок
     * @param label Метка команды
     * @param args Аргументы
     * @return Список строк-подсказок. Если пустой список - подсказок нет.
     */
    @NotNull
    default List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }

    /**
     * Право, по которому клиент красит команду (зелёная/красная в чате): Bukkit-пустышка
     * регистрируется с этим permission. ЕДИНЫЙ источник: process() и onTabComplete()
     * должны проверять ту же ноду. null → пустышка получает CORE_USE (команда без
     * собственного права). Для команд, имя которых занято нативным плагином (FAWE, Essentials),
     * пустышка не создаётся и цвет определяет владелец имени.
     */
    @Nullable
    default String getPermission() {
        return null;
    }

    /**
     * Отличает ДВА сценария "своей команды без прав" в GlobalCommandTabListener.tryHandleOrionCommand():
     *
     * 1. FAWE-прокси (/copy, /set, /brush...) — пустышка Orion зарегистрирована ПОВЕРХ имени
     *    (BukkitCommandRegistrar.FAWE_PROXY_NAMES), естественный провайдер подсказок подменён.
     *    Процессор обязан сам достать подсказки из Piston (FaweSuggestionProvider — async-safe).
     *    allowsForeignTabFallback() = false (дефолт): event.setHandled(true) ставится всегда.
     *
     * 2. Команда делит имя со сторонним плагином вне TAKEOVER_NAMES — пустышка не создаётся,
     *    оригинальный PluginCommand владельца жив в CommandMap (см. SpeedCommand vs
     *    EssentialsX /speed). Сторонний TabCompleter не рассчитан на async-вызов
     *    (AsyncTabCompleteEvent выполняется вне main thread), поэтому дёргать его напрямую нельзя.
     *    allowsForeignTabFallback() = true: при пустом результате onTabComplete() листенер
     *    НЕ трогает event, и Paper сам, синхронно в main thread, вызывает нативный tabComplete()
     *    владельца имени — то есть Orion как будто не перехватывал команду вовсе.
     *
     * @return true — пустой onTabComplete() не блокирует событие, позволяя откатиться
     * на синхронный нативный tab-complete стороннего владельца имени.
     */
    default boolean allowsForeignTabFallback() {
        return false;
    }

    /**
     * Возвращает список команд и алиасов, которые этот процессор перехватывает.
     * Например: ["//copy", "/copy", "//c"]
     */
    @NotNull
    Collection<String> getCommands();
}