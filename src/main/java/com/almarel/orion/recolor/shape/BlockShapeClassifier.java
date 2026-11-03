package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sk89q.worldedit.world.block.BlockCategories.CARPETS;

@Singleton
public class BlockShapeClassifier {

    private final JavaPlugin plugin;
    private final BlockGeometryFallbackClassifier geometryFallbackClassifier;

    /** byTypeId[BlockType.internalId] -> category */
    private volatile BlockShapeCategory[] byTypeId = new BlockShapeCategory[0];
    private volatile Map<BlockShapeCategory, Set<BlockType>> byCategory = Map.of();

    @Inject
    public BlockShapeClassifier(JavaPlugin plugin,
                                BlockGeometryFallbackClassifier geometryFallbackClassifier) {
        this.plugin = plugin;
        this.geometryFallbackClassifier = geometryFallbackClassifier;
    }

    public void load() {
        Map<BlockShapeCategory, Set<BlockType>> grouped = new EnumMap<>(BlockShapeCategory.class);

        // Vanilla tags (через готовые константы WE)
        register(grouped, BlockCategories.STAIRS, BlockShapeCategory.STAIRS);
        register(grouped, BlockCategories.SLABS, BlockShapeCategory.SLAB);
        register(grouped, BlockCategories.WALLS, BlockShapeCategory.WALL);
        register(grouped, BlockCategories.FENCES, BlockShapeCategory.FENCE);
        register(grouped, BlockCategories.FENCE_GATES, BlockShapeCategory.FENCE_GATE);
        register(grouped, BlockCategories.DOORS, BlockShapeCategory.DOOR);
        register(grouped, BlockCategories.TRAPDOORS, BlockShapeCategory.TRAPDOOR);
        register(grouped, BlockCategories.PRESSURE_PLATES, BlockShapeCategory.PRESSURE_PLATE);
        register(grouped, BlockCategories.BUTTONS, BlockShapeCategory.BUTTON);
        register(grouped, BlockCategories.LOGS, BlockShapeCategory.LOG);
        register(grouped, CARPETS, BlockShapeCategory.CARPET);
        register(grouped, BlockCategories.CHAINS, BlockShapeCategory.CHAIN);
        register(grouped, BlockCategories.LANTERNS, BlockShapeCategory.LANTERN);
        register(grouped, BlockCategories.LEAVES, BlockShapeCategory.LEAVES);

        register(grouped, BlockCategories.RAILS, BlockShapeCategory.RAIL);
        register(grouped, BlockCategories.BARS, BlockShapeCategory.BARS);
        register(grouped, BlockCategories.SAPLINGS, BlockShapeCategory.SAPLING);
        register(grouped, BlockCategories.WOODEN_SHELVES, BlockShapeCategory.SHELF);

        register(grouped, BlockCategories.CROPS, BlockShapeCategory.CROP);
        register(grouped, BlockCategories.FLOWER_POTS, BlockShapeCategory.POTTED);

        // Custom tag (TagRegistry)
        registerGlassPanes(grouped);

        registerExplicit(grouped, BlockShapeCategory.THIN_FOLIAGE,
                "minecraft:glow_lichen",
                "minecraft:big_dripleaf",
                "minecraft:big_dripleaf_stem",
                "minecraft:small_dripleaf",
                "minecraft:chorus_flower"
        );

        // Автоматическая геометрическая классификация ВСЕХ блоков, не попавших
        // ни в одну явную группу выше — делегируется BlockGeometryFallbackClassifier,
        // который перечисляет полный реестр BlockTypes и определяет FULL_BLOCK /
        // NON_CUBE_OTHER через BlockMaterial.isFullCube(). Данные приходят из
        // version-адаптера FAWE, не из нашего кода: корректны для любых новых
        // блоков будущих версий Minecraft автоматически, без необходимости
        // пополнять хардкод-списки при обновлении.
        //
        // Заменяет прежний fail-open дефолт ("всё непокрытое = FULL_BLOCK"), который
        // ошибочно приравнивал декоративные не-кубические блоки (одиночные цветы,
        // мелкие грибы-растения и т.п., не покрытые ни одним vanilla-тегом формы)
        // к цельным кубам — конкретное следствие: recolor мог заменить стену дома
        // на цветок или наоборот при авто-подборе палитры/ручном выборе цели.
        mergeRemainingByGeometry(grouped);

        // строим flat-map по internalId
        BlockShapeCategory[] newById = buildFlatIndex(grouped);

        // volatile swap (lock-free)
        this.byTypeId = newById;
        this.byCategory = freezeGrouped(grouped);
    }

    public BlockShapeCategory classify(BlockStateHolder<?> block) {
        return classify(block.getBlockType());
    }

    public BlockShapeCategory classify(BlockType type) {
        int id = type.getInternalId();
        BlockShapeCategory[] map = byTypeId;
        if (id >= 0 && id < map.length) {
            BlockShapeCategory cat = map[id];
            if (cat != null) {
                return cat;
            }
        }
        // Safety net: не должно происходить в норме (load() теперь классифицирует
        // ПОЛНЫЙ реестр BlockTypes за один проход), но если всё же встретился блок
        // вне проиндексированного диапазона (например, зарегистрирован уже после
        // load()) — считаем геометрию на лету вместо слепого фоллбэка на FULL_BLOCK.
        plugin.getLogger().warning("BlockShapeClassifier: block " + type.id()
                + " was not indexed during load() - falling back to on-the-fly geometry check.");
        return geometryFallbackClassifier.isFullCube(type)
                ? BlockShapeCategory.FULL_BLOCK
                : BlockShapeCategory.NON_CUBE_OTHER;
    }

