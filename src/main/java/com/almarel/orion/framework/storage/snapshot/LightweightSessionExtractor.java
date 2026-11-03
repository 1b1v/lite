package com.almarel.orion.framework.storage.snapshot;

import com.almarel.orion.framework.session.data.OrionPlayerSession;

/**
 * Единица маппинга ОДНОГО персистентного домена между OrionPlayerSession и
 * OrionSnapshot — "light" группа (кисти/конструкты/макросы/история/speed),
 * сохраняемая частым циклом (см. SessionPersistenceOrchestrator, LIGHT_SAVE).
 *
 * Реализации регистрируются через Multibinder<LightSessionExtractor> в
 * МОДУЛЕ СВОЕГО ДОМЕНА (см. HistoryModule, PlayerModule) — НЕ в общем месте.
 * SessionSnapshotMapper не знает о конкретных доменах, только о наборе
 * экстракторов (Open/Closed: новый домен = новый файл, 0 правок в
 * существующих общих классах).
 */
public interface LightweightSessionExtractor {

    void extract(OrionPlayerSession session, OrionSnapshot snapshot);

    void restore(OrionPlayerSession session, OrionSnapshot snapshot);
}