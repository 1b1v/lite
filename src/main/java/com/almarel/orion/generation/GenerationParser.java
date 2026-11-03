package com.almarel.orion.generation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.brush.fawe.builtins.FaweParserService;
import com.almarel.orion.framework.command.exceptions.CommandException;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.entity.Player;

@Singleton
public class GenerationParser {

    private final FaweParserService faweParserService;

    @Inject
    public GenerationParser(FaweParserService faweParserService) {
        this.faweParserService = faweParserService;
    }

    // --- DTO ---

    public record PlatformSettings(Pattern pattern, int radius) {}
    public record OnionSettings(Pattern pattern) {}

    // --- PARSERS ---

    public PlatformSettings parsePlatform(Player player, String[] args) {
        String patternArg = "white_stained_glass";
        int radius = 100;

        if (args.length == 1) {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                patternArg = args[0];
            }
        } else if (args.length >= 2) {
            patternArg = args[0];
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new CommandException(OrionMessage.CMD_INVALID_NUMBER, args[1]);
            }
        }

        if (radius < 1) radius = 1;

        Pattern pattern = faweParserService.parsePattern(player, patternArg);
        return new PlatformSettings(pattern, radius);
    }

    public OnionSettings parseOnion(Player player, String[] args) {
        String patternArg = (args.length > 0) ? args[0] : "lapis_block";
        Pattern pattern = faweParserService.parsePattern(player, patternArg);
        return new OnionSettings(pattern);
    }
}