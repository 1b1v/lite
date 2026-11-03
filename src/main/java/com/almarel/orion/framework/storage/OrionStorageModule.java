package com.almarel.orion.framework.storage;

import com.almarel.orion.framework.session.JoinListener;
import com.almarel.orion.framework.session.JoinRestoreHook;
import com.almarel.orion.framework.session.OrionSessionManager;
import com.almarel.orion.framework.session.QuitCleanupHook;
import com.almarel.orion.framework.session.SessionLifecycleListener;
import com.almarel.orion.framework.storage.snapshot.LightweightSessionExtractor;
import com.almarel.orion.framework.storage.sql.*;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.bukkit.event.Listener;

public class OrionStorageModule extends AbstractModule {

    @Override
    protected void configure() {
        // Системные сервисы хранилища и сессий (Framework)
        bind(DatabaseService.class).in(Scopes.SINGLETON);
        bind(JoinListener.class).in(Scopes.SINGLETON);
        bind(SessionLifecycleListener.class).in(Scopes.SINGLETON);
        bind(OrionSessionManager.class).in(Scopes.SINGLETON);
        bind(StorageManager.class).in(Scopes.SINGLETON);

        bind(StorageBackend.class).to(SQLiteStorage.class).in(Scopes.SINGLETON);
        bind(DatabaseMaintenanceScheduler.class).in(Scopes.SINGLETON);

        // Регистрация слушателей событий в Bukkit
        Multibinder<Listener> listenerBinder = Multibinder.newSetBinder(binder(), Listener.class);
        listenerBinder.addBinding().to(JoinListener.class);
        listenerBinder.addBinding().to(SessionLifecycleListener.class);

        // Хуки жизненного цикла игрока (Join / Quit)
        Multibinder.newSetBinder(binder(), JoinRestoreHook.class);
        Multibinder.newSetBinder(binder(), QuitCleanupHook.class);

        // Секции снимков БД (SQL Sections)
        Multibinder.newSetBinder(binder(), LightSnapshotSection.class);
        Multibinder.newSetBinder(binder(), ClipboardSnapshotSection.class);

        // Экстракторы данных сессий (Session Extractors)
        Multibinder.newSetBinder(binder(), LightweightSessionExtractor.class);
        Multibinder.newSetBinder(binder(), ClipboardSessionExtractor.class);
    }
}