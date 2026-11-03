package com.almarel.orion.region.centervertical;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
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
import com.almarel.orion.region.RegionParser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
public class CenterVerticalCommand extends AbstractCommand {

    public static final List<String> ALIASES = List.of(
            "//centerver", "/centerver", "centerver",
            "//cver", "/cver", "cver"
    );
    private static final String PERMISSION = OrionPermissions.REGION_CENTER_VERTICAL;
    private static final String TEMPLATE_BLOCK_COMMAND = "/set";

    private final CenterVerticalGenerationService genService;
    private final RegionParser argsParser;
    private final MessageManager messageManager;
    private final WorldEditSessionResolver sessionResolver;
    private final EditSessionRunner editSessionRunner;
    private final AsyncExecutor asyncExecutor;
    private final TaskRunner taskRunner;

    @Inject
    public CenterVerticalCommand(CenterVerticalGenerationService genService,
                                 RegionParser argsParser,
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
        if (ALIASES.contains(cmd)) {
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
            Region region = ctx.session().getSelection(ctx.wePlayer().getWorld());
            RegionParser.CenterVerticalSettings settings = argsParser.parseCenterVertical(player, args);
            applyGeneration(player, ctx, region, settings);
        } catch (IncompleteRegionException e) {
            player.sendMessage("§c[1b1v] First, select a region (cuboid).");
        } catch (CommandException e) {
            handleError(player, e);
        }

        return true;
    }

    /**
     * Единая точка оркестрации: createEditSession -> fillColumn -> remember -> close
     * выполняется через EditSessionRunner в AsyncExecutor, результат уведомляется через TaskRunner.
     */
    private void applyGeneration(Player player,
                                 WorldEditSessionResolver.WeSession ctx,
                                 Region region,
                                 RegionParser.CenterVerticalSettings settings) {
        editSessionRunner.withEditSessionAsync(ctx.session(), ctx.wePlayer(), asyncExecutor.getExecutor(),
                        editSession -> genService.fillColumn(region, settings.pattern(), editSession))
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
        return ALIASES;
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