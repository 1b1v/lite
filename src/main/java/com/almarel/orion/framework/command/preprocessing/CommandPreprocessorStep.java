package com.almarel.orion.framework.command.preprocessing;

/**
 * Контракт для шагов санитизации и трансляции команд до их парсинга в ядре.
 */
public interface CommandPreprocessorStep {
    /**
     * Обрабатывает строку команды.
     * @param message Исходная строка (например, //replace /bush1)
     * @return Измененная строка (например, //replace #fullcopy[bush1])
     */
    String process(String message);
}