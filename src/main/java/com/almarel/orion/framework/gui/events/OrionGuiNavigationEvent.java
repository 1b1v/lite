package com.almarel.orion.framework.gui.events;

import com.almarel.orion.framework.gui.data.enums.MenuType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OrionGuiNavigationEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final MenuType targetMenu;
    private final Object context;

    public OrionGuiNavigationEvent(Player player, MenuType targetMenu, Object context) {
        this.player = player;
        this.targetMenu = targetMenu;
        this.context = context;
    }

    public OrionGuiNavigationEvent(Player player, MenuType targetMenu) {
        this(player, targetMenu, null);
    }

    public Player getPlayer() {
        return player;
    }

    public MenuType getTargetMenu() {
        return targetMenu;
    }

    public Object getContext() {
        return context;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}