package com.almarel.orion.brush.fawe.builtins;

import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

/**
 * Интерфейс для процессоров нативных кистей FAWE.
 * Каждый процессор отвечает за парсинг аргументов и создание ОДНОГО типа кисти (например, Sphere).
 */
public interface BrushProcessor {
    /**
     * Создает и устанавливает кисть.
     * @param player Игрок, которому ставим кисть.
     * @param tool Инструмент (BrushTool), который нужно настроить.
     * @param args Аргументы (без названия кисти). Пример: ["sand", "5"]
     * @return true, если успешно.
     */
    boolean process(Player player, BrushTool tool, String[] args);

    /**
     * Право, необходимое для бинда этой кисти (FAWE-нода, например worldedit.brush.sphere).
     * Проверяется при бинде; FAWE дополнительно перепроверяет его при каждом использовании
     * через BrushTool.setBrush(brush, permission).
     */
    String getPermission();
}