package com.almarel.orion.framework.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Iterator;

/**
 * КАРАНТИННАЯ ЗОНА: версионный дрейф Bukkit/Paper API.
 * <p>
 * Этот класс изолирует обходные пути, нужные из-за того, что Paper/Mojang
 * периодически переименовывают/перемещают константы между релизами
 * (пример: {@code Attribute.GENERIC_ATTACK_DAMAGE} -> {@code Attribute.ATTACK_DAMAGE} в 1.21.3+).
 * <p>
 * Правило: всё, что ловит {@link Throwable} (а не {@link Exception}) из-за риска
 * {@link NoSuchFieldError}/{@link NoSuchMethodError} при рассинхроне версий API,
 * живёт ТОЛЬКО здесь. Не копировать такие try-catch в бизнес-классы.
 */
public final class AttributeCompatSupport {

    private static volatile Attribute cachedDummyAttribute;
    private static volatile boolean resolved = false;

    private AttributeCompatSupport() {}

    /**
     * Навешивает фиктивный {@link AttributeModifier} на любой существующий
     * в данной версии API {@link Attribute}, чтобы клиент признал изменение
     * атрибутов и корректно скрыл их по флагу {@link ItemFlag#HIDE_ATTRIBUTES}.
     * <p>
     * Нам не важно, какой именно атрибут — важен сам факт модификации.
     * Поэтому мы берём первый попавшийся из {@link Registry#ATTRIBUTE},
     * вместо того чтобы хардкодить конкретную константу, которая может
     * быть переименована/удалена в другой версии ядра.
     */
    public static void attachHideAttributesHack(ItemMeta meta) {
        if (meta == null) return;

        try {
            Attribute dummy = resolveDummyAttribute();
            if (dummy != null) {
                NamespacedKey key = NamespacedKey.minecraft("dummy_modifier");
                AttributeModifier modifier = new AttributeModifier(key, 0.0, AttributeModifier.Operation.ADD_NUMBER);
                meta.addAttributeModifier(dummy, modifier);
            }
        } catch (Throwable ignored) {
            // Критично: ловим Throwable. NoSuchFieldError/NoSuchMethodError — это Error,
            // а не Exception, и падение здесь не должно ломать static-инициализацию
            // классов-потребителей (см. инцидент с NavBarRenderer на 1.21.1).
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    private static Attribute resolveDummyAttribute() {
        if (resolved) return cachedDummyAttribute;
        synchronized (AttributeCompatSupport.class) {
            if (resolved) return cachedDummyAttribute;
            Attribute result = null;
            try {
                Iterator<Attribute> it = Registry.ATTRIBUTE.iterator();
                if (it.hasNext()) result = it.next();
            } catch (Throwable ignored) {
                // Registry.ATTRIBUTE недоступен в этой версии API — хак просто не применится.
            }
            cachedDummyAttribute = result;
            resolved = true;
            return result;
        }
    }
}