    public Set<BlockType> getBlocksInCategory(BlockShapeCategory category) {
        return byCategory.getOrDefault(category, Set.of());
    }

    private void register(Map<BlockShapeCategory, Set<BlockType>> grouped,
                          BlockCategory blockCategory,
                          BlockShapeCategory category) {
        try {
            putAll(grouped, blockCategory.getAll(), category);
        } catch (Exception e) {
            plugin.getLogger().severe("BlockShapeClassifier: failed to load tag " + blockCategory.getId()
                    + " for category " + category + ": " + e.getMessage());
        }
    }

    private static final String[] DYE_COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    /**
     * Панели стекла: у ваниллы нет тега формы, поэтому перечисляем по id.
     * Набор идентичен тегу ##all_glass_panes (TagDefinitions), но recolor
     * не должен зависеть от Pro-фичи тегов.
     */
    private void registerGlassPanes(Map<BlockShapeCategory, Set<BlockType>> grouped) {
        String[] ids = new String[DYE_COLORS.length + 1];
        ids[0] = "minecraft:glass_pane";
        for (int i = 0; i < DYE_COLORS.length; i++) {
            ids[i + 1] = "minecraft:" + DYE_COLORS[i] + "_stained_glass_pane";
        }
        registerExplicit(grouped, BlockShapeCategory.GLASS_PANE, ids);
    }

    /**
     * Регистрация формы для блоков, у которых нет ни одного подходящего
     * vanilla-тега формы, и группировка которых не имеет смысла как
     * билдерский "##"-тег (чисто внутренняя деталь геометрии для
     * recolor-движка) — поэтому НЕ заводится через TagRegistry/TagDefinitions,
     * а хранится здесь явным списком.
     */
    private void registerExplicit(Map<BlockShapeCategory, Set<BlockType>> grouped,
                                  BlockShapeCategory category,
                                  String... blockIds) {
        Set<BlockType> bucket = grouped.computeIfAbsent(category, k -> new HashSet<>());
        for (String id : blockIds) {
            BlockType type = BlockTypes.get(id);
            if (type == null) {
                plugin.getLogger().severe("BlockShapeClassifier: unknown block id '" + id
                        + "' for category " + category);
                continue;
            }
            bucket.add(type);
        }
    }

    /**
     * Делегирует {@link BlockGeometryFallbackClassifier} классификацию всех
     * блоков, ещё не попавших ни в одну явную категорию выше, и вливает
     * результат (FULL_BLOCK / NON_CUBE_OTHER) в общую карту группировки.
     */
    private void mergeRemainingByGeometry(Map<BlockShapeCategory, Set<BlockType>> grouped) {
        Set<BlockType> alreadyClassified = collectAllClassified(grouped);
        Map<BlockShapeCategory, Set<BlockType>> remaining =
                geometryFallbackClassifier.classifyRemaining(alreadyClassified);

        for (var entry : remaining.entrySet()) {
            grouped.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                    .addAll(entry.getValue());
        }
    }

    private static Set<BlockType> collectAllClassified(Map<BlockShapeCategory, Set<BlockType>> grouped) {
        Set<BlockType> all = new HashSet<>();
        for (Set<BlockType> bucket : grouped.values()) {
            all.addAll(bucket);
        }
        return all;
    }

    private void putAll(Map<BlockShapeCategory, Set<BlockType>> grouped,
                        Set<BlockType> blocks,
                        BlockShapeCategory category) {
        Set<BlockType> bucket = grouped.computeIfAbsent(category, k -> new HashSet<>());
        bucket.addAll(blocks);
    }

    private static BlockShapeCategory[] buildFlatIndex(Map<BlockShapeCategory, Set<BlockType>> grouped) {
        int maxId = -1;
        for (Set<BlockType> set : grouped.values()) {
            for (BlockType t : set) {
                maxId = Math.max(maxId, t.getInternalId());
            }
        }
        if (maxId < 0) return new BlockShapeCategory[0];

        // Массив по умолчанию заполнен null (не FULL_BLOCK!) — после
        // mergeRemainingByGeometry() каждый зарегистрированный на момент
        // load() BlockType получает явную категорию, null остаётся только
        // как сигнал аномалии для safety-net в classify().
        BlockShapeCategory[] index = new BlockShapeCategory[maxId + 1];

        for (var e : grouped.entrySet()) {
            BlockShapeCategory category = e.getKey();
            for (BlockType t : e.getValue()) {
                int id = t.getInternalId();
                if (id >= 0 && id < index.length) {
                    index[id] = category;
                }
            }
        }

        return index;
    }

    private Map<BlockShapeCategory, Set<BlockType>> freezeGrouped(Map<BlockShapeCategory, Set<BlockType>> grouped) {
        Map<BlockShapeCategory, Set<BlockType>> frozen = new EnumMap<>(BlockShapeCategory.class);
        for (var entry : grouped.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }
}