package com.almarel.orion.framework.notification;

/**
 * Stateless-утилита для подстановки {placeholder}-значений в текст,
 * приходящий из внешнего JSON-манифеста. Не сервис — чистые статические
 * функции без состояния (тот же паттерн, что SemanticVersionComparator).
 */
public final class NotificationTextFormatter {

    private NotificationTextFormatter() {
    }

    /**
     * Заменяет {key} на value в шаблоне. Пары передаются подряд: key1, value1, key2, value2...
     * Null-шаблон трактуется как пустая строка. Null-значение подставляется как "".
     */
    public static String format(String template, String... keyValuePairs) { // Не используется. написан на будущее.
        String result = template == null ? "" : template;
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[i + 1];
            result = result.replace("{" + key + "}", value == null ? "" : value);
        }
        return result;
    }

    /**
     * @return value, если оно не null и не пустое, иначе fallback.
     * Защита от гонки: манифест на GitHub может временно отставать от кода плагина.
     */
    public static String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}