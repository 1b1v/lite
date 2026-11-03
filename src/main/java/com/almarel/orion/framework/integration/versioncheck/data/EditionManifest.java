package com.almarel.orion.framework.integration.versioncheck.data;

import lombok.Getter;

/**
 * DTO для секции "lite"/"full" манифеста. Весь пользовательский текст
 * (заголовки, лейблы кнопок) теперь живёт здесь, а не в Java-коде —
 * это позволяет менять копирайт без пересборки плагина.
 */
@Getter
public class EditionManifest {
    private String latestVersion;
    private String minSupportedVersion;

    /**
     * Пустая строка/null = критическая ветка неактивна, даже если версия
     * игрока формально ниже minSupportedVersion. Заполняется вручную только
     * при реальном инциденте (см. договорённость про "двойную защёлку").
     */
    private String criticalReason;

    private String updateUrl;
    private String updateHeadline;
    private String updateButtonLabel;

    private String criticalHeadline;
    private String criticalButtonLabel;
}