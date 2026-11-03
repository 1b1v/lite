package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.ChatColor;

public class TextFormatter {

    public static String process(String text, String defaultColorPrefix) {
        if (text == null) return "§cMissing";

        // Проверяем все разделители из Палитры
        for (String separator : Palette.SEPARATORS) {
            if (text.contains(separator)) {
                // Если нашли разделитель - применяем стиль "Ключ-Значение"

                // Заменяем разделитель на: Стиль_Разделителя + Разделитель + Стиль_Значения
                String replacement = Palette.STYLE_SEPARATOR + separator + Palette.STYLE_VALUE;

                // [FIX] Используем defaultColorPrefix вместо STYLE_KEY для начала строки.
                // Это позволяет задавать цвет ключа (например, Желтый для Сортировки),
                // сохраняя автоматическую раскраску значения.

                // Если defaultColorPrefix передан как null или пустой, используем STYLE_KEY как фоллбек
                String prefix = (defaultColorPrefix != null && !defaultColorPrefix.isEmpty())
                        ? defaultColorPrefix
                        : Palette.STYLE_KEY;

                text = prefix + text.replace(separator, replacement);

                return colorize(text);
            }
        }

        // Если разделителей нет - используем переданный цвет
        return colorize(defaultColorPrefix + text);
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}