package com.almarel.orion.framework.gui.configurationlegacy;

/**
 * Центральная палитра цветов для всего GUI.
 * Позволяет менять стиль плагина в одном месте.
 */
public final class Palette {

    // Заголовки
    public static final String TITLE = "§9";         // Основные заголовки (Blue)
    public static final String HEADER_AQUA = "§b§l"; // Спец. заголовки (Aqua Bold)
    public static final String HEADER_PURPLE = "§5§l";
    public static final String HEADER_PINK = "§d§l";

    // Кнопки
    public static final String BTN_ACT = "§a";       // Действие (Green)
    public static final String BTN_SET = "§e";       // Настройка (Yellow)
    public static final String BTN_MACRO = "§d";     // Макросы (Pink)
    public static final String BTN_NAV = "§b";       // Навигация (Назад/Вперед)
    public static final String BTN_CLOSE = "§c";     // Закрыть

    // Значения
    public static final String VAL_ON = "§a";        // Вкл
    public static final String VAL_OFF = "§c";       // Выкл
    public static final String VAL_HIGH = "§6";      // Подсветка (Gold)

    // Лор (Описание)
    public static final String LORE_TEXT = "§7";     // Обычный текст
    public static final String LORE_VAL = "§f";      // Значение переменной (White)
    public static final String LORE_META = "§8";     // Техническая инфа (флаги, id)

    // Системное
    public static final String ERROR = "§c";

    // --- [NEW] Стилизация списков и пар (Ключ - Значение) ---
    // Используется в GuiTextFormatter для авто-покраски строк с разделителями

    public static final String STYLE_KEY = "§7";       // Серый (текст ДО разделителя)
    public static final String STYLE_SEPARATOR = "§f"; // Белый (сам разделитель)
    public static final String STYLE_VALUE = "§f";     // Белый (текст ПОСЛЕ разделителя)

    // Список разделителей, которые активируют авто-стилизацию.
    // Внимание: Пробелы важны! " - " это тире между словами.
    public static final String[] SEPARATORS = {
            " - ",  // Обычный дефис с пробелами
            " — ",  // Длинное тире (Long Dash)
            ": "    // Двоеточие с пробелом (для параметров "Маска: 0")
    };

    private Palette() {} // Запрещаем создание экземпляров
}