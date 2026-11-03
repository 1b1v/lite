package com.almarel.orion.curve.liana;

import com.almarel.orion.curve.CurveArgsParser;
import com.almarel.orion.curve.CurveCommandUtils;
import com.almarel.orion.curve.data.LianaCommandArgs;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.infrastructure.CommandArgsSupport;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.integration.worldedit.WorldEditSessionResolver;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class LianaCommand extends AbstractCommand {

    private static final List<String> COMMANDS = List.of("//liana", "/liana", "liana", "//lian", "/lian", "lian");
    private static final String PERMISSION = OrionPermissions.CURVE_LIANA;

    private static final List<String> SAG_SUGGESTIONS = List.of("10", "20", "40");
    private static final List<String> STEM_SUGGESTIONS = List.of("5", "10", "15");
    private static final List<String> COUNT_SUGGESTIONS = List.of("1", "2", "3");

    private final LianaGenerationService lianaService;
    private final MessageManager messageManager;
    private final CurveArgsParser curveArgsParser;
    private final Plugin plugin;
    private final TaskRunner taskRunner;
    private final WorldEditSessionResolver sessionResolver;

    @Inject
    public LianaCommand(FaweSuggestionProvider faweService,
                        LianaGenerationService lianaService,
                        MessageManager messageManager,
                        CurveArgsParser curveArgsParser,
                        Plugin plugin,
                        TaskRunner taskRunner,
                        WorldEditSessionResolver sessionResolver) {
        super(faweService);
        this.lianaService = lianaService;
        this.messageManager = messageManager;
        this.curveArgsParser = curveArgsParser;
        this.plugin = plugin;
        this.taskRunner = taskRunner;
        this.sessionResolver = sessionResolver;
    }

    @Override
    public @NotNull String getPermission() {
        return PERMISSION;
    }

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (!hasAccess(player)) {
            return true;
        }

        try {
            Optional<WorldEditSessionResolver.WeSession> weSessionOpt = sessionResolver.resolve(player);
            if (weSessionOpt.isEmpty()) {
                return true;
            }

            WorldEditSessionResolver.WeSession weSession = weSessionOpt.get();
            com.sk89q.worldedit.entity.Player wePlayer = weSession.wePlayer();
            LocalSession localSession = weSession.session();

            Optional<List<Vector3>> pointsOpt = extractCurvePoints(player, wePlayer, localSession);
            if (pointsOpt.isEmpty()) {
                return true;
            }

            ParserContext context = buildParserContext(wePlayer);
            LianaCommandArgs cmdArgs = curveArgsParser.parseLiana(args, context);

            dispatchLianaGeneration(player, wePlayer, localSession, pointsOpt.get(), cmdArgs);
        } catch (Exception e) {
            messageManager.send(player, OrionMessage.REGION_GEN_ERROR, e.getMessage());
        }

        return true;
    }

    private boolean hasAccess(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            messageManager.send(player, OrionMessage.CMD_NO_PERM);
            return false;
        }
        return true;
    }

    private Optional<List<Vector3>> extractCurvePoints(Player player,
                                                       com.sk89q.worldedit.entity.Player wePlayer,
                                                       LocalSession localSession) {
        List<Vector3> points = CurveCommandUtils.extractPoints(wePlayer, localSession);
        if (points == null) {
            messageManager.send(player, OrionMessage.REGION_SEL_REQUIRED);
            return Optional.empty();
        }
        return Optional.of(points);
    }

    private ParserContext buildParserContext(com.sk89q.worldedit.entity.Player wePlayer) {
        ParserContext context = new ParserContext();
        context.setActor(wePlayer);
        context.setWorld(wePlayer.getWorld());
        return context;
    }

    private void dispatchLianaGeneration(Player player,
                                         com.sk89q.worldedit.entity.Player wePlayer,
                                         LocalSession localSession,
                                         List<Vector3> points,
                                         LianaCommandArgs cmdArgs) {
        EditSession editSession = createAirMaskedSession(wePlayer, localSession);

        CurveCommandUtils.executeGeneration(
                plugin,
                taskRunner,
                lianaService.generateLiana(player, points, cmdArgs, editSession),
                localSession,
                editSession
        );
    }

    private EditSession createAirMaskedSession(com.sk89q.worldedit.entity.Player wePlayer, LocalSession localSession) {
        EditSession editSession = localSession.createEditSession(wePlayer);
        editSession.setMask(new BlockTypeMask(editSession, BlockTypes.AIR));
        return editSession;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (!player.hasPermission(PERMISSION)) {
            return List.of();
        }

        int actualIndex = args.length - 1;
        boolean skippedPattern = args.length > 1 && curveArgsParser.isNumeric(args[0]);

        if (!skippedPattern && actualIndex == 0) {
            return CommandArgsSupport.filterSuggestions(delegateToFawe(player, args, "/set"), args[0]);
        }

        int numericIndex = skippedPattern ? actualIndex : actualIndex - 1;
        return handleNumericTabComplete(args[actualIndex], numericIndex);
    }

    private List<String> handleNumericTabComplete(String currentArg, int numericIndex) {
        return switch (numericIndex) {
            case 0 -> CommandArgsSupport.filterSuggestions(SAG_SUGGESTIONS, currentArg);
            case 1 -> CommandArgsSupport.filterSuggestions(STEM_SUGGESTIONS, currentArg);
            case 2 -> CommandArgsSupport.filterSuggestions(COUNT_SUGGESTIONS, currentArg);
            default -> List.of();
        };
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return COMMANDS;
    }
}