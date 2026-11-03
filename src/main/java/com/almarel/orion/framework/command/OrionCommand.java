package com.almarel.orion.framework.command;

import com.google.inject.Inject;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.almarel.orion.framework.gui.GuiWindowManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OrionCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messageManager;
    private final GuiWindowManager guiWindowManager;

    // Динамический реестр подкоманд
    private final Map<String, OrionSubCommand> subCommandMap = new HashMap<>();
    private final Set<OrionSubCommand> uniqueSubCommands = new HashSet<>();

    private static final String MSG_PLAYER_ONLY = "command.player-only";
    private static final String MSG_NO_PERMISSION = "command.no-permission";

    @Inject
    public OrionCommand(MessageManager messageManager,
                        GuiWindowManager guiWindowManager,
                        Set<OrionSubCommand> subCommands) {

        this.messageManager = messageManager;
        this.guiWindowManager = guiWindowManager;

        // Автоматическая регистрация ВСЕХ подкоманд, которые собрал Guice
        for (OrionSubCommand subCmd : subCommands) {
            registerSubcommand(subCmd);
        }
    }

    private void registerSubcommand(OrionSubCommand subCmd) {
        subCommandMap.put(subCmd.getName().toLowerCase(), subCmd);
        for (String alias : subCmd.getAliases()) {
            subCommandMap.put(alias.toLowerCase(), subCmd);
        }
        uniqueSubCommands.add(subCmd);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasBasicAccess(sender)) {
            messageManager.sendMessage(sender, MSG_NO_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            return handleDefaultAction(sender);
        }

        return handleSubCommandRouting(sender, args);
    }

    /**
     * Floor-gate: контролирует доступ к дереву команд /1b1v ЦЕЛИКОМ.
     * НЕ связан с доступом к конкретным доменам GUI (Clipboard/Macro/Brush) —
     * они гейтятся отдельно, в точке реального действия (см. GuiWindowManager),
     * что закрывает обход через клики по навбару, а не только через команду.
     */
    private boolean hasBasicAccess(CommandSender sender) {
        return !(sender instanceof Player player) || player.hasPermission(OrionPermissions.CORE_USE);
    }

    private boolean handleDefaultAction(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, MSG_PLAYER_ONLY);
            return true;
        }

        guiWindowManager.openLastMenu(player);
        return true;
    }

    private boolean handleSubCommandRouting(CommandSender sender, String[] args) {
        String subCmdLabel = args[0].toLowerCase(Locale.ROOT);
        OrionSubCommand subCmd = subCommandMap.get(subCmdLabel);

        if (subCmd == null) {
            if (sender instanceof Player player) {
                sendHelpMessage(player);
            }
            return true;
        }

        if (!hasSubCommandAccess(sender, subCmd)) {
            messageManager.sendMessage(sender, MSG_NO_PERMISSION);
            return true;
        }

        return subCmd.execute(sender, args);
    }

    private boolean hasSubCommandAccess(CommandSender sender, OrionSubCommand subCmd) {
        String perm = subCmd.getPermission();
        return perm == null || sender.hasPermission(perm);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return completeSubCommandNames(sender, args[0]);
        }
        if (args.length >= 2) {
            return delegateSubCommandTab(sender, args);
        }
        return Collections.emptyList();
    }

    /**
     * ВАЖНО: алиасы (getAliases()) намеренно НЕ включаются в подсказки —
     * в tab-complete показывается только canonical name (getName()).
     * Это осознанное решение (не забывать при рефакторинге): цель — не
     * забивать чат игрока десятком синонимов одной команды. Алиасы при
     * этом полностью рабочие для маршрутизации, см. registerSubcommand().
     */
    private List<String> completeSubCommandNames(CommandSender sender, String currentArg) {
        List<String> allowed = new ArrayList<>();
        for (OrionSubCommand subCmd : uniqueSubCommands) {
            if (subCmd.isHidden()) {
                continue;
            }
            if (hasSubCommandAccess(sender, subCmd)) {
                allowed.add(subCmd.getName());
            }
        }

        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(currentArg, allowed, completions);
        Collections.sort(completions);
        return completions;
    }

    private List<String> delegateSubCommandTab(CommandSender sender, String[] args) {
        OrionSubCommand subCmd = subCommandMap.get(args[0].toLowerCase(Locale.ROOT));
        if (subCmd != null && hasSubCommandAccess(sender, subCmd)) {
            return subCmd.tabComplete(sender, args);
        }
        return Collections.emptyList();
    }

    private void sendHelpMessage(Player player) {
        messageManager.sendMessage(player, "command.help.header");
        messageManager.sendMessage(player, "command.help.open");


        messageManager.sendMessage(player, "command.help.brush");
        messageManager.sendMessage(player, "command.help.brushes");

    }
}