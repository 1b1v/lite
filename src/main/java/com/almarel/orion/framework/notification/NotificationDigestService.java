// TYPE B: FULL FILE
package com.almarel.orion.framework.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Собирает все активные LoginNotification в ОДНО агрегированное сообщение
 * при входе игрока, вместо серии разрозненных сообщений.
 *
 * Ответственность строго ограничена оркестрацией:
 * - фильтрация по shouldNotify(),
 * - сортировка по приоритету,
 * - единая визуальная рамка (заголовок + маркер списка),
 * - одна отправка,
 * - вызов onShown() только для реально показанных блоков.
 *
 * Оформление содержимого блоков — ответственность самих LoginNotification.
 */
@Singleton
public class NotificationDigestService {

    private static final long JOIN_NOTIFICATION_DELAY_TICKS = 60L;
    private static final String BULLET_MARKER = "\u25aa ";

    private final JavaPlugin plugin;
    private final Set<LoginNotification> notifications; // Guice Multibinder предоставляет иммутабельный Set

    @Inject
    public NotificationDigestService(JavaPlugin plugin, Set<LoginNotification> notifications) {
        this.plugin = plugin;
        this.notifications = notifications;
    }

    public void onPlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkNow(player), JOIN_NOTIFICATION_DELAY_TICKS);
    }

    /**
     * Собирает и немедленно отправляет дайджест активных уведомлений игроку,
     * без задержки логина. Используется как при входе (через onPlayerJoin),
     * так и при ручном форс-рефреше манифеста ("/1b1v checkupdate").
     * Подчиняется обычным правилам cooldown/snooze — не является их обходом.
     */
    public void checkNow(Player player) {
        if (!player.isOnline()) {
            return;
        }

        List<LoginNotification> activeNotifications = collectActive(player);
        if (activeNotifications.isEmpty()) {
            return;
        }

        List<LoginNotification> shownNotifications = new ArrayList<>();
        List<Component> renderedItems = renderNotifications(activeNotifications, player, shownNotifications);
        if (renderedItems.isEmpty()) {
            return;
        }

        Component digest = buildDigestMessage(renderedItems);
        player.sendMessage(digest);

        markAsShown(shownNotifications, player);
    }

    private List<LoginNotification> collectActive(Player player) {
        return notifications.stream()
                .filter(notification -> notification.shouldNotify(player))
                .sorted(Comparator.comparingInt(LoginNotification::getPriority))
                .toList();
    }

    private List<Component> renderNotifications(List<LoginNotification> active,
                                                Player player,
                                                List<LoginNotification> shownCollector) {
        List<Component> items = new ArrayList<>();

        for (LoginNotification notification : active) {
            Component rendered = safeRender(notification, player);
            if (rendered != null) {
                items.add(formatNotificationItem(rendered));
                shownCollector.add(notification);
            }
        }

        return items;
    }

    private Component formatNotificationItem(Component content) {
        return Component.text(BULLET_MARKER, NamedTextColor.GRAY)
                .append(content.colorIfAbsent(NamedTextColor.WHITE));
    }

    private Component buildDigestMessage(List<Component> items) {
        List<Component> parts = new ArrayList<>(items.size() + 1);
        parts.add(buildHeader());
        parts.addAll(items);

        return Component.join(JoinConfiguration.separator(Component.newline()), parts);
    }

    /**
     * Защита от битых MiniMessage-тегов в ручном manifest.json (опечатка в
     * criticalReason/headline). Один сломанный блок не должен ронять весь
     * дайджест — остальные уведомления игрок обязан увидеть как обычно.
     */
    private Component safeRender(LoginNotification notification, Player player) {
        try {
            return notification.render(player);
        } catch (Exception ex) {
            plugin.getLogger().warning("[1b1v] Failed to render notification "
                    + notification.getClass().getSimpleName() + ": " + ex.getMessage());
            return null;
        }
    }

    private Component buildHeader() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("1b1v", NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Notice", NamedTextColor.YELLOW));
    }

    private void markAsShown(List<LoginNotification> shownNotifications, Player player) {
        for (LoginNotification notification : shownNotifications) {
            notification.onShown(player);
        }
    }
}