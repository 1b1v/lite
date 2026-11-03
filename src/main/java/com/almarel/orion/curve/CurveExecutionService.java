package com.almarel.orion.curve;

import com.almarel.orion.framework.concurrent.TaskRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Singleton
public class CurveExecutionService {

    private final MessageManager messageManager;
    private final Plugin plugin;
    private final TaskRunner taskRunner;

    @Inject
    public CurveExecutionService(MessageManager messageManager,
                                 Plugin plugin,
                                 TaskRunner taskRunner) {
        this.messageManager = messageManager;
        this.plugin = plugin;
        this.taskRunner = taskRunner;
    }

    /**
     * Единый оркестратор асинхронной генерации.
     * Изолирует шаблонный код (Async, Try-Catch, Main Thread messaging).
     */
    public CompletableFuture<Boolean> executeAsync(Player player, String errorContext, Supplier<Integer> task) {
        return taskRunner.supplyAsync(() -> {
            try {
                int count = task.get();
                notifySuccess(player, count);
                return true;
            } catch (Exception e) {
                handleFailure(player, errorContext, e);
                return false;
            }
        });
    }

    /**
     * Оповещение игрока об успешном завершении генерации в основном потоке сервера.
     */
    private void notifySuccess(Player player, int count) {
        taskRunner.runSyncIfOnline(player, p -> messageManager.send(p, OrionMessage.REGION_COMPLETE, count));
    }

    /**
     * Логирование серверной ошибки и отправка сообщения игроку в основном потоке.
     */
    private void handleFailure(Player player, String errorContext, Exception e) {
        plugin.getLogger().severe(errorContext + " Gen Error: " + e.getMessage());
        taskRunner.runSyncIfOnline(player, p -> messageManager.send(p, OrionMessage.REGION_GEN_ERROR, e.getMessage()));
    }
}