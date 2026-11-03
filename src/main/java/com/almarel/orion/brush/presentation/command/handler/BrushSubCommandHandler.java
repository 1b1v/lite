package com.almarel.orion.brush.presentation.command.handler;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Контракт для процедурных субкоманд кистей.
 * Позволяет безопасно вырезать фичи (Feature Cutting) без сломанных импортов в роутерах.
 */
public interface BrushSubCommandHandler {

    /**
     * Проверяет, поддерживает ли этот хендлер указанную субкоманду (например, "crystal", "g").
     *
     * @param subCommand название субкоманды (в нижнем регистре).
     * @return true, если хендлер должен перехватить выполнение.
     */
    boolean canHandle(@NotNull String subCommand);

    /**
     * Выполняет логику обработки команды.
     *
     * @param player      игрок, вызвавший команду.
     * @param subCommand  субкоманда (для обработки алиасов, например "gen" и "g").
     * @param args        аргументы команды (массив после имени субкоманды).
     */
    void handle(@NotNull Player player, @NotNull String subCommand, @NotNull String[] args);

    /**
     * Генерирует подсказки для таб-комплита.
     *
     * @param player      игрок.
     * @param subCommand  субкоманда.
     * @param args        текущие аргументы.
     * @return список допустимых подсказок.
     */
    @NotNull
    List<String> onTabComplete(@NotNull Player player, @NotNull String subCommand, @NotNull String[] args);



    /**
     * Возвращает все алиасы субкоманды (например, List.of("cp", "copypaste", "cb")).
     * Используется фасадом для генерации TabComplete.
     */
    @NotNull
    Collection<String> getAliases();

    /**
     * Возвращает базовый пермишен для доступа к этой субкоманде.
     * Используется фасадом для фильтрации подсказок.
     */
    @NotNull
    String getRequiredPermission();
}