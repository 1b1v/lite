package com.almarel.orion.brush.fawe.builtins;

import com.sk89q.worldedit.command.tool.brush.Brush;
import java.lang.reflect.Constructor;

public class ReflectionUtils {

    /**
     * Создает экземпляр кисти по полному имени класса.
     * @param className Например "com.arceon.core.brush.BoulderBrush"
     * @param paramTypes Типы аргументов конструктора
     * @param args Сами аргументы
     * @return Экземпляр Brush или null, если не вышло
     */
    public static Brush createBrushInstance(String className, Class<?>[] paramTypes, Object[] args) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(paramTypes);
            return (Brush) constructor.newInstance(args);
        } catch (ClassNotFoundException e) {
            // Плагина нет на сервере - это нормально
            return null;
        } catch (Exception e) {
            // TODO: Add logger // e.printStackTrace();
            return null;
        }
    }
}