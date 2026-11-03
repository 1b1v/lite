package com.almarel.orion.recolor.shape;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Единственная ответственность: автоматический геометрический дефолт для
 * любого BlockType, не покрытого ни одним явным vanilla/custom/хардкод-
 * источником формы в {@link BlockShapeClassifier}.
 * <p>
 * Перечисляет ПОЛНЫЙ реестр WorldEdit через BlockTypes.size()/get(int)
 * (единственный публичный способ перечисления без values(), т.к. BlockTypes —
 * не enum, а класс со статическими полями) и для каждого ещё не
 * классифицированного блока определяет {@link BlockShapeCategory#FULL_BLOCK}
 * или {@link BlockShapeCategory#NON_CUBE_OTHER} через
 * {@link com.sk89q.worldedit.world.registry.BlockMaterial#isFullCube()}.
 * <p>
 * Данные о геометрии приходят из version-адаптера FAWE, не из нашего кода —
 * корректны для любых новых блоков будущих версий Minecraft автоматически,
 * без необходимости пополнять хардкод-списки при обновлении.
 */
@Singleton
public class BlockGeometryFallbackClassifier {

    private final JavaPlugin plugin;

    @Inject
    public BlockGeometryFallbackClassifier(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param alreadyClassified блоки, уже отнесённые к явной категории выше
     *                          по цепочке источников (не переклассифицируются)
     * @return карта ровно с двумя ключами: FULL_BLOCK и NON_CUBE_OTHER
     *         (значения — возможно пустые множества, но ключи присутствуют всегда)
     */
    public Map<BlockShapeCategory, Set<BlockType>> classifyRemaining(Set<BlockType> alreadyClassified) {
        Set<BlockType> fullBlocks = new HashSet<>();
        Set<BlockType> nonCubeOthers = new HashSet<>();

        int total = BlockTypes.size();
        for (int i = 0; i < total; i++) {
            BlockType type = BlockTypes.get(i);
            if (type == null || alreadyClassified.contains(type)) {
                continue;
            }
            if (isFullCube(type)) {
                fullBlocks.add(type);
            } else {
                nonCubeOthers.add(type);
            }
        }

        Map<BlockShapeCategory, Set<BlockType>> result = new EnumMap<>(BlockShapeCategory.class);
        result.put(BlockShapeCategory.FULL_BLOCK, fullBlocks);
        result.put(BlockShapeCategory.NON_CUBE_OTHER, nonCubeOthers);
        return result;
    }

    /**
     * Публичный метод, переиспользуется также safety-net'ом в
     * {@link BlockShapeClassifier#classify(BlockType)} для блоков, не
     * попавших в индекс на момент load() — единая точка правды о том,
     * "что такое цельный куб", без дублирования логики.
     */
    public boolean isFullCube(BlockType type) {
        try {
            return type.getMaterial().isFullCube();
        } catch (Exception e) {
            // Резилентность: материал блока недоступен по неизвестной причине
            // (не должно происходить в норме) — безопасный дефолт FULL_BLOCK,
            // чтобы не ломать уже существующее поведение recolor для обычных кубов.
            plugin.getLogger().warning("BlockGeometryFallbackClassifier: failed to read BlockMaterial for "
                    + type.id() + ", defaulting to FULL_BLOCK: " + e.getMessage());
            return true;
        }
    }
}