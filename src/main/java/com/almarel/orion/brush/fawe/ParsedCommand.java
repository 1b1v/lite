package com.almarel.orion.brush.fawe;

import java.util.Arrays;

/**
 * Immutable Data Object, представляющий результат парсинга внешней команды (FAWE/Arceon/EzEdits).
 * Создается строго через new. Не содержит бизнес-логики.
 */
public record ParsedCommand(CommandSource source, CommandType type, String alias, String[] args) {

    public enum CommandSource {
        FAWE, ARCEON, EZEDITS, UNKNOWN
    }

    public enum CommandType {
        CREATION, MODIFICATION, CLEAR, UNKNOWN
    }

    public static final ParsedCommand UNKNOWN = new ParsedCommand(CommandSource.UNKNOWN, CommandType.UNKNOWN, "", new String[0]);

    // Compact Constructor (Java 16+)
    public ParsedCommand {
        if (args == null) {
            args = new String[0];
        }
    }

    @Override
    public String toString() {
        return "ParsedCommand{" +
                "source=" + source +
                ", type=" + type +
                ", alias='" + alias + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}