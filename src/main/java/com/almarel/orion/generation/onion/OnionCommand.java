package com.almarel.orion.generation.onion;

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
import com.almarel.orion.generation.GenerationParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

@Singleton
public class OnionCommand extends AbstractCommand {

    public static final List<String> ALIASES_ONION = List.of("//onion", "/onion", "onion");
    private static final String PERMISSION = OrionPermissions.GENERATION_ONION;
    private static final String TEMPLATE_BLOCK_COMMAND = "/set";

    private final OnionGenerationService genService;
    private final GenerationParser argsParser;
    private final MessageManager messageManager;
    private final WorldEditSessionResolver sessionResolver;
    private final EditSessionRunner editSessionRunner;
    private final AsyncExecutor asyncExecutor;
    private final TaskRunner taskRunner;
    private final JavaPlugin plugin;

    @Inject
    public OnionCommand(OnionGenerationService genService,
                        GenerationParser argsParser,
                        MessageManager messageManager,
                        FaweSuggestionProvider faweSuggestionProvider,
                        WorldEditSessionResolver sessionResolver,
                        EditSessionRunner editSessionRunner,
                        AsyncExecutor asyncExecutor,
                        TaskRunner taskRunner,
                        JavaPlugin plugin) {
        super(faweSuggestionProvider);
        this.genService = genService;
        this.argsParser = argsParser;
        this.messageManager = messageManager;
        this.sessionResolver = sessionResolver;
        this.editSessionRunner = editSessionRunner;
        this.asyncExecutor = asyncExecutor;
        this.taskRunner = taskRunner;
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getPermission() {
        return PERMISSION;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        if (ALIASES_ONION.contains(cmd)) {
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
            messageManager.send(player, OrionMessage.REGION_SEL_REQUIRED);
            return true;
        }
        var ctx = weCtx.get();

        var selectionWorld = ctx.session().getSelectionWorld();
        if (selectionWorld == null) {
            messageManager.send(player, OrionMessage.REGION_SEL_REQUIRED);
            return true;
        }

        try {
            Region region = ctx.session().getSelection(selectionWorld);
            GenerationParser.OnionSettings settings = argsParser.parseOnion(player, args);
            applyGeneration(player, ctx, region, settings);
        } catch (IncompleteRegionException e) {
            messageManager.send(player, OrionMessage.REGION_SEL_REQUIRED);
        } catch (CommandException e) {
            handleError(player, e);
        }

        return true;
    }

    private void applyGeneration(Player player,
                                 WorldEditSessionResolver.WeSession ctx,
                                 Region region,
                                 GenerationParser.OnionSettings settings) {
        editSessionRunner.withEditSessionAsync(ctx.session(), ctx.wePlayer(), asyncExecutor.getExecutor(),
                        editSession -> genService.fillOnion(region, settings.pattern(), editSession))
                .thenAccept(count -> taskRunner.runSyncIfOnline(player, p ->
                        messageManager.send(p, OrionMessage.REGION_COMPLETE, count)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    plugin.getLogger().log(Level.WARNING, "1b1v: Onion generation failed", cause);
                    taskRunner.runSyncIfOnline(player, p ->
                            messageManager.send(p, OrionMessage.REGION_GEN_ERROR, cause.getMessage()));
                    return null;
                });
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return ALIASES_ONION;
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