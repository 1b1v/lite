package com.almarel.orion.framework.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.almarel.orion.Orion;
import com.almarel.orion.framework.data.MainConfig;
import org.bukkit.plugin.java.JavaPlugin;
import com.almarel.orion.framework.data.OrionEdition;
import java.io.InputStream;
import java.util.Properties;

public class OrionCoreModule extends AbstractModule {

    private final Orion plugin;

    public OrionCoreModule(Orion plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Биндим инстансы ядра Bukkit
        bind(Orion.class).toInstance(plugin);
        bind(JavaPlugin.class).toInstance(plugin);
        bind(org.bukkit.plugin.Plugin.class).toInstance(plugin);

    }

    @Provides
    @Singleton
    public MainConfig provideMainConfig() {
        MainConfig config = new MainConfig(plugin);
        config.load();
        return config;
    }

    @Provides
    @Singleton
    public OrionEdition provideOrionEdition(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("1b1v-edition.properties")) {
            if (in == null) {
                plugin.getLogger().warning("1b1v-edition.properties not found in JAR. Defaulting to FULL edition.");
                return OrionEdition.FULL;
            }

            Properties props = new Properties();
            props.load(in);

            String value = props.getProperty("edition", "full").trim().toLowerCase();
            return "lite".equals(value) ? OrionEdition.LITE : OrionEdition.FULL;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to read 1b1v-edition.properties: " + e.getMessage());
            return OrionEdition.FULL;
        }
    }

    @Provides
    @Singleton
    public WorldEdit provideWorldEdit() {
        return WorldEdit.getInstance();
    }

    @Provides
    @Singleton
    public WorldEditPlugin provideWorldEditPlugin(org.bukkit.plugin.Plugin plugin) {
        return (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
    }

}