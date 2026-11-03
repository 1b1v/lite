package com.almarel.orion;

import com.almarel.orion.framework.compat.InventoryViewCompat;
import com.almarel.orion.framework.concurrent.TaskRunner;
import com.almarel.orion.framework.storage.DatabaseMaintenanceScheduler;
import com.almarel.orion.recolor.shape.BlockShapeClassifier;
import com.almarel.orion.mapping.visual.BlockVisualProfileRepository;
import com.almarel.orion.recolor.core.RecolorExclusionRegistry;
import com.almarel.orion.recolor.RecolorModule;
import com.google.inject.*;
import com.google.inject.Module;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.almarel.orion.brush.BrushModule;
import com.almarel.orion.curve.CurveModule;
import com.almarel.orion.framework.integration.analytics.BStatsService;
import com.almarel.orion.framework.integration.versioncheck.UpdateCheckService;
import com.almarel.orion.generation.GenerationModule;
import com.almarel.orion.mapping.MappingModule;
import com.almarel.orion.region.RegionModule;
import com.almarel.orion.selection.SelectionModule;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.CommandProcessorRegistry;
import com.almarel.orion.framework.command.OrionCommand;
import com.almarel.orion.framework.command.infrastructure.BukkitCommandRegistrar;
import com.almarel.orion.brush.state.BrushStateRegistry;
import com.almarel.orion.framework.di.OrionCoreModule;
import com.almarel.orion.framework.di.OrionFeatureModule;
import com.almarel.orion.framework.gui.GuiModule;
import com.almarel.orion.framework.storage.OrionStorageModule;
import com.almarel.orion.brush.OrionBrushRegistry;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.mask.FaweMaskHook;
import com.almarel.orion.pattern.FawePatternHook;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.framework.session.OrionSessionManager;
import com.almarel.orion.framework.session.SessionPersistenceOrchestrator;
import com.almarel.orion.framework.storage.sql.DatabaseService;
import com.almarel.orion.framework.gui.common.OrionMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import com.almarel.orion.framework.loader.FeatureModuleScanner;
import java.util.ArrayList;
import java.util.List;

import com.almarel.orion.brush.api.BrushFeature;
import java.util.Comparator;
import java.util.Set;


public class Bootstrap {

    private final Orion plugin;
    private Injector injector;
    private TaskRunner taskRunner;

    public Bootstrap(Orion plugin) {
        this.plugin = plugin;
    }

    public void init() {
        initializeCoreInfrastructure();
        loadFeaturesAndPresets();
        registerCommands();
        registerListenersAndStartServices();
    }

    private void initializeCoreInfrastructure() {
        // 1. Динамический поиск модулей фич (Сканер сам создает инстансы вложенных $Module классов)
        List<Module> dynamicModules = FeatureModuleScanner.scan(
                plugin,
                plugin.getLogger(),
                "com.almarel.orion.brush.custom"
        );

        // 2. Объединяем базовые модули ядра и динамические модули фич
        List<Module> allModules = new ArrayList<>(dynamicModules);
        allModules.add(new OrionCoreModule(plugin));
        allModules.add(new OrionStorageModule());
        allModules.add(new OrionFeatureModule());
        allModules.add(new MappingModule());
        allModules.add(new GuiModule());
        allModules.add(new SelectionModule());
        allModules.add(new RegionModule());
        allModules.add(new GenerationModule());
        allModules.add(new CurveModule());
        allModules.add(new RecolorModule());
        allModules.add(new BrushModule());

        // 3. Создаем единый граф зависимостей
        this.injector = Guice.createInjector(allModules);

        this.taskRunner = injector.getInstance(TaskRunner.class);

        injector.getInstance(DatabaseService.class).initialize();
    }

    private void loadFeaturesAndPresets() {
        // 2.5 Динамическая регистрация Фичей
        OrionBrushRegistry orionBrushRegistry = injector.getInstance(OrionBrushRegistry.class);
        BrushStateRegistry settingsRegistry = injector.getInstance(BrushStateRegistry.class);

        // Извлекаем множество всех фич, которые зарегистрировали себя через Multibinder
        Set<BrushFeature<?>> features = injector.getInstance(
                Key.get(new TypeLiteral<Set<BrushFeature<?>>>() {})
        );

        // Сортируем по getOrder() для соблюдения приоритетов
        List<BrushFeature<?>> sortedFeatures = features.stream()
                .sorted(Comparator.comparingInt(BrushFeature::getOrder))
                .toList();

        for (BrushFeature<?> feature : sortedFeatures) {
            orionBrushRegistry.register(feature);
            settingsRegistry.register(feature.getId(), feature.getSettingsClass());
        }

        // [CRITICAL] Explicitly load all registries before checking for updates






        try { injector.getInstance(FawePatternHook.class).registerAll(); }
        catch (Exception e) { plugin.getLogger().warning("Failed to inject 1b1v parsers: " + e.getMessage()); }

        try { injector.getInstance(FaweMaskHook.class).registerAll(); }
        catch (Exception e) { plugin.getLogger().warning("Failed to inject 1b1v masks: " + e.getMessage()); }

    }

