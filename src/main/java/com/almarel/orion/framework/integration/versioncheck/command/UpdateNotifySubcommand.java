package com.almarel.orion.framework.integration.versioncheck.command;

import com.google.inject.Inject;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.integration.versioncheck.UpdateNotificationCooldownService;
import com.almarel.orion.framework.integration.versioncheck.UpdateNotificationSnoozeService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Toggles update/promotion notifications on or off for 60 days, per player.
 * "/1b1v notice" — running it again while snoozed re-enables immediately.
 */
public class UpdateNotifySubcommand implements OrionSubCommand {

    private final UpdateNotificationSnoozeService snoozeService;
    private final UpdateNotificationCooldownService cooldownService;

    @Inject
    public UpdateNotifySubcommand(UpdateNotificationSnoozeService snoozeService,
                                  UpdateNotificationCooldownService cooldownService) {
        this.snoozeService = snoozeService;
        this.cooldownService = cooldownService;
    }

    @Override
    public String getName() {
        return "notice";
    }

    @Override
    public String getPermission() {
        return OrionPermissions.ADMIN_NOTICE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§8[§b1b1v§8] §cThis command can only be used by a player.");
            return true;
        }

        UUID playerId = player.getUniqueId();
        if (snoozeService.isSnoozed(playerId)) {
            snoozeService.clearSnooze(playerId);
            cooldownService.clearForPlayer(playerId);
            sender.sendMessage("§8[§b1b1v§8] §fUpdate notifications are now §a§lON§f.");
        } else {
            snoozeService.snoozeFor60Days(playerId);
            sender.sendMessage("§8[§b1b1v§8] §fUpdate notifications are now §c§lOFF§f.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}