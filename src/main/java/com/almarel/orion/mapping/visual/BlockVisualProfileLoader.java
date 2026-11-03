package com.almarel.orion.mapping.visual;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Отвечает за загрузку, парсинг и слияние двух источников данных:
 * <ul>
 *     <li><b>Machine dataset</b> (JAR ресурс) — генерируется автоматически offline.</li>
 *     <li><b>Curator overrides</b> (файл на диске) — ручные правки куратора.</li>
 * </ul>
 */
@Singleton
public class BlockVisualProfileLoader {

    private static final String DATASET_RESOURCE_PATH = "datasets/block_visual_profiles.json";
    private static final String OVERRIDES_FILE_NAME = "visual_profile_overrides.json";
    private static final int EXPECTED_DATASET_FORMAT_VERSION = 1;

    private final JavaPlugin plugin;
    private final Gson gson;

    @Inject
    public BlockVisualProfileLoader(JavaPlugin plugin, Gson gson) {
        this.plugin = plugin;
        this.gson = gson;
    }

    public Map<BlockType, BlockVisualProfile> loadProfiles() {
        Map<BlockType, BlockVisualProfile> baseProfiles = loadMachineDataset();
        Map<String, OverrideEntryDto> overrides = loadCuratorOverrides();
        return applyOverrides(baseProfiles, overrides);
    }

    // ---------------------------------------------------------------------
    // Machine dataset (JAR)
    // ---------------------------------------------------------------------

    private Map<BlockType, BlockVisualProfile> loadMachineDataset() {
        try (InputStream in = plugin.getResource(DATASET_RESOURCE_PATH)) {
            if (in == null) {
                plugin.getLogger().severe("Bundled dataset '" + DATASET_RESOURCE_PATH
                        + "' not found in jar; block visual profiles will be unavailable.");
                return Map.of();
            }
            return readAndParseDataset(in);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load bundled block visual profiles.", e);
            return Map.of();
        }
    }

