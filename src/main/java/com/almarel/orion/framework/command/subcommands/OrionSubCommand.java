package com.almarel.orion.framework.command.subcommands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface OrionSubCommand {

    /**
     * Возвращает имя подкоманды (например, "defaults", "reload").
     * Используется для маршрутизации в OrionCommand.
     */
    String getName();

    /**
     * Возвращает алиасы подкоманды (например, "m", "menu" для "buffer").
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Выполняет логику подкоманды.
     * @return true, если команда обработана корректно
     */
    boolean execute(CommandSender sender, String[] args);

    /**
     * Возвращает список автодополнений для подкоманды.
     * @return список предложений (может быть пустым)
     */
    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Возвращает пермишен, необходимый для выполнения команды.
     * Если null — команда доступна всем (проверяется внутри).
     */
    default String getPermission() {
        return null;
    }

    /**
     * Если true — команда исполняется нормально, но никогда не появляется
     * в tab-complete подсказках (ни при "/1b1v ", ни при частичном вводе имени),
     * даже для отправителя, у которого есть право на её использование.
     * Для скрытых диагностических/служебных команд.
     */
    default boolean isHidden() {
        return false;
    }
}