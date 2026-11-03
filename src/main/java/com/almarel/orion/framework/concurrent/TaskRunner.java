package com.almarel.orion.framework.concurrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Singleton
public class TaskRunner {

    private final JavaPlugin plugin;
    private final AsyncExecutor asyncExecutor;

    @Inject
    public TaskRunner(JavaPlugin plugin, AsyncExecutor asyncExecutor) {
        this.plugin = plugin;
        this.asyncExecutor = asyncExecutor;
    }

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, asyncExecutor.getExecutor())
                .exceptionally(ex -> {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "1b1v: Unhandled exception in TaskRunner.runAsync task", ex);
                    return null;
                });
    }

    public <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor.getExecutor())
                .exceptionally(ex -> {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "1b1v: Unhandled exception in TaskRunner.supplyAsync task", ex);
                    throw new java.util.concurrent.CompletionException(ex);
                });
    }

    public BukkitTask runSyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runSyncIfOnline(UUID uuid, Consumer<Player> task) {
        runSync(() -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                task.accept(p);
            }
        });
    }

    public BukkitTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    public BukkitTask runAsyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    public BukkitTask runSyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public void runSyncIfOnline(Player playerRef, Consumer<Player> task) {
        runSyncIfOnline(playerRef.getUniqueId(), task);
    }
}