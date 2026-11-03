package com.almarel.orion.brush;

import com.almarel.orion.brush.fawe.gateway.*;
import com.almarel.orion.framework.session.JoinRestoreHook;
import com.almarel.orion.framework.session.QuitCleanupHook;
import com.almarel.orion.framework.storage.snapshot.LightweightSessionExtractor;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.Orion;
import com.almarel.orion.brush.api.BrushFeature;
import com.almarel.orion.brush.handler.BrushHandler;
import com.almarel.orion.brush.input.InteractionService;
import com.almarel.orion.brush.presentation.command.handler.BrushSubCommandHandler;
import com.almarel.orion.brush.presentation.gui.selector.BrushSelectorView;
import com.almarel.orion.brush.presentation.gui.settings.BrushSettingsRouter;
import com.almarel.orion.brush.state.*;
import com.almarel.orion.brush.fawe.data.*;
import com.almarel.orion.framework.storage.sql.LightSnapshotSection;
import com.almarel.orion.mapping.block.BlockTypeCacheService;
import com.almarel.orion.brush.fawe.ExternalStateFactory;
import com.almarel.orion.brush.fawe.NativeCaptureService;
import com.almarel.orion.brush.input.BrushInputListener;
import com.almarel.orion.brush.input.ScrollController;
import com.almarel.orion.brush.input.ToolCaptureListener;
import com.almarel.orion.brush.presentation.gui.settings.providers.StandardBrushWidgetProvider;
import com.almarel.orion.brush.presentation.parsing.BrushArgsParser;
import com.almarel.orion.brush.presentation.command.*;
import com.almarel.orion.brush.presentation.gui.settings.providers.ScrollToggleWidgetProvider;
import com.almarel.orion.brush.services.*;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.brush.presentation.gui.api.WidgetProvider;
import org.bukkit.event.Listener;

