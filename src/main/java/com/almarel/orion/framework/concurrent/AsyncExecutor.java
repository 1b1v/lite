package com.almarel.orion.framework.concurrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Singleton
public class AsyncExecutor {

    private static final long GRACEFUL_TIMEOUT_SECONDS = 10;
    private static final long FORCED_TIMEOUT_SECONDS = 5;

    private final ExecutorService executor;
    private final Logger logger;

    @Inject
    public AsyncExecutor(Logger logger) {
        this.logger = logger;
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(1, cores - 2);
        this.executor = Executors.newFixedThreadPool(poolSize, new OrionThreadFactory());
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Вызывать в главном классе плагина (onDisable) для корректного завершения.
     * Graceful two-phase shutdown: сперва даём текущим задачам (в т.ч. файловому I/O —
     * persistClipboard/performDelete) корректно дописаться/докрыть NIO-каналы, и только
     * при таймауте эскалируем до принудительного shutdownNow().
     * Без этого резкий interrupt() посреди записи .schem может оставить частично
     * записанный файл (ClosedByInterruptException на NIO-канале).
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(GRACEFUL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warning("1b1v: AsyncExecutor did not finish gracefully in "
                        + GRACEFUL_TIMEOUT_SECONDS + "s, forcing shutdownNow()");
                executor.shutdownNow();
                if (!executor.awaitTermination(FORCED_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    logger.severe("1b1v: AsyncExecutor did not terminate even after shutdownNow()");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class OrionThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "1b1v-Async-" + counter.incrementAndGet());
            thread.setDaemon(true); // Не блокирует выключение сервера
            thread.setPriority(Thread.NORM_PRIORITY - 1); // Чуть ниже основного потока
            return thread;
        }
    }
}