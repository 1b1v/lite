package com.almarel.orion.framework.integration.versioncheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.data.OrionEdition;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.notification.LoginNotification;
import com.almarel.orion.framework.notification.NotificationButtons;
import com.almarel.orion.framework.notification.NotificationPriority;
import com.almarel.orion.framework.notification.NotificationTextFormatter;
import com.almarel.orion.framework.integration.versioncheck.data.PromotionInfo;
import com.almarel.orion.framework.integration.versioncheck.data.VersionManifest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * Shows a promotional message advertising the Full edition to Lite admins.
 * Never triggers on the Full edition itself — gated by OrionEdition.
 */
@Singleton
public class LitePromotionNotificationService implements LoginNotification {

    private static final String COOLDOWN_PREFIX = "lite_promo:";

    private final OrionEdition edition;
    private final UpdateCheckService updateCheckService;
    private final UpdateNotificationCooldownService cooldownService;
    private final UpdateNotificationSnoozeService snoozeService;

    @Inject
    public LitePromotionNotificationService(OrionEdition edition,
                                            UpdateCheckService updateCheckService,
                                            UpdateNotificationCooldownService cooldownService,
                                            UpdateNotificationSnoozeService snoozeService) {
        this.edition = edition;
        this.updateCheckService = updateCheckService;
        this.cooldownService = cooldownService;
        this.snoozeService = snoozeService;
    }

    @Override
    public boolean shouldNotify(Player player) {
        if (edition != OrionEdition.LITE) {
            return false;
        }
        if (!player.hasPermission(OrionPermissions.ADMIN_NOTICE)) {
            return false;
        }
        if (snoozeService.isSnoozed(player.getUniqueId())) {
            return false;
        }
        if (!cooldownService.shouldShow(cooldownKey(player))) {
            return false;
        }

        PromotionInfo promotion = getPromotionInfo();
        return promotion != null && promotion.isEnabled();
    }

    @Override
    public Component render(Player player) {
        PromotionInfo promotion = getPromotionInfo();

        String template = NotificationTextFormatter.orDefault(
                promotion.getHeadline(),
                "Try <light_purple>1b1v</light_purple> — unlock the full toolkit!");

        return MiniMessage.miniMessage().deserialize(template)
                .append(Component.text("  "))
                .append(buildButtons(promotion));
    }

    @Override
    public void onShown(Player player) {
        cooldownService.markShown(cooldownKey(player));
    }

    @Override
    public int getPriority() {
        return NotificationPriority.LITE_PROMOTION.value();
    }

    private Component buildButtons(PromotionInfo promotion) {
        Component skip = NotificationButtons.commandButton(
                "[ Do not show ]", "/1b1v notice",
                "Click to silence update notices");

        String buttonLabel = NotificationTextFormatter.orDefault(promotion.getButtonLabel(), "Learn More");

        if (promotion.getUrl() != null && !promotion.getUrl().isBlank()) {
            Component link = NotificationButtons.linkButton(
                    "[ " + buttonLabel + " ]", promotion.getUrl(),
                    "Click to open: " + promotion.getUrl());
            return link.append(Component.text("  ")).append(skip);
        }
        return skip;
    }

    private PromotionInfo getPromotionInfo() {
        VersionManifest manifest = updateCheckService.getCachedManifest();
        return manifest == null ? null : manifest.getPromotion();
    }

    private String cooldownKey(Player player) {
        return COOLDOWN_PREFIX + player.getUniqueId();
    }
}