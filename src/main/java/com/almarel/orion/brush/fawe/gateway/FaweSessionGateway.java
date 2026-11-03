package com.almarel.orion.brush.fawe.gateway;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.almarel.orion.brush.fawe.util.BrushFormatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

@Singleton
public class FaweSessionGateway {

    private final JavaPlugin plugin;

    @Inject
    public FaweSessionGateway(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * ЧТЕНИЕ. Никогда не создаёт и не заменяет инструмент в слоте.
     * Для записи кисти использовать acquirePureBrushTool().
     * <p>
     * create=false критичен: LocalSession.getBrushTool(create=true) заменяет любой
     * не-BrushTool (flood, repl, tree...) пустым BrushTool, т.е. "прочитать" ломало инструмент.
     */
    public BrushTool getBrushTool(Player player, Material type) {
        try {
            LocalSession session = getSession(player);
            if (session == null) return null;

            if (isHoldingToolType(player, type)) {
                return session.getBrushTool(BukkitAdapter.adapt(player), false);
            }

            return resolveToolByType(session, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ЧТЕНИЕ. Возвращает то, что реально лежит в слоте основной руки (любой Tool, не только BrushTool).
     * Ничего не создаёт и не заменяет. Только Main Thread (читает предмет в руке).
     * Побочный эффект LocalSession.getTool(Player): однократная загрузка дефолтных палочек.
     */
    public Tool getToolInSlot(Player player) {
        LocalSession session = getSession(player);
        return (session == null) ? null : session.getTool(BukkitAdapter.adapt(player));
    }

    private boolean isHoldingToolType(Player player, Material type) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && mainHand.getType() == type;
    }

    private BrushTool resolveToolByType(LocalSession session, Material type) {
        Tool tool = session.getTool(BukkitAdapter.asItemType(type));
        return (tool instanceof BrushTool brushTool) ? brushTool : null;
    }

    /**
     * Возвращает "чистый" BrushTool для установки кисти в руку игрока.
     * <p>
     * Повторяет шаг ванильного BrushCommands.process(): session.setTool(player, null)
     * перед getBrushTool(create=true). Orion вызывает процессоры напрямую, минуя
     * BrushCommands, поэтому без этого шага подкласс BrushTool (DistanceWand, InspectBrush)
     * остаётся в слоте, и кисть оказывается внутри него.
     * <p>
     * Только для путей ЗАПИСИ кисти. Для чтения использовать getBrushTool().
     */
    public BrushTool acquirePureBrushTool(com.sk89q.worldedit.entity.Player wePlayer) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        BrushTool existing = session.getBrushTool(wePlayer, false);

        if (existing != null && !isPureBrushTool(existing)) {
            session.setTool(wePlayer, (Tool) null);
        }
        return session.getBrushTool(wePlayer, true);
    }

    private boolean isPureBrushTool(BrushTool tool) {
        return tool.getClass() == BrushTool.class;
    }

    public boolean hasActiveBrush(Player player, Material toolType) {
        BrushTool tool = getBrushTool(player, toolType);
        return tool != null && tool.getBrush() != null;
    }

    public int getBrushSize(Player player, Material toolType) {
        BrushTool tool = getBrushTool(player, toolType);
        return (tool != null) ? (int) tool.getSize() : 5;
    }

    public void setBrushSize(Player player, Material toolType, int size) {
        BrushTool tool = getBrushTool(player, toolType);
        if (tool != null) {
            tool.setSize(size);
        }
    }

    public int getBrushRange(Player player, Material toolType) {
        BrushTool tool = getBrushTool(player, toolType);
        return (tool != null) ? tool.getRange() : -1;
    }

    public void setBrushRange(Player player, Material toolType, int range) {
        BrushTool tool = getBrushTool(player, toolType);
        if (tool != null) {
            tool.setRange(range);
        }
    }

    public void unbindBrush(Player player, Material toolType) {
        try {
            LocalSession session = getSession(player);
            if (session != null) {
                session.setTool(BukkitAdapter.asItemType(toolType), null);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to unbind brush for tool: " + toolType, e);
        }
    }

    public String getBrushMask(Player player, Material toolType) {
        BrushTool tool = getBrushTool(player, toolType);
        return (tool != null && tool.getMask() != null) ? tool.getMask().toString() : null;
    }

    public boolean setBrushMask(Player player, Material toolType, String mask) {
        BrushTool tool = getBrushTool(player, toolType);
        if (tool == null) return false;

        if (mask == null || mask.isEmpty()) {
            tool.setMask(null);
            return true;
        }

        try {
            LocalSession session = getSession(player);
            Mask parsedMask = parseMask(player, session, mask);
            tool.setMask(parsedMask);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse/set brush mask: '" + mask + "' for player " + player.getName(), e);
            return false;
        }
    }

    public boolean setSourceMask(Player player, Material toolType, String sourceMask) {
        BrushTool tool = getBrushTool(player, toolType);
        if (tool == null) return false;

        if (sourceMask == null || sourceMask.isEmpty()) {
            tool.setSourceMask(null);
            return true;
        }

        try {
            LocalSession session = getSession(player);
            Mask parsedMask = parseMask(player, session, sourceMask);
            tool.setSourceMask(parsedMask);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse/set source mask: '" + sourceMask + "' for player " + player.getName(), e);
            return false;
        }
    }

    public String getBrushPattern(Player player, Material toolType) {
        BrushTool tool = getBrushTool(player, toolType);
        if (tool == null) return null;

        Pattern pattern = tool.getMaterial();
        return (pattern != null) ? BrushFormatter.formatPattern(pattern) : null;
    }

    public boolean setBrushPattern(Player player, Material toolType, String pattern) {
        BrushTool tool = getBrushTool(player, toolType);
        if (tool == null) return false;

        if (pattern == null || pattern.isEmpty()) {
            return true;
        }

        try {
            LocalSession session = getSession(player);
            Pattern parsedPattern = parsePattern(player, session, pattern);
            tool.setFill(parsedPattern);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse/set brush pattern: '" + pattern + "' for player " + player.getName(), e);
            return false;
        }
    }

    // ========================================================================
    // WORLDEDIT / FAWE HELPERS
    // ========================================================================

    private LocalSession getSession(Player player) {
        return WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
    }

    private ParserContext createParserContext(Player player, LocalSession session) {
        ParserContext context = new ParserContext();
        context.setActor(BukkitAdapter.adapt(player));
        context.setSession(session);
        return context;
    }

    private Mask parseMask(Player player, LocalSession session, String expression) throws Exception {
        ParserContext context = createParserContext(player, session);
        return WorldEdit.getInstance().getMaskFactory().parseFromInput(expression, context);
    }

    private Pattern parsePattern(Player player, LocalSession session, String expression) throws Exception {
        ParserContext context = createParserContext(player, session);
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(expression, context);
    }

    // ========================================================================
    // SESSION OWNER ADAPTER
    // ========================================================================

    public static class UuidSessionOwner implements SessionOwner {
        private final UUID uuid;

        public UuidSessionOwner(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public SessionKey getSessionKey() {
            return new SessionKey() {
                @Override
                public String getName() {
                    return uuid.toString();
                }

                @Override
                public UUID getUniqueId() {
                    return uuid;
                }

                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public boolean isPersistent() {
                    return true;
                }
            };
        }

        @Override
        public String[] getGroups() {
            return new String[0];
        }

        @Override
        public void checkPermission(String permission) throws AuthorizationException {
        }

        @Override
        public boolean hasPermission(String permission) {
            return false;
        }

        @Override
        public void setPermission(String permission, boolean value) {
        }
    }
}