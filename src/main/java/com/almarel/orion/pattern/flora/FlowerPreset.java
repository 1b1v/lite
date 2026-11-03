package com.almarel.orion.pattern.flora;

import com.sk89q.worldedit.world.block.BlockTypes;
import com.almarel.orion.pattern.util.WeightedPalette.WeightedBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum FlowerPreset {

    BLUE(
            List.of(
                    new WeightedBlock(BlockTypes.BLUE_ORCHID.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.CORNFLOWER.getDefaultState(), 1)
            ), true
    ),
    BLUE2(
            List.of(
                    new WeightedBlock(BlockTypes.CORNFLOWER.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.PITCHER_PLANT.getDefaultState(), 1, true)
            ), true
    ),
    RED(
            List.of(
                    new WeightedBlock(BlockTypes.POPPY.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.RED_TULIP.getDefaultState(), 1)
            ), true
    ),
    PINK(
            List.of(
                    new WeightedBlock(BlockTypes.PINK_TULIP.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.ALLIUM.getDefaultState(), 1)
            ), true
    ),
    ORANGE(
            List.of(
                    new WeightedBlock(BlockTypes.ORANGE_TULIP.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.TORCHFLOWER.getDefaultState(), 1)
            ), true
    ),
    YELLOW(
            List.of(
                    new WeightedBlock(BlockTypes.SUNFLOWER.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.DANDELION.getDefaultState(), 1)
            ), true
    ),
    GREEN(
            List.of(), true
    ),
    WHITE(
            List.of(
                    new WeightedBlock(BlockTypes.OXEYE_DAISY.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.AZURE_BLUET.getDefaultState(), 1)
            ), true
    ),
    WHITE2(
            List.of(
                    new WeightedBlock(BlockTypes.WHITE_TULIP.getDefaultState(), 1),
                    new WeightedBlock(BlockTypes.LILY_OF_THE_VALLEY.getDefaultState(), 1)
            ), true
    ),
    BROWN(
            List.of(
                    new WeightedBlock(BlockTypes.DEAD_BUSH.getDefaultState(), 1)
            ), false // Отключает спавн любой зеленой травы при использовании
    ),
    RANDOM(
            null, true
    );

    // Базовые палитры, общие для всех пресетов (вынесены из парсеров)
    private static final List<WeightedBlock> BASE_GRASS = List.of(
            new WeightedBlock(BlockTypes.SHORT_GRASS.getDefaultState(), 70),
            new WeightedBlock(BlockTypes.FERN.getDefaultState(), 30)
    );
    private static final List<WeightedBlock> BASE_TALL_GRASS = List.of(
            new WeightedBlock(BlockTypes.TALL_GRASS.getDefaultState(), 1),
            new WeightedBlock(BlockTypes.LARGE_FERN.getDefaultState(), 1)
    );

    private final List<WeightedBlock> flowerBlocks;
    private final boolean generateGrass;

    // Кэш для ленивой инициализации
    private PaletteSet cachedPaletteSet;

    FlowerPreset(List<WeightedBlock> flowerBlocks, boolean generateGrass) {
        this.flowerBlocks = flowerBlocks;
        this.generateGrass = generateGrass;
    }

    public List<WeightedBlock> getFlowerBlocks() {
        return flowerBlocks;
    }

    /**
     * Возвращает комплект палитр для пресета (Lazy Initialization).
     */
    public synchronized PaletteSet getPaletteSet() {
        if (cachedPaletteSet == null) {
            if (this == RANDOM) {
                List<WeightedBlock> allFlowers = new ArrayList<>();
                for (FlowerPreset preset : values()) {
                    if (preset != GREEN && preset != RANDOM && preset != BROWN) {
                        allFlowers.addAll(preset.flowerBlocks);
                    }
                }
                cachedPaletteSet = new PaletteSet(BASE_GRASS, allFlowers, BASE_TALL_GRASS);
            } else {
                cachedPaletteSet = new PaletteSet(
                        generateGrass ? BASE_GRASS : List.of(),
                        flowerBlocks == null ? List.of() : flowerBlocks,
                        generateGrass ? BASE_TALL_GRASS : List.of()
                );
            }
        }
        return cachedPaletteSet;
    }

    /**
     * Выбирает один случайный цветной пресет, исключая голую траву, пустоши и мешанину.
     */
    private static FlowerPreset getRandomSingleColorPreset() {
        List<FlowerPreset> validPresets = new ArrayList<>();
        for (FlowerPreset preset : values()) {
            if (preset != GREEN && preset != BROWN && preset != RANDOM) {
                validPresets.add(preset);
            }
        }
        return validPresets.get(ThreadLocalRandom.current().nextInt(validPresets.size()));
    }

    /**
     * Объединяет несколько пресетов.
     * Если хотя бы один из них отключает траву (например, brown), трава отключается полностью.
     */
    public static PaletteSet fromInputCombined(String input) {
        if (input == null || input.isEmpty()) {
            // ФИКС: Выбираем строго один случайный цвет для всей кисти/выделения
            return getRandomSingleColorPreset().getPaletteSet();
        }

        List<WeightedBlock> mergedFlowers = new ArrayList<>();
        boolean enableGrass = true;
        boolean hasRandom = false;

        String[] parts = input.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            FlowerPreset preset = fromInput(trimmed);
            if (!preset.generateGrass) {
                enableGrass = false; // Отключаем траву для всего микса
            }
            if (preset == RANDOM) {
                hasRandom = true;
            } else {
                mergedFlowers.addAll(preset.getFlowerBlocks());
            }
        }

        if (hasRandom) {
            for (FlowerPreset preset : values()) {
                if (preset != GREEN && preset != RANDOM && preset != BROWN) {
                    mergedFlowers.addAll(preset.getFlowerBlocks());
                }
            }
        }

        return new PaletteSet(
                enableGrass ? BASE_GRASS : List.of(),
                mergedFlowers,
                enableGrass ? BASE_TALL_GRASS : List.of()
        );
    }

    /**
     * Маппинг ввода пользователя (цифра или имя) в пресет.
     */
    public static FlowerPreset fromInput(String input) {
        if (input == null || input.isEmpty()) {
            // ФИКС: Выбираем строго один случайный цвет
            return getRandomSingleColorPreset();
        }

        try {
            int index = Integer.parseInt(input) - 1;
            FlowerPreset[] presets = values();
            if (index >= 0 && index < presets.length) {
                return presets[index];
            }
        } catch (NumberFormatException ignored) {}

        try {
            return valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RANDOM;
        }
    }

    /**
     * DTO: Комплект палитр, готовый к передаче в FlowersPattern.
     */
    public record PaletteSet(
            List<WeightedBlock> grass,
            List<WeightedBlock> flowers,
            List<WeightedBlock> tallGrass
    ) {}
}