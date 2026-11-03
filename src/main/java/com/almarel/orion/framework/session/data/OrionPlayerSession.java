package com.almarel.orion.framework.session.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Динамический контейнер данных сессии игрока.
 * Не содержит жестких зависимостей от конкретных фич.
 * Каждый модуль плагина сам запрашивает свой компонент данных.
 */
public class OrionPlayerSession {

    // Lock-Free коллекция для потокобезопасного хранения компонентов
    private final Map<Class<?>, Object> components = new ConcurrentHashMap<>();

    /**
     * Получить компонент сессии для конкретной фичи.
     * Если компонента еще нет, он будет лениво создан (Lazy Init) через рефлексию.
     * Требует наличия пустого конструктора (по умолчанию) у класса данных.
     *
     * @param clazz Класс данных (например, SessionBrushData.class)
     * @param <T>   Тип компонента
     * @return Экземпляр компонента
     */
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> clazz) {
        return (T) components.computeIfAbsent(clazz, key -> {
            try {
                // Строго используем new для объектов данных
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Не удалось инициализировать компонент сессии: " + clazz.getSimpleName() +
                        ". Убедитесь, что у класса есть пустой конструктор.", e);
            }
        });
    }

    /**
     * Проверить, загружен ли уже компонент, без его инициализации.
     * Полезно для GC и очистки ресурсов, чтобы не создавать пустые сессии.
     */
    public boolean hasComponent(Class<?> clazz) {
        return components.containsKey(clazz);
    }

    /**
     * Очистить компонент (например, при специфичных событиях сброса фичи).
     */
    public void removeComponent(Class<?> clazz) { // no usage почему-то
        components.remove(clazz);
    }

    /**
     * Компоненты:
     * Session BrushData.java
     * SessionClipboardDatajava
     * SessionConstructData.java
     * Session HistoryDatajava
     * Session MacroData.java
     */
}