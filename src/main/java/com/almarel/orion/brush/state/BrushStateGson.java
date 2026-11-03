package com.almarel.orion.brush.state;

import com.google.gson.*;
import com.almarel.orion.brush.data.BrushState;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

/**
 * GSON Factory for the Orion plugin.
 * Implements the Reference Merging pattern for backward compatibility of brush states.
 */
public class BrushStateGson {

    // Extracted constants to eliminate duplication (DRY)
    private static final JsonSerializer<Material> MATERIAL_SERIALIZER =
            (src, typeOfSrc, context) -> new JsonPrimitive(src.name());

    private static final JsonDeserializer<Material> MATERIAL_DESERIALIZER = (json, typeOfT, context) -> {
        try {
            return Material.valueOf(json.getAsString());
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    };

    private static final Gson PLAIN_GSON = new GsonBuilder()
            .registerTypeAdapter(Material.class, MATERIAL_SERIALIZER)
            .registerTypeAdapter(Material.class, MATERIAL_DESERIALIZER)
            .create();

    public static Gson create(BrushStateRegistry brushRegistry, JavaPlugin plugin) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Material.class, MATERIAL_SERIALIZER)
                .registerTypeAdapter(Material.class, MATERIAL_DESERIALIZER)
                .registerTypeHierarchyAdapter(BrushState.class, new BrushStateAdapter(brushRegistry, plugin.getLogger()))
                .create();
    }

    private static class BrushStateAdapter implements JsonSerializer<BrushState>, JsonDeserializer<BrushState> {

        private final BrushStateRegistry registry;
        private final Logger logger;

        public BrushStateAdapter(BrushStateRegistry registry, Logger logger) {
            this.registry = registry;
            this.logger = logger;
        }

        @Override
        public JsonElement serialize(BrushState src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement elem = PLAIN_GSON.toJsonTree(src);

            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                obj.addProperty("class_type", src.getClass().getSimpleName());
                return obj;
            }

            return elem;
        }

        @Override
        public BrushState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject dbJson = json.getAsJsonObject();

            String typeName = extractClassType(dbJson);
            Class<? extends BrushState> clazz = resolveClass(typeName);
            BrushState defaultInstance = createDefaultInstance(clazz, typeName);

            return mergeAndParseSafely(defaultInstance, dbJson, clazz, typeName);
        }

        // --- Extracted Deserialization Steps ---

        private String extractClassType(JsonObject dbJson) {
            if (!dbJson.has("class_type")) {
                throw new JsonParseException("JSON object is missing 'class_type'. Cannot deserialize brush state.");
            }
            return dbJson.get("class_type").getAsString();
        }

        private Class<? extends BrushState> resolveClass(String typeName) {
            Class<? extends BrushState> clazz = registry.getClass(typeName);
            if (clazz == null) {
                throw new JsonParseException("Unknown brush state type: '" + typeName + "'. Module might be unloaded or class renamed.");
            }
            return clazz;
        }

        private BrushState createDefaultInstance(Class<? extends BrushState> clazz, String typeName) {
            try {
                Constructor<? extends BrushState> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new JsonParseException("Class " + typeName + " lacks a no-args constructor required for safe deserialization.", e);
            } catch (Exception e) {
                throw new JsonParseException("Failed to instantiate state for brush: " + typeName, e);
            }
        }

        private BrushState mergeAndParseSafely(BrushState defaultInstance, JsonObject dbJson, Class<? extends BrushState> clazz, String typeName) {
            try {
                // Serialize the default instance to get a clean JSON tree with all default fields
                JsonObject defaultJson = PLAIN_GSON.toJsonTree(defaultInstance).getAsJsonObject();

                // Merge database data OVER the default data
                JsonObject mergedJson = merge(defaultJson, dbJson);

                return PLAIN_GSON.fromJson(mergedJson, clazz);

            } catch (Exception parseException) {
                logger.warning("Corrupted data or type mismatch for brush '" + typeName + "'.");
                logger.warning("Resetting this brush to default settings to prevent loss. Error: " + parseException.getMessage());

                return defaultInstance; // Fallback
            }
        }

        // --- Deep Merge Logic ---

        private JsonObject merge(JsonObject base, JsonObject overlay) {
            JsonObject result = base.deepCopy();

            for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
                String key = entry.getKey();
                JsonElement overlayElement = entry.getValue();

                if (overlayElement == null || overlayElement.isJsonNull()) {
                    continue;
                }

                if (result.has(key) && result.get(key).isJsonObject() && overlayElement.isJsonObject()) {
                    result.add(key, merge(result.getAsJsonObject(key), overlayElement.getAsJsonObject()));
                } else {
                    result.add(key, overlayElement);
                }
            }

            return result;
        }
    }
}