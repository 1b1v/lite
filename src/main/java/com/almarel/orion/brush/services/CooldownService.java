package com.almarel.orion.brush.services;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Сервис управления ограничениями по времени (Cooldowns) и блокировками (Locks).
 * Вынесен из BrushStateManager для соблюдения SRP.
 */
public class CooldownService implements Listener {

    // Runtime-данные (Очищаются при рестарте или выходе игрока)
    // Key: UUID Игрока -> Value: Время окончания кулдауна (millis)
    private final Map<UUID, Long> magicCopyCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pasteCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> macroCooldowns = new ConcurrentHashMap<>();

    // Блокировки (Locks)
    private final Set<UUID> magicCopyLocks = Collections.synchronizedSet(new HashSet<>());

    // Задержка анти-спама макросов для защиты TPS
    private static final long MACRO_COOLDOWN_MS = 150L;

    @Inject
    public CooldownService() {
    }

    // [FIX] Устранение утечки памяти (очистка по выходу)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearForPlayer(event.getPlayer().getUniqueId());
    }

    public void clearForPlayer(UUID uuid) {
        magicCopyCooldowns.remove(uuid);
        pasteCooldowns.remove(uuid);
        macroCooldowns.remove(uuid);
        magicCopyLocks.remove(uuid);
    }

    public void clearAll() { // Method no usage
        magicCopyCooldowns.clear();
        pasteCooldowns.clear();
        macroCooldowns.clear();
        magicCopyLocks.clear();
    }

    // --- Macro Cooldown (Anti-Spam) ---

    public boolean hasCooldown(Player player) {
        return System.currentTimeMillis() - macroCooldowns.getOrDefault(player.getUniqueId(), 0L) < MACRO_COOLDOWN_MS;
    }

    public void setCooldown(Player player) {
        macroCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // --- Paste Cooldown ---

    public boolean isPasteCooldownActive(UUID uuid, long cooldownMillis) {
        return System.currentTimeMillis() - pasteCooldowns.getOrDefault(uuid, 0L) < cooldownMillis;
    }

    public void startPasteCooldown(UUID uuid) {
        pasteCooldowns.put(uuid, System.currentTimeMillis());
    }

    // --- Magic Copy Cooldown ---

    public boolean isMagicCopyCooldownActive(UUID uuid, long cooldownMillis) {
        return System.currentTimeMillis() - magicCopyCooldowns.getOrDefault(uuid, 0L) < cooldownMillis;
    }

    public void startMagicCopyCooldown(UUID uuid) {
        magicCopyCooldowns.put(uuid, System.currentTimeMillis());
    }

    // --- Magic Copy Locks ---

    public boolean isMagicCopyLocked(UUID uuid) {
        return magicCopyLocks.contains(uuid);
    }

    public void lockMagicCopy(UUID uuid) {
        magicCopyLocks.add(uuid);
    }

    public void unlockMagicCopy(UUID uuid) {
        magicCopyLocks.remove(uuid);
    }
}