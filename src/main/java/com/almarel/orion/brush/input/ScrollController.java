package com.almarel.orion.brush.input;

import com.google.inject.Singleton;
import com.sk89q.worldedit.WorldEdit;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.ScrollAction;
import com.almarel.orion.brush.services.BrushRotationService;
import com.almarel.orion.framework.data.MainConfig;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Set;
import com.google.inject.Inject;

@Singleton
public class ScrollController implements Listener {

    private final BrushStateManager stateManager;
    private final MainConfig mainConfig;
    private final MessageManager messageManager;
    private final FaweIntegrationService faweIntegrationService;
    private final GuiStateManager sessionManager;
    private final BrushRotationService brushRotationService;

    @Inject
    public ScrollController(GuiStateManager sessionManager,
                            BrushStateManager stateManager,
                            MainConfig mainConfig,
                            MessageManager messageManager,
                            FaweIntegrationService faweIntegrationService,
                            BrushRotationService brushRotationService) {
        this.stateManager = stateManager;
        this.mainConfig = mainConfig;
        this.messageManager = messageManager;
        this.faweIntegrationService = faweIntegrationService;
        this.sessionManager = sessionManager;
        this.brushRotationService = brushRotationService;
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (!sessionManager.getSession(player).isScrollModeEnabled()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        ScrollActionable state = getScrollableState(player, item.getType());
        if (state == null) return;

        if (state.getActiveScrollActions().isEmpty()) return;

        int direction = calculateScrollDirection(event.getPreviousSlot(), event.getNewSlot());
        if (direction == 0) return;

        event.setCancelled(true);
        handleScroll(player, state, direction);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (!sessionManager.getSession(player).isScrollModeEnabled()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        ScrollActionable state = getScrollableState(player, item.getType());
        if (state == null) return;

        if (state.getActiveScrollActions().isEmpty()) return;

        event.setCancelled(true);
        cycleScrollMode(player, state);
    }

    private ScrollActionable getScrollableState(Player player, Material tool) {
        BrushState state = stateManager.resolveActiveState(player, tool);
        if (state instanceof ScrollActionable scrollable) {
            return scrollable;
        }
        return null;
    }

    private int calculateScrollDirection(int oldSlot, int newSlot) {
        int diff = newSlot - oldSlot;
        int absDiff = Math.abs(diff);
        if (absDiff == 8) {
            return (diff > 0) ? 1 : -1;
        }
        if (absDiff > 2) return 0;
        int rawDir = (diff > 0) ? 1 : -1;
        return -rawDir;
    }

    private void cycleScrollMode(Player player, ScrollActionable state) {
        // Вся грязная логика переключения теперь инкапсулирована в интерфейсе!
        state.cycleNextScrollAction();

        ScrollAction next = state.getCurrentScrollAction();
        String modeName = getScrollActionName(next);
        sendActionBar(player, messageManager.get(OrionMessage.SCROLL_MODE_CHANGED).replace("{0}", modeName));
    }

    private void handleScroll(Player player, ScrollActionable state, int direction) {
        double shiftMult = player.isSneaking() ? 5.0 : 1.0;

        ScrollAction current = validateCurrentScrollAction(player, state);
        if (current == null) return;

        org.bukkit.Location playerLoc = player.getLocation();
        org.bukkit.util.Vector playerDir = playerLoc.getDirection();
        String axis = getDominantAxis(playerDir);
        int axisSign = getAxisSign(playerDir, axis);

        switch (current) {
            case SCALE -> handleScaleScroll(player, state, direction, shiftMult);
            case SIZE -> handleSizeScroll(player, state, direction, shiftMult);
            case SCALE_AXIS -> handleScaleAxisScroll(player, state, direction, shiftMult, axis);
            case SIZE_AXIS -> handleSizeAxisScroll(player, state, direction, shiftMult, axis);
            case ROTATION -> handleRotationScroll(player, state, direction, shiftMult, axis, axisSign);
            case BUFFER_SLOT -> handleBufferSlotScroll(player, state, direction);
        }
    }

    private ScrollAction validateCurrentScrollAction(Player player, ScrollActionable state) {
        ScrollAction current = state.getCurrentScrollAction();
        Set<ScrollAction> active = state.getActiveScrollActions();

        if (current == null || active == null || active.isEmpty()) return null;

        if (!active.contains(current)) {
            current = active.iterator().next();
            state.setCurrentScrollAction(current);
            String modeName = getScrollActionName(current);
            sendActionBar(player, messageManager.get(OrionMessage.SCROLL_MODE_RESET).replace("{0}", modeName));
        }
        return current;
    }

    private void handleScaleScroll(Player player, ScrollActionable state, int direction, double shiftMult) {
        if (state instanceof Scalable scalable) {
            double val = scalable.getScale() + (direction * 0.1 * shiftMult);
            val = Math.max(0.1, val);
            scalable.setScale(val);
            sendActionBar(player, messageManager.get(OrionMessage.SCROLL_SCALE).replace("{0}", format(val)));
        }
    }

    private void handleSizeScroll(Player player, ScrollActionable state, int direction, double shiftMult) {
        if (state instanceof Sizeable sizeable) {
            double limit = WorldEdit.getInstance().getConfiguration().maxBrushRadius;
            double val = sizeable.getSize() + (direction * 1.0 * shiftMult);
            val = Math.max(0.1, Math.min(val, limit));
            sizeable.setSize(val);

            if (state instanceof BrushState brushState) {
                faweIntegrationService.setBrushSize(player, brushState.getTool(), (int) Math.round(val));
            }
            sendActionBar(player, messageManager.get(OrionMessage.SCROLL_RADIUS).replace("{0}", format(val)));
        }
    }

    private void handleScaleAxisScroll(Player player, ScrollActionable state, int direction, double shiftMult, String axis) {
        if (state instanceof AxisScalable axisScalable) {
            double val = axisScalable.updateAxisScale(axis, direction * 0.1 * shiftMult);
            sendActionBar(player, messageManager.get(OrionMessage.SCROLL_SCALE_AXIS).replace("{0}", axis).replace("{1}", format(val)));
        }
    }

    private void handleSizeAxisScroll(Player player, ScrollActionable state, int direction, double shiftMult, String axis) {
        if (state instanceof AxisSizeable axisSizeable) {
            double val = axisSizeable.updateAxisSize(axis, direction * 1.0 * shiftMult);
            String formattedVal = String.format(Locale.ROOT, "%.0f", val);
            sendActionBar(player, messageManager.get(OrionMessage.SCROLL_SCALE_AXIS).replace("{0}", axis).replace("{1}", formattedVal));
        }
    }

    private void handleRotationScroll(Player player, ScrollActionable state, int direction, double shiftMult, String axis, int axisSign) {
        if (state instanceof Rotatable rotatable) {
            int rotStep = (shiftMult > 1.0) ? 45 : 15;
            int delta = direction * rotStep * axisSign;

            int newVal = applyRotationDelta(player, rotatable, axis, delta);
            String rotMsg = formatRotationMessage(axis, newVal);
            sendActionBar(player, messageManager.get(OrionMessage.SCROLL_ROTATION_BASE).replace("{0}", rotMsg));
        }
    }

    private void handleBufferSlotScroll(Player player, ScrollActionable state, int direction) {
    }

    // [ИЗМЕНЕНО] Добавлен Player player первым аргументом
    private int applyRotationDelta(org.bukkit.entity.Player player, Rotatable rotatable, String axis, int delta) {
        String action = switch (axis) {
            case "Y" -> "YAW";
            case "Z" -> "PITCH";
            default -> "ROLL";
        };

        // [ИЗМЕНЕНО] Вся логика ушла в Сервис.
        brushRotationService.applyBrushRotation(player, rotatable, action, delta);

        return 0;
    }

    private String formatRotationMessage(String axis, int newVal) {
        if (axis.equals("Y")) {
            return messageManager.get(OrionMessage.SCROLL_AXIS_Y).replace("{0}", String.valueOf(newVal));
        } else if (axis.equals("Z")) {
            return messageManager.get(OrionMessage.SCROLL_AXIS_X).replace("{0}", String.valueOf(newVal));
        } else {
            return messageManager.get(OrionMessage.SCROLL_AXIS_Z).replace("{0}", String.valueOf(newVal));
        }
    }

    private String getScrollActionName(ScrollAction action) {
        switch (action) {
            case SCALE: return messageManager.get(OrionMessage.ACTION_SCALE);
            case SCALE_AXIS: return messageManager.get(OrionMessage.ACTION_SCALE_AXIS);
            case SIZE_AXIS: return messageManager.get(OrionMessage.ACTION_SCALE_AXIS); // Переиспользуем тот же текст для чистоты
            case ROTATION: return messageManager.get(OrionMessage.ACTION_ROTATION);
            case BUFFER_SLOT: return messageManager.get(OrionMessage.ACTION_BUFFER_SLOT);
            case SIZE: return messageManager.get(OrionMessage.ACTION_RADIUS);
            default: return action.name();
        }
    }

    private String getDominantAxis(Vector dir) {
        double x = Math.abs(dir.getX());
        double y = Math.abs(dir.getY());
        double z = Math.abs(dir.getZ());
        if (y > x && y > z) return "Y";
        if (x > z) return "X";
        return "Z";
    }

    private int getAxisSign(Vector dir, String axis) {
        if (axis.equals("X")) return dir.getX() >= 0 ? 1 : -1;
        if (axis.equals("Y")) return dir.getY() >= 0 ? -1 : 1;
        return dir.getZ() >= 0 ? 1 : -1;
    }

    private String format(double val) {
        return String.format(Locale.ROOT, "%.2f", val);
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }
}