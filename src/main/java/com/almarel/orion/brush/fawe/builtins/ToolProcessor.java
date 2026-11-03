package com.almarel.orion.brush.fawe.builtins;

import com.sk89q.worldedit.entity.Player;

/**
 * Интерфейс для процессоров нативных инструментов (Tools) FAWE.
 * В отличие от кистей, инструменты применяются напрямую к предмету в сессии.
 */
public interface ToolProcessor {
    /**
     * Создает и устанавливает инструмент игроку.
     * @param player WE Игрок
     * @param args Аргументы (без названия инструмента)
     * @return true, если успешно
     */
    boolean process(Player player, String[] args);

    /**
     * Право, необходимое для бинда этого инструмента (FAWE-нода, например worldedit.tool.farwand).
     * Проверяется при бинде; FAWE перепроверяет его при использовании.
     */
    String getPermission();
}