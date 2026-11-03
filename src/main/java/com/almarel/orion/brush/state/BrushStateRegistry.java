package com.almarel.orion.brush.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр типов состояний кистей для GSON.
 * Содержит маппинг между ID кисти (строка) и Классом состояния.
 * Используется для сохранения и загрузки данных игроков.
 */
@Singleton
public class BrushStateRegistry {

    // --- DTO для передачи данных из Guice ---
    public static class Registration {
        public final String prefix;
        public final Class<? extends BrushState> stateClass;

        public Registration(String prefix, Class<? extends BrushState> stateClass) {
            this.prefix = prefix;
            this.stateClass = stateClass;
        }
    }

    private final Map<String, Class<? extends BrushState>> typeMap = new ConcurrentHashMap<>();

    // Guice автоматически соберет Set<Registration> из всех модулей и вставит сюда
    @Inject
    public BrushStateRegistry(Set<Registration> registrations) {
        for (Registration reg : registrations) {
            register(reg.prefix, reg.stateClass);
        }
    }

    /**
     * Регистрирует новый тип состояния кисти.
     * @param id Уникальный ID (например "fire").
     * @param clazz Класс состояния.
     */
    public void register(String id, Class<? extends BrushState> clazz) {
        if (id == null || clazz == null) return;

        typeMap.put(id, clazz);
        typeMap.put(clazz.getSimpleName(), clazz);
    }

    /**
     * Возвращает класс состояния по его имени или ID.
     */
    /**
     * Возвращает класс состояния по его имени или ID.
     */
    /**
     * Возвращает класс состояния по его имени (class_type) или ключу из БД.
     */
    public Class<? extends BrushState> getClass(String typeName) {
        if (typeName == null) return null;

        // --- 1. JSON LOOKUP (По точному совпадению: GSON class_type или ID) ---
        Class<? extends BrushState> directMatch = typeMap.get(typeName);
        if (directMatch != null) return directMatch;

        // --- 2. DATABASE KEY LOOKUP (Динамический парсинг префикса) ---
        // Ключи в БД хранятся в формате id_material (например: plump_stick, fawe_wooden_axe).
        // Мы отсекаем всё начиная с первого '_' и ищем базовый ID в реестре.
        String lowerName = typeName.toLowerCase();
        int underscoreIndex = lowerName.indexOf('_');

        if (underscoreIndex != -1) {
            String prefix = lowerName.substring(0, underscoreIndex);
            return typeMap.get(prefix); // Автоматически вернет нужный класс
        }

        return null;
    }
}