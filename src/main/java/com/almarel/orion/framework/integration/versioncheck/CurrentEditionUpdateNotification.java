package com.almarel.orion.framework.integration.versioncheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.data.OrionEdition;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.notification.LoginNotification;
import com.almarel.orion.framework.notification.NotificationButtons;
import com.almarel.orion.framework.notification.NotificationPriority;
import com.almarel.orion.framework.notification.NotificationTextFormatter;
import com.almarel.orion.framework.integration.versioncheck.data.EditionManifest;
import com.almarel.orion.framework.integration.versioncheck.data.VersionManifest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Notifies admins when the currently running edition (Full or Lite) has
 * a newer version available, or is critically outdated (below minSupportedVersion).
 */
@Singleton
public class CurrentEditionUpdateNotification implements LoginNotification {

    private static final String COOLDOWN_PREFIX = "edition_update:";

    private final JavaPlugin plugin;
    private final OrionEdition edition;
    private final UpdateCheckService updateCheckService;
    private final UpdateNotificationCooldownService cooldownService;
    private final UpdateNotificationSnoozeService snoozeService;

    @Inject
    public CurrentEditionUpdateNotification(JavaPlugin plugin,
                                            OrionEdition edition,
                                            UpdateCheckService updateCheckService,
                                            UpdateNotificationCooldownService cooldownService,
                                            UpdateNotificationSnoozeService snoozeService) {
        this.plugin = plugin;
        this.edition = edition;
        this.updateCheckService = updateCheckService;
        this.cooldownService = cooldownService;
        this.snoozeService = snoozeService;
    }

    @Override
    public boolean shouldNotify(Player player) {
        if (!player.hasPermission(OrionPermissions.ADMIN_NOTICE)) {
            return false;
        }
        if (snoozeService.isSnoozed(player.getUniqueId())) {
            return false;
        }

        UpdateStatus status = resolveStatus();
        if (status == UpdateStatus.UP_TO_DATE) {
            return false;
        }

        return cooldownService.shouldShow(cooldownKey(player, status));
    }

    @Override
    public Component render(Player player) {
        EditionManifest manifest = getEditionManifest();
        UpdateStatus status = resolveStatus();
        if (manifest == null || status == UpdateStatus.UP_TO_DATE) {
            return Component.empty();
        }

        String currentVersion = plugin.getDescription().getVersion();
        Component body = renderBody(manifest, status, currentVersion);

        return body.append(Component.text("  "))
                .append(buildButtons());
    }

    private Component renderBody(EditionManifest manifest, UpdateStatus status, String currentVersion) {
        if (status == UpdateStatus.CRITICALLY_OUTDATED) {
            return renderCriticalBody(manifest, currentVersion);
        }
        return renderOutdatedBody(manifest, currentVersion);
    }

    private Component renderCriticalBody(EditionManifest manifest, String currentVersion) {
        String template = NotificationTextFormatter.orDefault(
                manifest.getCriticalHeadline(),
                "<red><bold>Critical issue in your version: <reason>. Update immediately.</bold></red>"
        );

        return MiniMessage.miniMessage().deserialize(
                template,
                Placeholder.component("reason", Component.text(manifest.getCriticalReason())),
                Placeholder.component("current", Component.text(currentVersion, NamedTextColor.YELLOW)),
                Placeholder.component("min", Component.text(manifest.getMinSupportedVersion(), NamedTextColor.GREEN))
        );
    }

    private Component renderOutdatedBody(EditionManifest manifest, String currentVersion) {
        String template = NotificationTextFormatter.orDefault(
                manifest.getUpdateHeadline(),
                "1b1v update available: <current> → <latest>"
        );

        return MiniMessage.miniMessage().deserialize(
                template,
                Placeholder.component("current", Component.text(currentVersion, NamedTextColor.YELLOW)),
                Placeholder.component("latest", Component.text(manifest.getLatestVersion(), NamedTextColor.GREEN))
        );
    }

    private UpdateStatus resolveStatus() {
        EditionManifest manifest = getEditionManifest();
        if (manifest == null) {
            return UpdateStatus.UP_TO_DATE;
        }

        String currentVersion = plugin.getDescription().getVersion();

        if (isCriticallyOutdated(manifest, currentVersion)) {
            return UpdateStatus.CRITICALLY_OUTDATED;
        }
        if (isOutdated(manifest, currentVersion)) {
            return UpdateStatus.OUTDATED;
        }

        return UpdateStatus.UP_TO_DATE;
    }

