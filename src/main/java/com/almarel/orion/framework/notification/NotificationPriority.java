package com.almarel.orion.framework.notification;

/**
 * Централизованный реестр приоритетов login-уведомлений.
 * Меньшее значение — выше в дайджесте.
 *
 * Значения намеренно разрежены (большие интервалы между числами),
 * чтобы при добавлении нового источника уведомлений было куда
 * вставить новый уровень, не пересчитывая существующие константы.
 */
public enum NotificationPriority {

    /** Обновление версии плагина, включая критические предупреждения. */
    EDITION_UPDATE(10),

    /** Новые дефолтные пресеты в JAR-файле. */
    PRESET_UPDATE(50),

    /** Рассинхронизация схематик с удалённым манифестом ассетов. */
    ASSET_SYNC(60),

    /** Промо Full-редакции на Lite-серверах — наименее срочное, всегда последнее. */
    LITE_PROMOTION(200);

    private final int value;

    NotificationPriority(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}