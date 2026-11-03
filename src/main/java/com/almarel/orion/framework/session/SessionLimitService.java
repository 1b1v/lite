package com.almarel.orion.framework.session;

import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import org.bukkit.entity.Player;


/**
 * Сервис, отвечающий за расчет лимитов сессии на основе прав игрока.
 * Вынесен из SessionManager для соблюдения SRP.
 */
public class SessionLimitService {

    public void updateSessionLimits(Player player, OrionPlayerSession session) {


    }





    /**
     * Максимальное числовое значение permission-ноды, которое имеет смысл сканировать
     * для ЛЮБОГО *_LIMIT_PREFIX права в проекте. Текущий максимум по всем тирам
     * plugin.yml — 99 (CONSTRUCT_GROUPS/POINTS на Advanced); запас до 999 оставлен
     * на случай кастомных донат-рангов сверх стандартной лестницы тиров.
     */
    private static final int MAX_LIMIT_SCAN = 512;

    /**
     * Резолвит числовой лимит по префиксу права, беря МАКСИМАЛЬНОЕ выданное значение
     * через bounded descending scan (аналог уже проверенного resolveMaxBufferSlots()).
     *
     * [LUCKPERMS QUIRK — обнаружено при диагностике CLIPBOARD_SLOTS_LIMIT_PREFIX]
     * player.getEffectivePermissions() НЕ включает permission-ноды, полученные
     * ИСКЛЮЧИТЕЛЬНО через многоуровневое рекурсивное разворачивание children: из
     * чужого plugin.yml (тиры 1b1v.basic -> .intermediate -> .advanced -> .admin).
     * LuckPerms оптимизирует getEffectivePermissions() под явно назначенные им ноды,
     * а не под полный enumerate всего дерева наследования третьих плагинов.
     * player.hasPermission(String), напротив, РАБОТАЕТ корректно и учитывает все
     * children — подтверждено логами (has(...)=true при explicit-выданном тире,
     * одновременно с Total effective permissions=2). Поэтому единственный надёжный
     * способ найти "максимальное выданное число" — явный bounded-скан через
     * hasPermission(), НЕ через enumerate getEffectivePermissions().
     */
    private int resolveConfiguredLimit(Player player, String prefix, int fallback) {
        for (int i = MAX_LIMIT_SCAN; i >= 1; i--) {
            if (player.hasPermission(prefix + i)) {
                return i;
            }
        }
        return fallback;
    }
}