    private boolean isCriticallyOutdated(EditionManifest manifest, String currentVersion) {
        boolean hasMinimum = manifest.getMinSupportedVersion() != null;
        boolean hasReason = manifest.getCriticalReason() != null && !manifest.getCriticalReason().isBlank();

        return hasMinimum
                && hasReason
                && SemanticVersionComparator.isBelowMinimum(currentVersion, manifest.getMinSupportedVersion());
    }

    private boolean isOutdated(EditionManifest manifest, String currentVersion) {
        return manifest.getLatestVersion() != null
                && SemanticVersionComparator.isNewer(currentVersion, manifest.getLatestVersion());
    }

    private String cooldownKey(Player player, UpdateStatus status) {
        EditionManifest manifest = getEditionManifest();
        String versionMarker = resolveVersionMarker(manifest, status);
        return COOLDOWN_PREFIX + player.getUniqueId() + ":" + versionMarker;
    }

    private String resolveVersionMarker(EditionManifest manifest, UpdateStatus status) {
        if (status == UpdateStatus.CRITICALLY_OUTDATED) {
            return manifest.getMinSupportedVersion() + "|" + manifest.getCriticalReason();
        }
        return manifest.getLatestVersion();
    }

    @Override
    public void onShown(Player player) {
        String key = cooldownKey(player, resolveStatus());
        String prunePrefix = COOLDOWN_PREFIX + player.getUniqueId() + ":";
        cooldownService.markShownAndPruneStale(key, prunePrefix);
    }

    @Override
    public int getPriority() {
        return NotificationPriority.EDITION_UPDATE.value();
    }

    private Component buildButtons() {
        VersionManifest manifest = updateCheckService.getCachedManifest();
        if (manifest == null) {
            return buildSkipButton();
        }

        String currentVersion = plugin.getDescription().getVersion();

        Component liteButton = buildEditionButton(
                manifest.getLite(), "Lite",
                edition == OrionEdition.LITE, currentVersion);
        Component proButton = buildEditionButton(
                manifest.getFull(), "Pro",
                edition == OrionEdition.FULL, currentVersion);

        Component buttons = Component.empty();
        boolean hasLite = !liteButton.equals(Component.empty());
        boolean hasPro = !proButton.equals(Component.empty());

        if (hasLite) {
            buttons = buttons.append(liteButton);
        }
        if (hasPro) {
            if (hasLite) {
                buttons = buttons.append(Component.text("  "));
            }
            buttons = buttons.append(proButton);
        }
        if (hasLite || hasPro) {
            buttons = buttons.append(Component.text("  "));
        }
        return buttons.append(buildSkipButton());
    }

    private Component buildEditionButton(EditionManifest editionManifest, String name,
                                         boolean isCurrent, String currentVersion) {
        if (editionManifest == null
                || editionManifest.getLatestVersion() == null
                || !SemanticVersionComparator.isNewer(currentVersion, editionManifest.getLatestVersion())) {
            return Component.empty();
        }

        String url = resolveUrl(editionManifest);
        if (url == null) {
            return Component.empty();
        }

        String label = "[ Update " + name + " ]";

        if (isCurrent) {
            return Component.text(label, NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Click to download " + name + " update",
                                    NamedTextColor.WHITE)));
        }

        return NotificationButtons.linkButton(label, url,
                "Click to download " + name + " update");
    }

    private String resolveUrl(EditionManifest editionManifest) {
        if (editionManifest.getUpdateUrl() != null && !editionManifest.getUpdateUrl().isBlank()) {
            return editionManifest.getUpdateUrl();
        }
        return null;
    }

    private Component buildSkipButton() {
        return NotificationButtons.commandButton(
                "[ Do not show ]", "/1b1v notice",
                "Click to silence update notices");
    }

    private EditionManifest getEditionManifest() {
        VersionManifest manifest = updateCheckService.getCachedManifest();
        if (manifest == null) {
            return null;
        }
        return edition == OrionEdition.LITE ? manifest.getLite() : manifest.getFull();
    }

    private enum UpdateStatus {
        UP_TO_DATE, OUTDATED, CRITICALLY_OUTDATED
    }
}