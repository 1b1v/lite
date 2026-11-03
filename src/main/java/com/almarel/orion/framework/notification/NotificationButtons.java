package com.almarel.orion.framework.notification;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Stateless-фабрика кликабельных кнопок для login-уведомлений.
 * Это НЕ сервис и НЕ менеджер — чистые статические функции без состояния,
 * поэтому DI (Guice) для неё не требуется (см. SemanticVersionComparator
 * как прецедент такого же решения в проекте).
 */
public final class NotificationButtons {

    private NotificationButtons() {}

    public static Component linkButton(String label, String url, String hoverText) {
        return Component.text(label, NamedTextColor.GREEN)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.WHITE)));
    }

    public static Component commandButton(String label, String command, String hoverText) {
        return commandButton(label, NamedTextColor.GRAY, command, hoverText);
    }

    public static Component commandButton(String label, NamedTextColor color, String command, String hoverText) {
        return Component.text(label, color)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.WHITE)));
    }
}