package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;

public interface ReloadableConfig {
    void updateSource(FileConfiguration newSource);
}