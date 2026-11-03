package com.almarel.orion.framework.data;

/**
 * Определяет, в каком техническом состоянии собран плагин: FULL (полная кодовая база core)
 * или LITE (урезанная сборка). Публичное соответствие: LITE = 1b1v Lite, FULL = билд,
 * поставляемый подписчикам как 1b1v Pro (см. Orion_Infrastructure, п.0 — открытый вопрос full/pro).
 * Значение вычисляется один раз при старте на основе ресурса 1b1v-edition.properties,
 * который подменяется release_cutter.py при сборке Lite-версии.
 */
public enum OrionEdition {
    FULL,
    LITE
}