    private Map<BlockType, BlockVisualProfile> readAndParseDataset(InputStream in) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            DatasetDto dataset = gson.fromJson(reader, DatasetDto.class);
            if (dataset == null) {
                plugin.getLogger().warning("Bundled dataset is empty or malformed.");
                return Map.of();
            }
            validateFormatVersion(dataset.formatVersion);
            return parseDataset(dataset);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read bundled dataset stream.", e);
            return Map.of();
        }
    }

    private void validateFormatVersion(int version) {
        if (version != EXPECTED_DATASET_FORMAT_VERSION) {
            plugin.getLogger().warning("Dataset formatVersion=" + version
                    + " differs from expected " + EXPECTED_DATASET_FORMAT_VERSION
                    + "; attempting to parse anyway.");
        }
    }

    private Map<BlockType, BlockVisualProfile> parseDataset(DatasetDto dataset) {
        if (dataset.blocks == null) {
            return Map.of();
        }

        AtomicInteger unknownBlockIdCount = new AtomicInteger();
        Map<BlockType, BlockVisualProfile> parsed = dataset.blocks.stream()
                .map(entry -> parseEntry(entry, unknownBlockIdCount))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableMap(BlockVisualProfile::blockType, p -> p, (first, second) -> second));

        logUnknownBlockIdSummary(unknownBlockIdCount.get());
        return parsed;
    }

    private Optional<BlockVisualProfile> parseEntry(EntryDto entry, AtomicInteger unknownBlockIdCount) {
        if (entry.id == null) {
            plugin.getLogger().warning("Skipping visual profile entry with missing 'id'.");
            return Optional.empty();
        }

        BlockType blockType = resolveBlockType(entry.id, unknownBlockIdCount);
        if (blockType == null) {
            return Optional.empty();
        }

        Optional<RgbColor> rgbColor = parseRgbColor(entry.rgb, entry.id);
        if (rgbColor.isEmpty()) {
            return Optional.empty();
        }

        Optional<ExclusionResolution> exclusion = parseEntryExclusion(entry);
        if (exclusion.isEmpty()) {
            return Optional.empty();
        }

        return createProfile(blockType, rgbColor.get(), entry.noiseScore, exclusion.get(), entry.id);
    }

    private BlockType resolveBlockType(String blockId, AtomicInteger unknownBlockIdCount) {
        BlockType blockType = BlockTypes.get(blockId);
        if (blockType == null) {
            unknownBlockIdCount.incrementAndGet();
            plugin.getLogger().fine(() -> "Skipping visual profile entry: unknown block id '" + blockId + "'.");
        }
        return blockType;
    }

    private void logUnknownBlockIdSummary(int unknownBlockIdCount) {
        if (unknownBlockIdCount > 0) {
            plugin.getLogger().fine(() -> "Skipped " + unknownBlockIdCount
                    + " block visual profile dataset entries: block id unknown to this server "
                    + "(dataset was generated from a newer Minecraft/FAWE version than this server runs). "
                    + "This is expected and does not affect functionality.");
        }
    }

    private Optional<RgbColor> parseRgbColor(int[] rgbArray, String blockId) {
        if (rgbArray == null || rgbArray.length != 3) {
            plugin.getLogger().warning("Skipping visual profile entry '" + blockId
                    + "': 'rgb' must be an array of exactly 3 values.");
            return Optional.empty();
        }

        try {
            return Optional.of(new RgbColor(rgbArray[0], rgbArray[1], rgbArray[2]));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Skipping visual profile entry '" + blockId + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ExclusionResolution> parseEntryExclusion(EntryDto entry) {
        if (entry.excluded && entry.exclusionReason == null) {
            plugin.getLogger().warning("Skipping visual profile entry '" + entry.id
                    + "': excluded=true but no exclusionReason given.");
            return Optional.empty();
        }

        VisualExclusionReason reason = parseReasonOrNull(entry.exclusionReason, entry.id);
        if (entry.exclusionReason != null && reason == null) {
            return Optional.empty();
        }

        if (!entry.excluded && reason != null) {
            plugin.getLogger().warning("Skipping visual profile entry '" + entry.id
                    + "': exclusionReason set but excluded=false.");
            return Optional.empty();
        }

        return Optional.of(new ExclusionResolution(entry.excluded, reason));
    }

    private Optional<BlockVisualProfile> createProfile(BlockType blockType,
                                                       RgbColor rgb,
                                                       double noiseScore,
                                                       ExclusionResolution exclusion,
                                                       String blockId) {
        try {
            return Optional.of(BlockVisualProfile.of(blockType, rgb, noiseScore, exclusion.excluded, exclusion.reason));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Skipping visual profile entry '" + blockId + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------------
    // Curator overrides (Disk)
    // ---------------------------------------------------------------------

    private Map<String, OverrideEntryDto> loadCuratorOverrides() {
        File file = new File(plugin.getDataFolder(), OVERRIDES_FILE_NAME);
        if (!file.exists()) {
            createEmptyOverridesFile(file);
            return Map.of();
        }

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            OverridesDto dto = gson.fromJson(reader, OverridesDto.class);
            if (dto == null || dto.overrides == null) {
                return Map.of();
            }
            return dto.overrides.stream()
                    .filter(e -> e.id != null)
                    .collect(Collectors.toMap(e -> e.id, e -> e, (first, second) -> second));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to load curator overrides from " + file + "; using machine dataset only.", e);
            return Map.of();
        }
    }

    private void createEmptyOverridesFile(File file) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                gson.toJson(new OverridesDto(), writer);
            }
            plugin.getLogger().info("Created empty overrides file at " + file
                    + " — hand-edit it to override machine-generated visual profiles; "
                    + "it is never rewritten by the plugin.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not create overrides file at " + file, e);
        }
    }

    // ---------------------------------------------------------------------
    // Merge logic
    // ---------------------------------------------------------------------

    private Map<BlockType, BlockVisualProfile> applyOverrides(
            Map<BlockType, BlockVisualProfile> base, Map<String, OverrideEntryDto> overrides) {
        if (overrides.isEmpty()) {
            return base;
        }

        Map<BlockType, BlockVisualProfile> merged = new HashMap<>(base);
        for (OverrideEntryDto override : overrides.values()) {
            BlockType blockType = BlockTypes.get(override.id);
            if (blockType == null) {
                plugin.getLogger().warning("Unknown block id in curator overrides: '" + override.id + "'.");
                continue;
            }
            mergeOne(override, blockType, merged.get(blockType)).ifPresent(p -> merged.put(blockType, p));
        }
        return Map.copyOf(merged);
    }

    private Optional<BlockVisualProfile> mergeOne(OverrideEntryDto o, BlockType blockType, BlockVisualProfile base) {
        Optional<RgbColor> rgb = resolveOverrideRgb(o, base);
        if (rgb.isEmpty()) {
            return Optional.empty();
        }

        double noiseScore = resolveNoiseScore(o.noiseScore, base);
        ExclusionResolution exclusion = resolveExclusion(o, base);

        return createProfile(blockType, rgb.get(), noiseScore, exclusion, o.id);
    }

    private Optional<RgbColor> resolveOverrideRgb(OverrideEntryDto override, BlockVisualProfile base) {
        if (override.rgb != null) {
            return parseRgbColor(override.rgb, override.id);
        }
        if (base != null) {
            return Optional.of(base.rgbColor());
        }

        plugin.getLogger().warning("Override for '" + override.id
                + "' has no base machine profile and no 'rgb' of its own; skipping.");
        return Optional.empty();
    }

    private double resolveNoiseScore(Double overrideNoise, BlockVisualProfile base) {
        if (overrideNoise != null) {
            return overrideNoise;
        }
        return (base != null) ? base.noiseScore() : 0.0;
    }

    private ExclusionResolution resolveExclusion(OverrideEntryDto o, BlockVisualProfile base) {
        VisualExclusionReason explicitReason = o.exclusionReason != null
                ? parseReasonOrNull(o.exclusionReason, o.id)
                : null;

        if (o.excluded != null) {
            boolean excluded = o.excluded;
            VisualExclusionReason reason = excluded
                    ? (explicitReason != null ? explicitReason : VisualExclusionReason.CURATOR_OVERRIDE)
                    : null;
            return new ExclusionResolution(excluded, reason);
        }

        if (explicitReason != null) {
            return new ExclusionResolution(true, explicitReason);
        }

        boolean baseExcluded = base != null && base.excluded();
        VisualExclusionReason baseReason = base != null ? base.exclusionReason() : null;
        return new ExclusionResolution(baseExcluded, baseReason);
    }

    private VisualExclusionReason parseReasonOrNull(String rawReason, String blockId) {
        if (rawReason == null) {
            return null;
        }
        try {
            return VisualExclusionReason.valueOf(rawReason);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown exclusionReason '" + rawReason
                    + "' for block '" + blockId + "'; ignoring.");
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------------

    private record ExclusionResolution(boolean excluded, VisualExclusionReason reason) {}

    private static final class DatasetDto {
        int formatVersion;
        String minecraftVersion;
        List<EntryDto> blocks;
    }

    private static final class EntryDto {
        String id;
        int[] rgb;
        double noiseScore;
        boolean excluded;
        String exclusionReason;
    }

    private static final class OverridesDto {
        int formatVersion = 1;
        List<OverrideEntryDto> overrides = List.of();
    }

    private static final class OverrideEntryDto {
        String id;
        int[] rgb;
        Double noiseScore;
        Boolean excluded;
        String exclusionReason;
    }
}