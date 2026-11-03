package com.almarel.orion.framework.gui.data;

/**
 * Хранилище констант для разметки GUI.
 * Позволяет избегать "магических чисел" (49, 50, 20) в коде.
 */
public final class GuiLayout {

    private GuiLayout() {} // Запрещаем создание экземпляров

    // --- Размеры ---
    public static final int ROW_SIZE = 9;
    public static final int ROWS_6 = 54;

    // --- Навигационная панель (Нижняя строка) ---
    public static final int NAV_BAR_START = 45;
    public static final int NAV_BAR_END = 53;

    // Основные слоты навигации
    public static final int SLOT_NAV_PREV = 48;   // Левая кнопка (обычно Пагинация или Буфер)
    public static final int SLOT_NAV_CENTER = 49; // Центральная кнопка (Назад / Переключение контекста)
    public static final int SLOT_NAV_NEXT = 50;   // Правая кнопка (обычно Пагинация)
    public static final int SLOT_MACROS = 52;     // Слот для макросов

    // --- Боковые Табы (Правая колонка) ---
    // Используем слоты 17, 26, 35 (последние слоты 2, 3 и 4 строки)
    public static final int SLOT_TAB_SETTINGS = 17; // Вкладка "Настройки кисти"
    public static final int SLOT_TAB_POINT = 26;    // Вкладка "Точка" (Новая)
    public static final int SLOT_TAB_GROUPS = 35;   // Вкладка "Список Групп"

    // --- Стандартные позиции элементов ---
    public static final int SLOT_INFO_MAIN = 20;  // Основная иконка с информацией о кисти
}