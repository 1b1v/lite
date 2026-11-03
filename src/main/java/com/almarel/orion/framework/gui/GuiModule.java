package com.almarel.orion.framework.gui;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.gui.configurationlegacy.*;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import org.bukkit.event.Listener;

public class GuiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GuiStateManager.class).in(Scopes.SINGLETON);
        bind(GuiWindowManager.class).in(Scopes.SINGLETON);
        bind(GuiRegistry.class).asEagerSingleton();

        // Регистрация слушателей модуля GUI
        Multibinder<Listener> listenerBinder =
                Multibinder.newSetBinder(binder(), org.bukkit.event.Listener.class);
        listenerBinder.addBinding().to(GuiWindowManager.class);
        listenerBinder.addBinding().to(GuiStateManager.class);
    }

    @Provides
    @Singleton
    public ConfigurationGuiStore provideConfigStore(MessageManager messageManager) {
        return new ConfigurationGuiStore(messageManager.getConfig());
    }

    @Provides
    @Singleton
    public CommonGuiConfig provideCommonConfig(ConfigurationGuiStore store) {
        return store.getCommon();
    }

    @Provides
    @Singleton
    public BrushListGuiConfig provideBrushListConfig(ConfigurationGuiStore store) {
        return store.getBrushList();
    }

    @Provides
    @Singleton
    public CommandGuiConfig provideCommandConfig(ConfigurationGuiStore store) {
        return store.getCommand();
    }

    @Provides
    @Singleton
    public ConstructGuiConfig provideConstructConfig(ConfigurationGuiStore store) {
        return store.getConstruct();
    }

    @Provides
    @Singleton
    public ClipboardGuiConfig provideClipboardConfig(ConfigurationGuiStore store) {
        return store.getClipboard(); // Если геттер называется иначе, например getClipboardConfig(), исправь это
    }

    @Provides
    @Singleton
    public ProceduralGuiConfig provideProceduralConfig(ConfigurationGuiStore store) {
        return store.getPro(); // Если геттер называется иначе, например getProceduralConfig(), исправь на правильный!
    }
}