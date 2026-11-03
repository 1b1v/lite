package com.almarel.orion.framework.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.Orion;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Registry для сессий игроков.
 * Отвечает ТОЛЬКО за хранение, создание и мониторинг активности (Idle).
 * НЕ работает с файловой системой и не зависит от бизнес-логики фич.
 */
@Singleton
public class OrionSessionManager {

    private final Orion plugin;
    private final SessionLimitService limitService;

    // Lock-Free мапа для потокобезопасного хранения
    private final Map<UUID, SessionHolder> sessions = new ConcurrentHashMap<>();

    @Inject
    public OrionSessionManager(Orion plugin, SessionLimitService limitService) {
        this.plugin = plugin;
        this.limitService = limitService;
    }

    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public OrionPlayerSession getSession(UUID uuid) {
        return getSessionWithResult(uuid).getSession();
    }

    public OrionPlayerSession getSession(Player player) {
        SessionHolder holder = sessions.get(player.getUniqueId());
        if (holder != null) {
            holder.lastKnownName = player.getName();
        }
        return getSession(player.getUniqueId());
    }

    /**
     * Атомарно возвращает сессию, ЕСЛИ она уже существует, иначе null.
     * В отличие от getSession(uuid), НИКОГДА не создаёт новую сессию.
     * [CRITICAL] Использовать именно этот метод во всех периодических/фоновых
     * циклах вместо пары hasSession()+getSession() — та пара не атомарна и
     * подвержена TOCTOU-гонке с параллельными циклами (AFK/Light/Clipboard),
     * которая может привести к тихому созданию пустой сессии и перезаписи
     * реальных данных игрока пустым снапшотом.
     */
    public OrionPlayerSession getSessionIfPresent(UUID uuid) {
        SessionHolder holder = sessions.get(uuid);
        return holder != null ? holder.session : null;
    }

    /**
     * Помечает, что данные сессии были загружены из хранилища (успешно или нет —
     * важен сам факт завершения попытки загрузки). Периодические циклы сохранения
     * обязаны проверять этот флаг перед записью в БД, иначе есть риск затереть
     * реальные данные пустым снапшотом сессии, которая ещё не успела загрузиться.
     */
    public void markDataLoaded(UUID uuid) {
        SessionHolder holder = sessions.get(uuid);
        if (holder != null) {
            holder.dataLoaded = true;
        }
    }

    public boolean isDataLoaded(UUID uuid) {
        SessionHolder holder = sessions.get(uuid);
        return holder != null && holder.dataLoaded;
    }

    public SessionResult getSessionWithResult(UUID uuid) {
        SessionHolder newHolder = new SessionHolder(new OrionPlayerSession());
        SessionHolder existing = sessions.putIfAbsent(uuid, newHolder);

        SessionHolder holder = (existing == null) ? newHolder : existing;
        holder.markActive();

        return new SessionResult(holder.session, existing == null);
    }

    public String getLastKnownName(UUID uuid) {
        SessionHolder holder = sessions.get(uuid);
        return (holder != null && holder.lastKnownName != null) ? holder.lastKnownName : "Unknown_" + uuid.toString().substring(0, 5);
    }

    public Set<UUID> getAllActiveSessions() {
        return new HashSet<>(sessions.keySet());
    }

    public void markIdle(UUID uuid) {
        SessionHolder holder = sessions.get(uuid);
        if (holder != null) {
            holder.markIdle();
        }
    }

    public List<UUID> getIdleSessions(long timeoutMillis) {
        List<UUID> idleUUIDs = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, SessionHolder> entry : sessions.entrySet()) {
            SessionHolder holder = entry.getValue();
            if (holder.isIdle && (now - holder.lastActive > timeoutMillis)) {
                idleUUIDs.add(entry.getKey());
            }
        }
        return idleUUIDs;
    }

    /**
     * Возвращает UUID сессий, которые НЕ помечены idle (т.е. игрок либо
     * онлайн, либо ещё не покинул сервер с момента последнего quit-события).
     * Используется периодическими циклами автосохранения, чтобы не дублировать
     * работу с AFK-выгрузкой (processAfkSessions) и не трогать сессии,
     * ожидающие garbage collection.
     */
    public List<UUID> getOnlineSessions() {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, SessionHolder> entry : sessions.entrySet()) {
            if (!entry.getValue().isIdle) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }

    public void shutdown() {
        sessions.clear();
    }

    public void updateCachedLimits(Player player) {
        limitService.updateSessionLimits(player, getSession(player));
    }

    public static class SessionResult {
        private final OrionPlayerSession session;
        private final boolean isNew;

        public SessionResult(OrionPlayerSession session, boolean isNew) {
            this.session = session;
            this.isNew = isNew;
        }

        public OrionPlayerSession getSession() { return session; }
        public boolean isNew() { return isNew; }
    }

    /**
     * Контейнер сессии.
     * Использует volatile поля для потокобезопасности вместо тяжелых synchronized блоков.
     */
    private static class SessionHolder {
        final OrionPlayerSession session;
        volatile long lastActive;
        volatile boolean isIdle;
        volatile String lastKnownName;
        volatile boolean dataLoaded;

        SessionHolder(OrionPlayerSession session) {
            this.session = session;
            this.markActive();
        }

        void markActive() {
            this.isIdle = false;
            this.lastActive = System.currentTimeMillis();
        }

        void markIdle() {
            this.isIdle = true;
            this.lastActive = System.currentTimeMillis();
        }
    }
}