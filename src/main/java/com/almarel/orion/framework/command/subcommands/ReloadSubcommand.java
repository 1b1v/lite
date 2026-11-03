package com.almarel.orion.framework.command.subcommands;

import com.google.inject.Inject;
import com.almarel.orion.framework.data.MainConfig;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore; // [NEW] Прямой импорт хранилища конфигов
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadSubcommand implements OrionSubCommand {

    private final MainConfig mainConfig;
    private final MessageManager messageManager;
    private final ConfigurationGuiStore configStore; // [NEW]

    @Inject
    public ReloadSubcommand(MainConfig mainConfig,
                            MessageManager messageManager,
                            ConfigurationGuiStore configStore) {
        this.mainConfig = mainConfig;
        this.messageManager = messageManager;
        this.configStore = configStore;
    }

    @Override
    public String getName() { return "reload"; }

    @Override
    public List<String> getAliases() { return Collections.emptyList(); }

    @Override
    public String getPermission() { return OrionPermissions.ADMIN_USE; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Перезагружаем основные конфиги
        mainConfig.load();
        messageManager.load();

        // Перезагружаем конфиги GUI напрямую!
        if (configStore != null) {
            configStore.reload(messageManager.getConfig());
        }

        messageManager.send(sender, OrionMessage.CMD_RELOAD_SUCCESS);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}