package com.almarel.orion.brush.state;

import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import com.almarel.orion.framework.storage.snapshot.LightweightSessionExtractor;
import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;

import java.util.HashMap;

/**
 * Маппинг домена "Кисти" между OrionPlayerSession и OrionSnapshot.
 * Выделен из SessionSnapshotMapper (SRP) — см. LightSessionExtractor.
 * Core-компонент, НЕ является ORION_CUT-опциональным модулем.
 */
@Singleton
public class BrushSnapshotExtractor implements LightweightSessionExtractor {

    @Inject
    public BrushSnapshotExtractor() {
    }

    @Override
    public void extract(OrionPlayerSession session, OrionSnapshot snapshot) {
        snapshot.brushesData = new HashMap<>();
        for (BrushState state : session.getComponent(SessionBrushData.class).getBrushesData().values()) {
            if (state.isPersistent()) {
                snapshot.brushesData.put(state.getStorageKey(), state);
            }
        }
    }

    @Override
    public void restore(OrionPlayerSession session, OrionSnapshot snapshot) {
        if (snapshot.brushesData == null || snapshot.brushesData.isEmpty()) return;

        SessionBrushData bd = session.getComponent(SessionBrushData.class);
        for (BrushState state : snapshot.brushesData.values()) {
            if (state != null && state.getTool() != null && state.getTool() != Material.AIR) {
                bd.putBrushSettings(state.getTool(), state);
            }
        }
    }
}