public class BrushModule extends AbstractModule {
    @Override
    protected void configure() {
        // --- ПЕРСИСТЕНТНОСТЬ СНАПШОТА ---
        Multibinder<LightSnapshotSection> snapshotSectionBinder = Multibinder.newSetBinder(binder(), LightSnapshotSection.class);
        snapshotSectionBinder.addBinding().to(BrushSnapshotGateway.class);

        Multibinder<LightweightSessionExtractor> sessionExtractorBinder = Multibinder.newSetBinder(binder(), LightweightSessionExtractor.class);
        sessionExtractorBinder.addBinding().to(BrushSnapshotExtractor.class);

        // --- JOIN/QUIT HOOKS ---
        Multibinder<JoinRestoreHook> joinHookBinder = Multibinder.newSetBinder(binder(), JoinRestoreHook.class);
        joinHookBinder.addBinding().to(BrushSyncJoinHook.class);

        Multibinder<QuitCleanupHook> quitHookBinder = Multibinder.newSetBinder(binder(), QuitCleanupHook.class);
        quitHookBinder.addBinding().to(BrushSyncQuitHook.class);
        quitHookBinder.addBinding().to(FaweOptionQuitHook.class);
        quitHookBinder.addBinding().to(BrushStateQuitHook.class);

        // --- БАЗОВЫЕ СЕРВИСЫ ---
        bind(BrushArgsParser.class).in(Scopes.SINGLETON);
        bind(BrushManager.class).in(Scopes.SINGLETON);
        bind(BrushSetupService.class).in(Scopes.SINGLETON);
        bind(BrushStateManager.class).in(Scopes.SINGLETON);
        bind(BrushSyncService.class).in(Scopes.SINGLETON);
        bind(BrushQueryService.class).in(Scopes.SINGLETON);
        bind(CooldownService.class).in(Scopes.SINGLETON);
        bind(InteractionService.class).in(Scopes.SINGLETON);
        bind(BlockTypeCacheService.class).in(Scopes.SINGLETON);

        // --- ВВОД И СЛУШАТЕЛИ (Явная инициализация как Singleton) ---
        bind(BrushInputListener.class).in(Scopes.SINGLETON);
        bind(ScrollController.class).in(Scopes.SINGLETON);
        bind(ToolCaptureListener.class).in(Scopes.SINGLETON);

        // --- FAWE ИНТЕГРАЦИЯ ---
        bind(NativeCaptureService.class).in(Scopes.SINGLETON);
        bind(ExternalStateFactory.class).in(Scopes.SINGLETON); // Фабрика состояний
        bind(FaweSessionGateway.class).in(Scopes.SINGLETON);
        bind(FaweStateReader.class).in(Scopes.SINGLETON);
        bind(FaweOptionWriter.class).in(Scopes.SINGLETON);
        bind(FaweBrushInjector.class).in(Scopes.SINGLETON);

        // --- РЕГИСТРАЦИЯ КОМАНД ---
        Multibinder<AbstractCommand> commandBinder = Multibinder.newSetBinder(binder(), AbstractCommand.class);
        commandBinder.addBinding().to(BrushCommand.class);
        commandBinder.addBinding().to(SizeCommand.class);
        commandBinder.addBinding().to(ToolCommand.class);
        commandBinder.addBinding().to(UnbindCommand.class);

        // --- РЕГИСТРАЦИЯ САБКОМАНД ---
        Multibinder<OrionSubCommand> subCommandBinder = Multibinder.newSetBinder(binder(), OrionSubCommand.class);
        subCommandBinder.addBinding().to(BrushSelectorSubcommand.class);
        subCommandBinder.addBinding().to(BrushSettingsSubcommand.class);

        // Явное объявление коллекции обработчиков команд кистей,
        // чтобы Guice не падал, даже если все кастомные кисти вырезаны (коллекция будет пустой).
        Multibinder.newSetBinder(binder(), BrushSubCommandHandler.class);

        // --- РЕГИСТРАЦИЯ СЛУШАТЕЛЕЙ (Добавление в общий пул для Bootstrap) ---
        Multibinder<Listener> listenerBinder = Multibinder.newSetBinder(binder(), Listener.class);
        // Мы НЕ пишем здесь .in(Scopes.SINGLETON), потому что мы уже забиндили их как синглтоны выше.
        // Guice просто возьмет готовые экземпляры и положит их в корзину.
        listenerBinder.addBinding().to(BrushInputListener.class);
        listenerBinder.addBinding().to(ScrollController.class);
        listenerBinder.addBinding().to(ToolCaptureListener.class);
        listenerBinder.addBinding().to(CooldownService.class);

        // Регистрация GUI Провайдеров Кистей
        Multibinder<WidgetProvider> widgetBinder =
                Multibinder.newSetBinder(binder(), WidgetProvider.class);
        widgetBinder.addBinding().to(ScrollToggleWidgetProvider.class);
        widgetBinder.addBinding().to(StandardBrushWidgetProvider.class);

        // Gui
        bind(BrushSettingsRouter.class).asEagerSingleton();
        bind(BrushSelectorView.class).asEagerSingleton();

        // --- РЕГИСТРАЦИЯ ТИПОВ КИСТЕЙ ДЛЯ GSON (Multibinder) ---
        Multibinder<BrushStateRegistry.Registration> stateBinder = Multibinder.newSetBinder(binder(), BrushStateRegistry.Registration.class);

        // Явное объявление коллекции обработчиков (Handlers) для чистого DI
        Multibinder.newSetBinder(binder(), BrushHandler.class);
        // [ORION_CUT: FaweConstructBrushState]
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("fawe", FaweConstructBrushState.class));
        // [ORION_CUT_END]
        // [ORION_CUT: ArceonConstructBrushState]
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("arceon", ArceonConstructBrushState.class));
        // [ORION_CUT_END]
        // [ORION_CUT: ArceonBoulderState]
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("arceon_boulder", ArceonBoulderState.class));
        // [ORION_CUT_END]
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("tool", FaweToolState.class));
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("arceon_basic", ArceonBrushState.class));
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("fawe_basic", FaweBrushState.class));
        // ДОБАВИТЬ:
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("fawe_repl", FaweReplToolState.class));
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("fawe_flood", FaweFloodToolState.class));
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("fawe_stacker", FaweStackerToolState.class));
        stateBinder.addBinding().toInstance(new BrushStateRegistry.Registration("ezedits", EzEditsBrushState.class));

        // Явное объявление коллекции фич, чтобы избежать краша при вырезании всех кистей.
        // Используем TypeLiteral, так как OrionBrushFeature содержит дженерик <?>.
        Multibinder.newSetBinder(binder(), new com.google.inject.TypeLiteral<BrushFeature<?>>() {});
    }

    @Provides
    @Singleton
    public Gson provideGson(BrushStateRegistry registry, Orion plugin) { // [FIX] Добавлен plugin
        return BrushStateGson.create(registry, plugin);
    }
}