package com.almarel.orion;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.NoCapablePlatformException;
import com.almarel.orion.framework.loader.DependencyManager; // [NEW] Импорт загрузчика
import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Injector;

public final class Orion extends JavaPlugin {

    private Bootstrap context;
    private Injector injector;

    @Override
    public void onEnable() {
        // [STEP 1] Загрузка библиотек (Runtime Dependency Loading)
        try {
            new DependencyManager(this).load();
        } catch (Exception e) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!isEnabled()) {
            return;
        }

        // [STEP 2] Проверка зависимостей сервера (FAWE)
        if (getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            getLogger().severe("FastAsyncWorldEdit not found! Disabling 1b1v.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // [STEP 2.5] ЗАЩИТА ОТ /RELOAD CONFIRM
        try {
            WorldEdit.getInstance().getConfiguration();
        } catch (NoCapablePlatformException e) {
            getLogger().severe("=============================================");
            getLogger().severe(" DETECTED SERVER RELOAD (/reload confirm)");
            getLogger().severe(" FastAsyncWorldEdit does NOT support hot-reloading.");
            getLogger().severe(" 1b1v will disable itself to prevent data corruption.");
            getLogger().severe(" Please use a full server RESTART to apply changes.");
            getLogger().severe("=============================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // [DEBUG] Netty Sniffer (Раскомментировать при необходимости жесткой отладки пакетов)
        // getServer().getPluginManager().registerEvents(new com.almarel.orion.debug.PacketSnifferListener(), this);

        saveDefaultConfig();

        // [STEP 3] Инициализация контекста
        this.context = new Bootstrap(this);
        this.context.init();
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.shutdown();
        }
    }
}