package com.almarel.orion.brush.state;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.framework.data.MainConfig;
import com.almarel.orion.framework.session.data.OrionPlayerSession;

import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.framework.session.OrionSessionManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;

import java.util.UUID;
import java.util.function.Supplier;
import com.google.inject.Inject;

public class BrushStateManager {

    private final MainConfig mainConfig;
    private final OrionSessionManager orionSessionManager;
    private final FaweIntegrationService faweIntegrationService;
    private final BrushStateRegistry stateRegistry;

    @Inject
    public BrushStateManager(
            MainConfig mainConfig,
            OrionSessionManager orionSessionManager,
            FaweIntegrationService faweIntegrationService,
            BrushStateRegistry stateRegistry) {
        this.mainConfig = mainConfig;
        this.orionSessionManager = orionSessionManager;
        this.faweIntegrationService = faweIntegrationService;
        this.stateRegistry = stateRegistry; // [NEW] Присвоение
    }

    public void shutdown() {
    }

    // --- Helpers ---

    private OrionPlayerSession getValidSession(Player player) {
        if (player == null) return null;
        return orionSessionManager.getSession(player);
    }

    private void validateStateRegistration(BrushState state) {
        if (state == null) return;

        String className = state.getClass().getSimpleName();
        if (stateRegistry.getClass(className) == null) {
            throw new IllegalStateException(
                    "Unregistered brush state: '" + className + "'. Add it to Guice Multibinder to prevent data loss."
            );
        }
    }

    // --- API: Полиморфное получение настроек ---

    public <T extends BrushState> T getBrushSettings(Player player, Material tool, Class<T> clazz) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || tool == null) return null;

        BrushState state = session.getComponent(SessionBrushData.class).getBrushSettings(tool);
        return clazz.isInstance(state) ? clazz.cast(state) : null;
    }

    public <T extends BrushState> T getOrCreateBrushSettings(Player player, Material tool, Class<T> clazz, Supplier<T> factory) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || tool == null) return null;

        BrushState existing = session.getComponent(SessionBrushData.class).getBrushSettings(tool);
        if (clazz.isInstance(existing)) {
            return clazz.cast(existing);
        }

        T newState = factory.get();
        validateStateRegistration(newState); // [FAIL-FAST CHECK]

        // [НОВОЕ] Находим максимальный индекс и задаем новой кисти
        int maxOrder = session.getComponent(SessionBrushData.class).getBrushesData().values().stream()
                .mapToInt(BrushState::getOrderIndex)
                .max().orElse(-1);
        newState.setOrderIndex(maxOrder + 1);

        session.getComponent(SessionBrushData.class).putBrushSettings(tool, newState);
        faweIntegrationService.markSlotPending(player, tool);
        return newState;
    }


    public void putBrushSettings(Player player, Material tool, BrushState state) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || tool == null || state == null) return;

        validateStateRegistration(state); // [FAIL-FAST CHECK]
        session.getComponent(SessionBrushData.class).putBrushSettings(tool, state);
        faweIntegrationService.markSlotPending(player, tool);
    }

    /**
     * Запись DTO, когда соответствующий инструмент УЖЕ лежит в слоте FAWE
     * (нативный бинд, захват команды). Снимает pending и фиксирует квитанцию.
     */
    public void putSyncedBrushSettings(Player player, Material tool, BrushState state) {
        putBrushSettings(player, tool, state);
        faweIntegrationService.recordSlotReceipt(player, tool);
    }

    public boolean removeBrushSettings(Player player, Material tool) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || tool == null) return false;

        if (session.getComponent(SessionBrushData.class).getBrushSettings(tool) != null) {
            session.getComponent(SessionBrushData.class).removeBrushSettings(tool);
            faweIntegrationService.forgetSlotReceipt(player, tool);
            return true;
        }
        return false;
    }

    // --- Resolver (Поиск активной кисти) ---

    public BrushState resolveActiveState(Player player, Material tool) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || tool == null) return null;
        return session.getComponent(SessionBrushData.class).getBrushSettings(tool);
    }

    // --- Unbound Brushes API ---

    public void putUnboundBrush(Player player, BrushState state) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || state == null || state.getId() == null) return;

        validateStateRegistration(state); // [FAIL-FAST CHECK]

        // Находим максимальный индекс среди всех кистей, чтобы новая была в конце списка
        int maxOrder = session.getComponent(SessionBrushData.class).getUnboundBrushes().values().stream()
                .mapToInt(BrushState::getOrderIndex)
                .max().orElse(-1);
        state.setOrderIndex(maxOrder + 1);

        session.getComponent(SessionBrushData.class).putUnboundBrush(state);
    }

    public BrushState getUnboundBrush(Player player, String id) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null || id == null) return null;
        return session.getComponent(SessionBrushData.class).getUnboundBrush(id);
    }

    public List<BrushState> getAllUnboundBrushes(Player player) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null) return new ArrayList<>();

        return new ArrayList<>(session.getComponent(SessionBrushData.class).getUnboundBrushes().values());
    }

    // --- Misc Methods ---

    public List<BrushState> getAllActiveBrushes(Player player) {
        OrionPlayerSession session = getValidSession(player);
        if (session == null) return new ArrayList<>();

        return new ArrayList<>(session.getComponent(SessionBrushData.class).getBrushesData().values());
    }

    public MainConfig getGenericConfig() { return mainConfig; } // no usage

    public void cleanupSession(UUID uuid) {
    }

    public void unbindExternalBrushes(Player p, Material tool) {
        if (tool == null || tool.isAir() || tool.isBlock()) return;
        faweIntegrationService.unbindFaweBrushSilently(p, tool);
    }
}