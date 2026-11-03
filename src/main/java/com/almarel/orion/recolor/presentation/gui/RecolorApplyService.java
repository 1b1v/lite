package com.almarel.orion.recolor.presentation.gui;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.integration.worldedit.EditSessionRunner;
import com.almarel.orion.framework.integration.worldedit.WorldEditSessionResolver;
import com.almarel.orion.recolor.shape.ShapeAwareStateTransferor;
import com.almarel.orion.recolor.core.RecolorMapBuilder;
import com.almarel.orion.recolor.core.RecolorPattern;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Singleton
public class RecolorApplyService {

    private final RecolorMapBuilder mapBuilder;
    private final ShapeAwareStateTransferor transferor;
    private final TaskRunner taskRunner;
    private final WorldEditSessionResolver sessionResolver;
    private final EditSessionRunner editSessionRunner;

    @Inject
    public RecolorApplyService(RecolorMapBuilder mapBuilder,
                               ShapeAwareStateTransferor transferor,
                               TaskRunner taskRunner,
                               WorldEditSessionResolver sessionResolver,
                               EditSessionRunner editSessionRunner) {
        this.mapBuilder = mapBuilder;
        this.transferor = transferor;
        this.taskRunner = taskRunner;
        this.sessionResolver = sessionResolver;
        this.editSessionRunner = editSessionRunner;
    }

    /**
     * Асинхронное применение recolor-пар из GUI:
     * - snapshot desiredTargets в main thread
     * - build mapping + setBlocks в async
     * - сообщения игроку в main thread
     */
    public void apply(Player player, Region region, Map<BlockType, BlockType> desiredTargets) {
        var weCtx = sessionResolver.resolve(player);
        if (weCtx.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No active WorldEdit session.");
            return;
        }

        UUID uuid = player.getUniqueId();
        long startTime = System.currentTimeMillis();
        Map<BlockType, BlockType> desiredSnapshot = Map.copyOf(desiredTargets);
        var wePlayer = weCtx.get().wePlayer();
        var localSession = weCtx.get().session();

        taskRunner.runAsync(() -> executeRecolor(uuid, wePlayer, localSession, region, desiredSnapshot, startTime));
    }

    private void executeRecolor(UUID uuid,
                                com.sk89q.worldedit.entity.Player wePlayer,
                                com.sk89q.worldedit.LocalSession localSession,
                                Region region,
                                Map<BlockType, BlockType> desiredTargets,
                                long startTime) {

        Map<BlockType, BlockType> resolvedMapping = mapBuilder.build(desiredTargets);
        if (resolvedMapping.isEmpty()) {
            dispatch(uuid, ChatColor.RED + "No shape-matching candidates found for the configured pairs.");
            return;
        }

        try {
            int changedBlocks = modifyBlocks(localSession, wePlayer, region, resolvedMapping);
            notifySuccess(uuid, changedBlocks, startTime);
        } catch (Exception e) {
            handleError(uuid, e);
        }
    }

    /**
     * Непосредственное применение изменений блоков через WorldEdit EditSession.
     */
    private int modifyBlocks(com.sk89q.worldedit.LocalSession localSession,
                             com.sk89q.worldedit.entity.Player wePlayer,
                             Region region,
                             Map<BlockType, BlockType> mapping) throws Exception {
        return editSessionRunner.withEditSession(localSession, wePlayer, editSession -> {
            RecolorPattern pattern = new RecolorPattern(editSession, mapping, transferor);
            return editSession.setBlocks(region, pattern);
        });
    }

    /**
     * Формирование и отправка отчета об успешном завершении операции.
     */
    private void notifySuccess(UUID uuid, int changedBlocks, long startTime) {
        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        String message = String.format(Locale.ROOT, "%sRecolor done: %d blocks changed (%.2fs).",
                ChatColor.GREEN, changedBlocks, elapsedSeconds);
        dispatch(uuid, message);
    }

    /**
     * Обработка и трансляция исключений в сообщения для игрока.
     */
    private void handleError(UUID uuid, Exception e) {
        if (e instanceof MaxChangedBlocksException) {
            dispatch(uuid, ChatColor.RED + "Too many blocks changed, limit exceeded.");
        } else if (e instanceof IllegalStateException) {
            dispatch(uuid, ChatColor.RED + e.getMessage());
        } else {
            dispatch(uuid, ChatColor.RED + "Recolor failed: " + e.getMessage());
        }
    }

    private void dispatch(UUID uuid, String message) {
        taskRunner.runSyncIfOnline(uuid, p -> p.sendMessage(message));
    }
}