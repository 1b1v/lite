package com.almarel.orion.framework.storage.snapshot;

import com.almarel.orion.framework.storage.sql.ClipboardSessionExtractor;
import com.almarel.orion.framework.session.data.OrionPlayerSession;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;

/**
 * Тонкий оркестратор маппинга OrionPlayerSession <-> OrionSnapshot.
 * НЕ содержит доменной логики — вся она делегирована в LightSessionExtractor/
 * ClipboardSessionExtractor реализации (BrushSnapshotExtractor,
 * ConstructSnapshotExtractor, MacroSnapshotExtractor, HistorySnapshotExtractor,
 * SpeedSnapshotExtractor, ClipboardStateSnapshotExtractor,
 * BrushBufferSnapshotExtractor), каждая из которых регистрируется в МОДУЛЕ
 * СВОЕГО ДОМЕНА через Multibinder.
 *
 * Добавление новой персистентной фичи в будущем = 1 новый класс-экстрактор
 * + 1 строка Multibinder в модуле этой фичи, БЕЗ правок этого файла.
 *
 * [AI CONTEXT] Три публичных метода создания снапшота соответствуют трем
 * циклам персистентности (см. StorageBackend):
 * - createSnapshot: полный снимок (AFK-выгрузка / Shutdown).
 * - createLightSnapshot: только light-домены (частый цикл).
 * - createClipboardSnapshot: только буфер обмена (редкий цикл, вызывается
 *   ТОЛЬКО если SessionClipboardData.isDirty() == true).
 */
@Singleton
public class SessionSnapshotMapper {

    private final Set<LightweightSessionExtractor> lightExtractors;
    private final Set<ClipboardSessionExtractor> clipboardExtractors;

    @Inject
    public SessionSnapshotMapper(Set<LightweightSessionExtractor> lightExtractors,
                                 Set<ClipboardSessionExtractor> clipboardExtractors) {
        this.lightExtractors = lightExtractors;
        this.clipboardExtractors = clipboardExtractors;
    }

    // =========================================
    // CREATION (SESSION -> SNAPSHOT)
    // =========================================

    public OrionSnapshot createSnapshot(OrionPlayerSession session) {
        OrionSnapshot snapshot = new OrionSnapshot();
        extractLight(snapshot, session);
        extractClipboard(snapshot, session);
        return snapshot;
    }

    public OrionSnapshot createLightSnapshot(OrionPlayerSession session) {
        OrionSnapshot snapshot = new OrionSnapshot();
        extractLight(snapshot, session);
        return snapshot;
    }

    public OrionSnapshot createClipboardSnapshot(OrionPlayerSession session) {
        OrionSnapshot snapshot = new OrionSnapshot();
        extractClipboard(snapshot, session);
        return snapshot;
    }

    private void extractLight(OrionSnapshot snapshot, OrionPlayerSession session) {
        for (LightweightSessionExtractor extractor : lightExtractors) {
            extractor.extract(session, snapshot);
        }
    }

    private void extractClipboard(OrionSnapshot snapshot, OrionPlayerSession session) {
        for (ClipboardSessionExtractor extractor : clipboardExtractors) {
            extractor.extract(session, snapshot);
        }
    }

    // =========================================
    // APPLICATION (SNAPSHOT -> SESSION)
    // =========================================

    public void applySnapshot(OrionPlayerSession session, OrionSnapshot snapshot) {
        for (ClipboardSessionExtractor extractor : clipboardExtractors) {
            extractor.restore(session, snapshot);
        }
        // Пост-обработка батча (напр. сброс dirty-флага) — ТОЛЬКО после
        // того, как ВСЕ clipboard-экстракторы завершили restore().
        for (ClipboardSessionExtractor extractor : clipboardExtractors) {
            extractor.afterRestoreBatch(session);
        }
        for (LightweightSessionExtractor extractor : lightExtractors) {
            extractor.restore(session, snapshot);
        }
    }
}