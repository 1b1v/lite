package com.almarel.orion.brush.fawe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.command.infrastructure.CommandLineFormat;

import java.util.Arrays;
import java.util.Set;

/**
 * Единственная точка входа для лексического анализа сырых команд FAWE/Arceon/EzEdits.
 * Stateless сервис. Переводит сырую строку в структурированный ParsedCommand DTO.
 */
@Singleton
public class ExternalCommandParser {

    // Standalone модификаторы
    private static final Set<String> MODIFICATION_PREFIXES = Set.of(
            "/mat", "//mat", "/material", "//material",
            "/mask", "//mask",
            "/size", "//size", "/range", "//range"
    );

    // Алиасы, которые могут идти после креативного префикса, но являются модификаторами
    // (Например: //br mask stone)
    private static final Set<String> MODIFICATION_ALIASES = Set.of(
            "mat", "material", "mask", "size", "range"
    );

    private static final Set<String> EZEDITS_PREFIXES = Set.of(
            "/ezbr", "//ezbr", "/ezbrush", "//ezbrush"
    );

    private static final Set<String> ARCEON_PREFIXES = Set.of(
            "/arcbrush"
    );

    private static final Set<String> FAWE_CREATION_PREFIXES = Set.of(
            "/br", "//br", "/brush", "//brush",
            "/tool", "//tool"
    );

    private static final Set<String> CLEAR_ALIASES = Set.of(
            "none", "clear", "off"
    );

    @Inject
    public ExternalCommandParser() {
        // Stateless - нет зависимостей
    }

    /**
     * Парсит сырую команду игрока в структурированный DTO.
     * @param rawCommand Сырая строка (например, "//br sphere stone 5" или "/mask >stone")
     * @return ParsedCommand или ParsedCommand.UNKNOWN, если команда не распознана
     */
    public ParsedCommand parse(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) return ParsedCommand.UNKNOWN;

        String normalized = rawCommand.trim().toLowerCase();
        String[] parts = normalized.split("\\s+");
        if (parts.length == 0) return ParsedCommand.UNKNOWN;

        String prefix = parts[0];

        if (MODIFICATION_PREFIXES.contains(prefix)) {
            return parseStandaloneModification(parts, prefix);
        }
        if (EZEDITS_PREFIXES.contains(prefix)) {
            return parseCreationCommand(parts, 1, ParsedCommand.CommandSource.EZEDITS);
        }
        if (ARCEON_PREFIXES.contains(prefix)) {
            return parseCreationCommand(parts, 1, ParsedCommand.CommandSource.ARCEON);
        }
        if (FAWE_CREATION_PREFIXES.contains(prefix)) {
            return parseFaweCreationCommand(parts);
        }

        return ParsedCommand.UNKNOWN;
    }

    private ParsedCommand parseStandaloneModification(String[] parts, String prefix) {
        String alias = CommandLineFormat.stripSlashes(prefix);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        return new ParsedCommand(ParsedCommand.CommandSource.FAWE, ParsedCommand.CommandType.MODIFICATION, alias, args);
    }

    private ParsedCommand parseFaweCreationCommand(String[] parts) {
        int aliasIndex = resolveFaweAliasIndex(parts);
        return parseCreationCommand(parts, aliasIndex, ParsedCommand.CommandSource.FAWE);
    }

    private int resolveFaweAliasIndex(String[] parts) {
        // Обработка legacy-синтаксиса: "//br brush sphere..." -> пропуск слова "brush"
        if (parts.length > 1 && "brush".equals(parts[1])) {
            return 2;
        }
        return 1;
    }

    private ParsedCommand parseCreationCommand(String[] parts, int aliasIndex, ParsedCommand.CommandSource source) {
        if (parts.length <= aliasIndex) {
            return ParsedCommand.UNKNOWN; // Ввели просто "/br" без аргументов
        }

        String alias = parts[aliasIndex];
        String[] args = Arrays.copyOfRange(parts, aliasIndex + 1, parts.length);
        ParsedCommand.CommandType type = resolveCreationCommandType(alias);

        return new ParsedCommand(source, type, alias, args);
    }

    private ParsedCommand.CommandType resolveCreationCommandType(String alias) {
        if (CLEAR_ALIASES.contains(alias)) {
            return ParsedCommand.CommandType.CLEAR;
        }
        if (MODIFICATION_ALIASES.contains(alias)) {
            return ParsedCommand.CommandType.MODIFICATION;
        }
        return ParsedCommand.CommandType.CREATION;
    }

    /**
     * Быстрая проверка, является ли строка известным префиксом внешней команды.
     * Используется для фильтрации до полного парсинга.
     */
    public boolean isExternalCommandPrefix(String prefix) {
        if (prefix == null) return false;
        String p = prefix.toLowerCase();
        return MODIFICATION_PREFIXES.contains(p)
                || EZEDITS_PREFIXES.contains(p)
                || ARCEON_PREFIXES.contains(p)
                || FAWE_CREATION_PREFIXES.contains(p);
    }
}