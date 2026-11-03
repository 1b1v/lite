package com.almarel.orion.mapping.block;

import com.google.inject.Singleton;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class BlockVariantMapper {

    /**
     * Splits a pattern string by commas, ignoring commas inside square brackets [...].
     * Example: "stone,dirt[snowy=true,age=1],glass" -> ["stone", "dirt[snowy=true,age=1]", "glass"]
     *
     * @param input the raw pattern string
     * @return a list of parsed arguments
     */
    public List<String> splitPatternArg(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBrackets = false;

        for (char c : input.toCharArray()) {
            if (c == '[') {
                inBrackets = true;
            } else if (c == ']') {
                inBrackets = false;
            }

            if (c == ',' && !inBrackets) {
                parts.add(current.toString());
                current.setLength(0); // Reuse StringBuilder instead of creating a new one
            } else {
                current.append(c);
            }
        }

        parts.add(current.toString());
        return parts;
    }

    public Pattern resolveRelatedPattern(Pattern pattern, String targetSuffix) {
        if (!(pattern instanceof BlockStateHolder<?> holder)) {
            return pattern;
        }

        String id = holder.getBlockType().id();
        if (id.endsWith(targetSuffix)) {
            return pattern;
        }

        String namespace = extractNamespace(id);
        String name = extractName(id);

        List<String> candidates = generateCandidates(namespace, name, targetSuffix);
        BlockType matchingType = findMatchingBlockType(candidates);

        return (matchingType != null) ? matchingType.getDefaultState() : pattern;
    }

    private BlockType findMatchingBlockType(List<String> candidates) {
        for (String candidate : candidates) {
            BlockType type = BlockTypes.get(candidate);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    private List<String> generateCandidates(String namespace, String name, String targetSuffix) {
        List<String> candidates = new ArrayList<>();
        String strippedBase = stripKnownSuffix(name);

        if (strippedBase != null) {
            candidates.add(namespace + strippedBase + targetSuffix);
        }

        // Fallback: прямое добавление суффикса к исходному имени
        candidates.add(namespace + name + targetSuffix);
        return candidates;
    }

    private String stripKnownSuffix(String name) {
        if (name.endsWith("_stairs")) return name.substring(0, name.length() - 7);
        if (name.endsWith("_slab")) return name.substring(0, name.length() - 5);
        if (name.endsWith("_planks")) return name.substring(0, name.length() - 7);
        if (name.endsWith("_block")) return name.substring(0, name.length() - 6);
        if (name.endsWith("bricks")) return name.substring(0, name.length() - 1);
        if (name.endsWith("s") && !name.endsWith("ss")) return name.substring(0, name.length() - 1);
        return null;
    }

    private String extractNamespace(String id) {
        int separatorIndex = id.indexOf(':');
        return separatorIndex != -1 ? id.substring(0, separatorIndex + 1) : "minecraft:";
    }

    private String extractName(String id) {
        int separatorIndex = id.indexOf(':');
        return separatorIndex != -1 ? id.substring(separatorIndex + 1) : id;
    }
}