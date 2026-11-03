package com.almarel.orion.generation.platform;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.math.BlockVector3;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.exceptions.CommandException;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.almarel.orion.framework.integration.worldedit.EditSessionRunner;
import com.almarel.orion.framework.integration.worldedit.WorldEditSessionResolver;
import com.almarel.orion.generation.GenerationParser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
public class PlatformCommand extends AbstractCommand {

    public static final List<String> ALIASES_PLATFORM = List.of(
            "//platform", "/platform", "platform",
            "//plat", "/plat", "plat"
    );
    private static final String PERMISSION = OrionPermissions.GENERATION_PLATFORM;
    private static final String TEMPLATE_BLOCK_COMMAND = "/set";

    private final PlatformGenerationService genService;
    private final GenerationParser argsParser;
    private final MessageManager messageManager;
    private final WorldEditSessionResolver sessionResolver;
    private final EditSessionRunner editSessionRunner;
    private final AsyncExecutor asyncExecutor;
    private final TaskRunner taskRunner;

    @Inject
    public PlatformCommand(PlatformGenerationService genService,
                           GenerationParser argsParser,
                           MessageManager messageManager,
                           FaweSuggestionProvider faweSuggestionProvider,
                           WorldEditSessionResolver sessionResolver,
                           EditSessionRunner editSessionRunner,
                           AsyncExecutor asyncExecutor,
                           TaskRunner taskRunner) {
        super(faweSuggestionProvider);
        this.genService = genService;
        this.argsParser = argsParser;
        this.messageManager = messageManager;
        this.sessionResolver = sessionResolver;
        this.editSessionRunner = editSessionRunner;
        this.asyncExecutor = asyncExecutor;
        this.taskRunner = taskRunner;
    }

    @Override
    public @NotNull String getPermission() {
        return PERMISSION;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        if (ALIASES_PLATFORM.contains(cmd)) {
            if (!player.hasPermission(PERMISSION)) return List.of();
            if (args.length == 1) {
                return delegateToFawe(player, args, TEMPLATE_BLOCK_COMMAND);
            }
        }
        return List.of();
    }

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (!checkPerm(player, PERMISSION)) return true;

        var weCtx = sessionResolver.resolve(player);
        if (weCtx.isEmpty()) {
            player.sendMessage("§c[1b1v] No active WorldEdit session.");
            return true;
        }
        var ctx = weCtx.get();

        try {
            GenerationParser.PlatformSettings settings = argsParser.parsePlatform(player, args);
            BlockVector3 center = extractPlatformCenter(player);
            applyGeneration(player, ctx, center, settings);
        } catch (CommandException e) {
            handleError(player, e);
        }

        return true;
    }

    private BlockVector3 extractPlatformCenter(Player player) {
        Location loc = player.getLocation();
        return BlockVector3.at(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
    }

    private void applyGeneration(Player player,
                                 WorldEditSessionResolver.WeSession ctx,
                                 BlockVector3 center,
                                 GenerationParser.PlatformSettings settings) {
        editSessionRunner.withEditSessionAsync(ctx.session(), ctx.wePlayer(), asyncExecutor.getExecutor(),
                        editSession -> genService.fillPlatform(center, settings.radius(), settings.pattern(), editSession))
                .thenAccept(count -> taskRunner.runSyncIfOnline(player, p ->
                        messageManager.send(p, OrionMessage.REGION_COMPLETE, count)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    taskRunner.runSyncIfOnline(player, p ->
                            messageManager.send(p, OrionMessage.REGION_GEN_ERROR, cause.getMessage()));
                    return null;
                });
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return ALIASES_PLATFORM;
    }

    private void handleError(Player player, CommandException e) {
        messageManager.send(player, e.getOrionMessage(), e.getArgs());
    }

    private boolean checkPerm(Player p, String perm) {
        if (!p.hasPermission(perm)) {
            messageManager.send(p, OrionMessage.CMD_NO_PERM);
            return false;
        }
        return true;
    }
}