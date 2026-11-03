package com.almarel.orion.framework.command.subcommands;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class WikiSubcommand implements OrionSubCommand {

    private static final String WIKI_URL = "https://1b1v.github.io/admin/";
    private static final String DISCORD_URL = "https://discord.gg/QtQuz9ZX";

    private static final Component ONLY_PLAYERS_MESSAGE = MiniMessage.miniMessage().deserialize(
            "<dark_gray>[<aqua>1b1v<dark_gray>] <red>This command is only for players."
    );

    private static final List<String> ALIASES = List.of("docs", "link", "links", "discord", "suggestion");

    private final Component linksMessage;

    @Inject
    public WikiSubcommand(JavaPlugin plugin) {
        String version = plugin.getDescription().getVersion();

        this.linksMessage = MiniMessage.miniMessage().deserialize(
                "<dark_gray>[<aqua>1b1v<dark_gray>] <white>v" + version + "\n" +
                        "<gray>▪ <white>Documentation: <aqua><underlined><click:open_url:'" + WIKI_URL + "'><hover:show_text:'<gray>Click to open in browser'>" + WIKI_URL + "</hover></click></underlined>\n" +
                        "<gray>▪ <white>Discord: <aqua><underlined><click:open_url:'" + DISCORD_URL + "'><hover:show_text:'<gray>Click to open in browser'>" + DISCORD_URL + "</hover></click></underlined>"
        );
    }

    @Override
    public String getName() {
        return "wiki";
    }

    @Override
    public List<String> getAliases() {
        return ALIASES;
    }

    /**
     * CORE_USE уже гарантирован централизованным floor-gate в
     * OrionCommand.hasBasicAccess() — повторная проверка здесь избыточна.
     */
    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ONLY_PLAYERS_MESSAGE);
            return true;
        }

        player.sendMessage(linksMessage);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}