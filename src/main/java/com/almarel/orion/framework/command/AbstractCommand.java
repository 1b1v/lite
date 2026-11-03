package com.almarel.orion.framework.command;

import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Базовая реализация команды Orion.
 * Предоставляет удобный доступ к сервисам делегации и утилитам для табов.
 */
public abstract class AbstractCommand implements CommandProcessor {

    protected final FaweSuggestionProvider faweService;

    // Конструктор для DI.
    // Наследники должны будут вызывать super(faweService).
    protected AbstractCommand(FaweSuggestionProvider faweService) {
        this.faweService = faweService;
    }

    // --- Утилиты для Tab Completion ---



    /**
     * Делегирует поиск подсказок в FAWE (через сервис).
     * Обертка для удобства использования внутри команд.
     */
    protected List<String> delegateToFawe(Player player, String[] args, String template) {
        return faweService.getFaweSuggestions(player, args, template);
    }
}