    private void registerCommands() {
        // [CRITICAL] СНАЧАЛА заполняем реестр процессоров!
        CommandProcessorRegistry commandRegistry = injector.getInstance(CommandProcessorRegistry.class);

        // Динамически получаем ВСЕ команды из Guice Multibinder
        Set<AbstractCommand> commands = injector.getInstance(
                Key.get(new TypeLiteral<Set<AbstractCommand>>() {})
        );

        for (AbstractCommand cmd : commands) {
            commandRegistry.register(cmd);
        }

        // [CRITICAL] ПОСЛЕ того как реестр заполнен, инжектим "пустышки" в Bukkit для подсветки!
        try { injector.getInstance(BukkitCommandRegistrar.class).registerAll(); }
        catch (Exception e) { plugin.getLogger().severe("Error registering 1b1v commands: " + e.getMessage()); }

        OrionCommand orionCommandInstance = injector.getInstance(OrionCommand.class);
        org.bukkit.command.PluginCommand mainCommand = plugin.getCommand("1b1v");
        if (mainCommand != null) {
            mainCommand.setExecutor(orionCommandInstance);
            mainCommand.setTabCompleter(orionCommandInstance);
        } else {
            plugin.getLogger().severe("Failed to register command '/1b1v'. Is it defined in plugin.yml?");
        }
    }

    private void registerListenersAndStartServices() {
        PluginManager pm = plugin.getServer().getPluginManager();
        WorldEditPlugin wePlugin = (WorldEditPlugin) pm.getPlugin("WorldEdit");

        // Запрашиваем ВСЕХ слушателей из Guice
        Set<Listener> listeners = injector.getInstance(
                Key.get(new TypeLiteral<Set<Listener>>() {})
        );

        for (Listener listener : listeners) {
            // Защита: не регистрируем Tab-листенеры, если нет WorldEdit
            String name = listener.getClass().getSimpleName();
            if ((name.equals("AliasTabListener") || name.equals("GlobalCommandTabListener")) && wePlugin == null) {
                continue;
            }
            pm.registerEvents(listener, plugin);
        }

        // Запуск фоновых процессов
        injector.getInstance(DatabaseMaintenanceScheduler.class).start();
        injector.getInstance(SessionPersistenceOrchestrator.class).start();
        injector.getInstance(BStatsService.class).start();
        injector.getInstance(UpdateCheckService.class).start();

    }

    public void shutdown() {
        if (injector == null) {
            return;
        }

        // ЭТАП 1: Изоляция. Закрываем GUI и снимаем листенеры, чтобы никто не мутировал данные.
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory topInventory = InventoryViewCompat.getTopInventory(player);
                if (topInventory != null && topInventory.getHolder() instanceof OrionMenuHolder) {
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to safely close 1b1v GUIs: " + e.getMessage());
        }

        try {
            HandlerList.unregisterAll(plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unregister listeners: " + e.getMessage());
        }

        // ЭТАП 2: Синхронный сброс данных. Мы СНАЧАЛА сохраняем всё в БД/Диск.
        // Инфраструктура (потоки, БД) ещё жива!
        try {
            injector.getInstance(SessionPersistenceOrchestrator.class).shutdownSync();





        } catch (ProvisionException ignored) {
            plugin.getLogger().info("Data flush skipped for uninitialized managers.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error during Graceful Data Flush: " + e.getMessage());
            plugin.getLogger().severe("Error: " + e.getMessage());
        }

        // ЭТАП 3: Остановка инфраструктуры I/O и закрытие пулов.
        // Теперь, когда данные в безопасности, глушим I/O и БД (с WAL checkpoint).
        try {
            injector.getInstance(OrionSessionManager.class).shutdown();

            injector.getInstance(DatabaseMaintenanceScheduler.class).shutdown();

            injector.getInstance(DatabaseService.class).shutdown();
        } catch (Exception e) {
            plugin.getLogger().severe("Error during I/O Shutdown: " + e.getMessage());
        }

        // ЭТАП 4: Финальная зачистка. Очищаем RAM и убиваем все таски/потоки.
        try {
            injector.getInstance(BrushStateManager.class).shutdown();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear brush states: " + e.getMessage());
        }

        try {
            Bukkit.getScheduler().cancelTasks(plugin);
            injector.getInstance(AsyncExecutor.class).shutdown();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cancel tasks or shutdown async executor: " + e.getMessage());
        }
    }

    // точка входа для внешних плагинов. В Orion -  no usage
    public Injector getInjector() {
        return injector;
    }
}