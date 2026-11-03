package com.almarel.orion.region;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.command.exceptions.CommandException;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class RegionParser {

    private final FaweParserService faweParserService;

    @Inject
    public RegionParser(FaweParserService faweParserService) {
        this.faweParserService = faweParserService;
    }

    // --- DTO ---

    public record FrameSettings(Pattern pattern) {}
    public record PolygonSettings(Pattern pattern, int vertices, boolean hollow) {}
    public record CenterVerticalSettings(Pattern pattern) {}

    // --- PARSERS ---

    public FrameSettings parseFrame(Player player, String[] args) {
        String matArg = (args.length > 0) ? args[0] : "diamond_block";
        Pattern pattern = faweParserService.parsePattern(player, matArg);
        return new FrameSettings(pattern);
    }

    public PolygonSettings parsePolygon(Player player, String[] args) {
        boolean hollow = hasFlag(args, "-h", "-hand");
        List<String> cleanArgs = removeFlags(args, "-h", "-hand");

        String matArg = (!cleanArgs.isEmpty()) ? cleanArgs.get(0) : "diamond_block";
        Pattern pattern = faweParserService.parsePattern(player, matArg);

        int vertices = parseVertices(cleanArgs);

        return new PolygonSettings(pattern, vertices, hollow);
    }

    public CenterVerticalSettings parseCenterVertical(Player player, String[] args) {
        String patternArg = (args.length > 0) ? args[0] : "diamond_block";
        Pattern pattern = faweParserService.parsePattern(player, patternArg);
        return new CenterVerticalSettings(pattern);
    }

    // --- PRIVATE UTILS ---

    private boolean hasFlag(String[] args, String... flags) {
        List<String> flagList = Arrays.asList(flags);
        for (String arg : args) {
            if (flagList.contains(arg.toLowerCase())) return true;
        }
        return false;
    }

    private List<String> removeFlags(String[] args, String... flags) {
        List<String> flagList = Arrays.asList(flags);
        List<String> cleanArgs = new ArrayList<>();
        for (String arg : args) {
            if (!flagList.contains(arg.toLowerCase())) {
                cleanArgs.add(arg);
            }
        }
        return cleanArgs;
    }

    private int parseVertices(List<String> cleanArgs) {
        if (cleanArgs.size() <= 1) return 8;

        try {
            int n = Integer.parseInt(cleanArgs.get(1));
            return Math.max(3, Math.min(n, 128));
        } catch (NumberFormatException e) {
            throw new CommandException(OrionMessage.CMD_INVALID_NUMBER, cleanArgs.get(1));
        }
    }
}