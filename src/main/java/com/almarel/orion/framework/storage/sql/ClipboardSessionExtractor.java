package com.almarel.orion.framework.storage.sql;

import com.almarel.orion.framework.session.data.OrionPlayerSession;
import com.almarel.orion.framework.storage.snapshot.OrionSnapshot;

/**
 * Аналог LightSessionExtractor для ДОМЕНА БУФЕРА ОБМЕНА — сохраняется
 * отдельным, более редким циклом (см. SessionPersistenceOrchestrator,
 * CLIPBOARD_SAVE, dirty-checked). Разделение интерфейсов отражает разные
 * политики персистентности, а не разную механику маппинга.
 */
public interface ClipboardSessionExtractor {

    void extract(OrionPlayerSession session, OrionSnapshot snapshot);

    void restore(OrionPlayerSession session, OrionSnapshot snapshot);

    /**
     * Вызывается ОДИН РАЗ после restore() ВСЕХ ClipboardSessionExtractor
     * в батче (см. SessionSnapshotMapper.applySnapshot). Нужен для
     * пост-обработки, которая обязана видеть финальное состояние ПОСЛЕ
     * всех частичных restore() — например, сброс dirty-флага, который
     * могли заново взвести чужие мутации dirty-tracking коллекций.
     * По умолчанию no-op — большинству экстракторов это не требуется.
     */
    default void afterRestoreBatch(OrionPlayerSession session) {
    }
}