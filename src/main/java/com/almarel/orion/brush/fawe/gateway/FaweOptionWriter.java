package com.almarel.orion.brush.fawe.gateway;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.Maskable;
import com.almarel.orion.brush.data.capabilities.Patternable;
import com.almarel.orion.brush.data.capabilities.Rangeable;
import com.almarel.orion.brush.data.capabilities.Sizeable;
import com.almarel.orion.brush.data.capabilities.SourceMaskable;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FaweOptionWriter {

    private final FaweSessionGateway sessionGateway;

    // Player-scoped кэш для предотвращения Memory Leak и изоляции контекста
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Mask>> playerMaskCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Pattern>> playerPatternCache = new ConcurrentHashMap<>();

    @Inject
    public FaweOptionWriter(FaweSessionGateway sessionGateway) {
        this.sessionGateway = sessionGateway;
    }

    public boolean applyBrushOptions(Player player, BrushState state) {
        try {
            BrushTool tool = sessionGateway.getBrushTool(player, state.getTool());
            if (tool == null) return false;

            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));

            applySize(tool, state);
            applyRange(tool, state);
            applySourceMask(tool, player, session, state);
            applyPattern(tool, player, session, state);
            applyMask(tool, player, session, state);

            return true;

        } catch (Exception e) {
            // Try-Commit: FAWE parser выдал ошибку. Вызывающий код должен откатить DTO.
            return false;
        }
    }

    // --- Extracted Appliers ---

    private void applySize(BrushTool tool, BrushState state) {
        if (state instanceof Sizeable sizeable && sizeable.getSize() > 0) {
            tool.setSize(sizeable.getSize());
        }
    }

    private void applyRange(BrushTool tool, BrushState state) {
        if (state instanceof Rangeable rangeable && rangeable.getRange() != 0) {
            tool.setRange(rangeable.getRange());
        }
    }

    private void applySourceMask(BrushTool tool, Player player, LocalSession session, BrushState state) throws Exception {
        if (state instanceof SourceMaskable sourceMaskable) {
            String srcMaskStr = sourceMaskable.getSourceMask();
            if (srcMaskStr != null && !srcMaskStr.isEmpty()) {
                Mask srcMask = parseMaskCached(player.getUniqueId(), session, player, srcMaskStr);
                tool.setSourceMask(srcMask);
            } else {
                tool.setSourceMask(null);
            }
        }
    }

    private void applyPattern(BrushTool tool, Player player, LocalSession session, BrushState state) throws Exception {
        if (state instanceof Patternable patternable) {
            String patternStr = patternable.getPattern();
            if (patternStr != null && !patternStr.isEmpty()) {
                Pattern pattern = parsePatternCached(player.getUniqueId(), session, player, patternStr);
                tool.setFill(pattern);
            }
        }
    }

    private void applyMask(BrushTool tool, Player player, LocalSession session, BrushState state) throws Exception {
        if (state instanceof Maskable maskable) {
            String maskStr = maskable.getMask();
            if (maskStr != null && !maskStr.isEmpty()) {
                Mask mask = parseMaskCached(player.getUniqueId(), session, player, maskStr);
                tool.setMask(mask);
            } else {
                tool.setMask(null);
            }
        }
    }

    /**
     * Очистка кэша при дисконнекте игрока для предотвращения Memory Leak.
     * Должен вызываться из SessionLifecycleListener.
     */
    public void invalidatePlayerCache(UUID uuid) {
        playerMaskCache.remove(uuid);
        playerPatternCache.remove(uuid);
    }

    private Mask parseMaskCached(UUID playerUuid, LocalSession session, Player player, String maskStr) throws Exception {
        ConcurrentHashMap<String, Mask> cache = playerMaskCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());

        return cache.computeIfAbsent(maskStr, key -> {
            ParserContext context = new ParserContext();
            context.setActor(BukkitAdapter.adapt(player));
            context.setSession(session);
            return WorldEdit.getInstance().getMaskFactory().parseFromInput(key, context);
        });
    }

    private Pattern parsePatternCached(UUID playerUuid, LocalSession session, Player player, String patternStr) throws Exception {
        ConcurrentHashMap<String, Pattern> cache = playerPatternCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());

        return cache.computeIfAbsent(patternStr, key -> {
            ParserContext context = new ParserContext();
            context.setActor(BukkitAdapter.adapt(player));
            context.setSession(session);
            return WorldEdit.getInstance().getPatternFactory().parseFromInput(key, context);
        });
    }
}