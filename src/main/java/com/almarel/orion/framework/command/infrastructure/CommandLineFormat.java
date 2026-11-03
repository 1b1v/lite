package com.almarel.orion.framework.command.infrastructure;

/**
 * SSOT лексического анализа сырой командной строки Bukkit/FAWE (label + args).
 * Используется routing-слоем ДО того, как команда попала к конкретному обработчику:
 * CommandInterceptor, AliasRoutingService, GlobalCommandTabListener, AliasTabListener,
 * MacroRecorder.
 */
public class CommandLineFormat {

    /**
     * Удаляет абсолютно все ведущие слеши (/, //, ///) из начала команды.
     * Пример: "//br" -> "br", "/sphere" -> "sphere".
     */
    public static String stripSlashes(String rawCommand) {
        if (rawCommand == null) return null;
        return rawCommand.replaceAll("^/+", "");
    }

    /**
     * Подготавливает команду для поиска в HashMap реестрах.
     * Удаляет слеши и переводит строку в нижний регистр.
     * Пример: "//Replace" -> "replace".
     */
    public static String cleanAlias(String rawCommand) {
        if (rawCommand == null) return null;
        return stripSlashes(rawCommand).toLowerCase();
    }

    /**
     * Нормализует команду для внутреннего движка Piston (FAWE).
     * Piston ожидает строго ОДИН ведущий слеш, даже если оригинал имел два.
     * Пример: "//re" -> "/re", "set" -> "/set".
     */
    public static String formatForPiston(String rawCommand) {
        if (rawCommand == null) return null;
        String stripped = stripSlashes(rawCommand);
        return "/" + stripped;
    }

    /**
     * Извлекает label (первый токен до первого пробела) из полной командной строки.
     * Регистр не меняется. "/br gen sphere" -> "/br". "/br" -> "/br" (пробела нет).
     */
    public static String extractLabel(String message) {
        if (message == null) return "";
        int spaceIdx = message.indexOf(' ');
        return spaceIdx == -1 ? message : message.substring(0, spaceIdx);
    }

    /**
     * Извлекает хвост аргументов КАК ЦЕЛЬНУЮ СТРОКУ (без разбиения на токены).
     * "/br gen sphere" -> "gen sphere". "/br" -> "".
     */
    public static String extractArgsLine(String message) {
        if (message == null) return "";
        int spaceIdx = message.indexOf(' ');
        return spaceIdx == -1 ? "" : message.substring(spaceIdx + 1);
    }

    /**
     * Разбивает командную строку на токены аргументов (без label).
     * Если пробела в строке нет вообще — возвращает ПУСТОЙ массив (а не {""}).
     *
     * @param preserveTrailingEmpty true — для Tab-complete (отличить "set " от "set");
     *                               false — для обычного выполнения команды.
     */
    public static String[] extractArgs(String message, boolean preserveTrailingEmpty) {
        if (message == null) return new String[0];
        int spaceIdx = message.indexOf(' ');
        if (spaceIdx == -1) return new String[0];
        String argsLine = message.substring(spaceIdx + 1);
        return argsLine.split(" ", preserveTrailingEmpty ? -1 : 0);
    }

    /**
     * Проверяет, что команда введена РОВНО с одним ведущим слешем (не ноль, не два и более).
     * Используется для Anti-Shadowing: защищает двойной слеш (нативный синтаксис FAWE/Piston)
     * от попадания в логику, предназначенную только для single-slash shortcuts.
     * Пример: "/stack" -> true, "//stack" -> false, "stack" -> false.
     */
    public static boolean isSingleSlash(String rawCommand) {
        if (rawCommand == null) return false;
        return rawCommand.startsWith("/") && !rawCommand.startsWith("//");
    }
}