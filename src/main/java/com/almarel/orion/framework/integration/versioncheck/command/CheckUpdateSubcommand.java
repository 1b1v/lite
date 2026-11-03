package com.almarel.orion.framework.integration.versioncheck.command;

import com.almarel.orion.framework.integration.versioncheck.UpdateNotificationCooldownService;
import com.almarel.orion.framework.integration.versioncheck.UpdateNotificationSnoozeService;
import com.google.inject.Inject;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.notification.NotificationDigestService;
import com.almarel.orion.framework.integration.versioncheck.UpdateCheckService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Форсирует немедленное обновление кэша manifest.json, минуя встроенный
 * 12-часовой цикл фоновой проверки. Диагностический инструмент для админов
 * при тестировании изменений в manifest.json на GitHub.
 */
public class CheckUpdateSubcommand implements OrionSubCommand {

    private final UpdateCheckService updateCheckService;
    private final NotificationDigestService digestService;
    private final UpdateNotificationCooldownService cooldownService;
    private final UpdateNotificationSnoozeService snoozeService;

    @Inject
    public CheckUpdateSubcommand(UpdateCheckService updateCheckService,
                                 NotificationDigestService digestService,
                                 UpdateNotificationCooldownService cooldownService,
                                 UpdateNotificationSnoozeService snoozeService) {
        this.updateCheckService = updateCheckService;
        this.digestService = digestService;
        this.cooldownService = cooldownService;
        this.snoozeService = snoozeService;
    }

    @Override
    public String getName() {
        return "checkupdate";
    }

    @Override
    public String getPermission() {
        return OrionPermissions.ADMIN_NOTICE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            UUID playerId = player.getUniqueId();
            snoozeService.clearSnooze(playerId);
            cooldownService.clearForPlayer(playerId);
        }

        sender.sendMessage("§8[§b1b1v§8] §7Checking for updates...");
        updateCheckService.forceRefresh(success -> {
            if (!success) {
                sender.sendMessage("§8[§b1b1v§8] §cFailed to fetch manifest — check console for details.");
                return;
            }
            sender.sendMessage("§8[§b1b1v§8] §aManifest refreshed.");
            if (sender instanceof Player player) {
                digestService.checkNow(player);
            }
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("checkup", "cu");
    }
}