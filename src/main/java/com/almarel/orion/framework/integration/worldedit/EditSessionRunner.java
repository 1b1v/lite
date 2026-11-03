package com.almarel.orion.framework.integration.worldedit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Singleton
public class EditSessionRunner {

    private final WorldEdit worldEdit;

    @Inject
    public EditSessionRunner(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public <T> T withEditSession(LocalSession session, com.sk89q.worldedit.entity.Player wePlayer, EditTask<T> task) throws Exception {
        try (EditSession editSession = session.createEditSession(wePlayer)) {
            T result = task.run(editSession);
            session.remember(editSession);
            return result;
        }
    }

    /**
     * Async-версия: создаёт EditSession в вызывающем потоке (сохраняя контракт WorldEdit API
     * из оригинального кода), а саму работу + remember() + close() выполняет в указанном executor'е.
     */
    public <T> CompletableFuture<T> withEditSessionAsync(LocalSession session,
                                                         com.sk89q.worldedit.entity.Player wePlayer,
                                                         Executor executor,
                                                         EditTask<T> task) {
        EditSession editSession = session.createEditSession(wePlayer);

        return CompletableFuture.supplyAsync(() -> {
            try (editSession) {
                T result = task.run(editSession);
                session.remember(editSession);
                return result;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @FunctionalInterface
    public interface EditTask<T> {
        T run(EditSession editSession) throws Exception;
    }
}