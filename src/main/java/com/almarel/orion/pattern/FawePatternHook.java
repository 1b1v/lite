package com.almarel.orion.pattern;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.almarel.orion.framework.concurrent.AsyncExecutor;
import com.almarel.orion.mapping.block.BlockVariantMapper;
import com.almarel.orion.pattern.noise.RidgedMultiPatternParser;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.pattern.flora.FlowersPatternParser;
import com.almarel.orion.pattern.flora.GrassPatternParser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Singleton
public class FawePatternHook {

    private final List<PendingParser<Pattern>> overridingPatterns = new ArrayList<>();
    private final List<PendingParser<Pattern>> standardPatterns = new ArrayList<>();

    private final WorldEdit worldEdit;
    private final MessageManager messageManager;
    private final BlockVariantMapper blockVariantMapper;
    private final AsyncExecutor asyncExecutor;

    @Inject
    public FawePatternHook(WorldEdit worldEdit,
                           MessageManager messageManager,
                           BlockVariantMapper blockVariantMapper,
                           AsyncExecutor asyncExecutor) {
        this.worldEdit = worldEdit;
        this.messageManager = messageManager;
        this.blockVariantMapper = blockVariantMapper;
        this.asyncExecutor = asyncExecutor;

        // --- 1. OVERRIDING PATTERNS (Требуют Reflection для перехвата нативных) ---

        // --- 2. STANDARD PATTERNS (Новые паттерны, безопасная регистрация API) ---
        standardPattern(() -> true, RidgedMultiPatternParser::new);


        standardPattern(() -> true, FlowersPatternParser::new);




        standardPattern(() -> true, (we) -> new GrassPatternParser(we, this.asyncExecutor));

    }

    private void overridePattern(BooleanSupplier isEnabled, Function<WorldEdit, InputParser<Pattern>> constructor) {
        overridingPatterns.add(new PendingParser<>(isEnabled, constructor));
    }

    private void standardPattern(BooleanSupplier isEnabled, Function<WorldEdit, InputParser<Pattern>> constructor) {
        standardPatterns.add(new PendingParser<>(isEnabled, constructor));
    }

    public void registerAll() {
        PatternFactory factory = worldEdit.getPatternFactory();

        for (PendingParser<Pattern> pending : standardPatterns) {
            if (pending.isEnabled()) {
                factory.register(pending.construct(worldEdit));
            }
        }

        for (PendingParser<Pattern> pending : overridingPatterns) {
            if (pending.isEnabled()) {
                injectParserAtStart(factory, pending.construct(worldEdit));
            }
        }

        overridingPatterns.clear();
        standardPatterns.clear();
    }

    private boolean injectParserAtStart(Object factory, Object myParser) {
        try {
            Field parsersField = AbstractFactory.class.getDeclaredField("parsers");
            parsersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> parsers = (List<Object>) parsersField.get(factory);
            parsers.add(0, myParser);
            return true;
        } catch (Exception e) {
            System.err.println("[1b1v] Reflection error when registering a pattern! " + e.getMessage());
            return false;
        }
    }

    private static class PendingParser<T> {
        private final BooleanSupplier isEnabled;
        private final Function<WorldEdit, InputParser<T>> constructor;

        public PendingParser(BooleanSupplier isEnabled, Function<WorldEdit, InputParser<T>> constructor) {
            this.isEnabled = isEnabled;
            this.constructor = constructor;
        }
        public boolean isEnabled() { return isEnabled.getAsBoolean(); }
        public InputParser<T> construct(WorldEdit worldEdit) { return constructor.apply(worldEdit); }
    }
}

/**
 * [ORION ARCHITECTURE NOTE - PISTON TAB-COMPLETE BUG]
 * При создании кастомных парсеров (наследников RichParser) ВАЖНО соблюдать
 * строгую валидацию в методе parseFromInput(String[] arguments, ParserContext context).
 *
 * Как работает движок FAWE (Piston):
 * Piston запрашивает подсказки (getSuggestions) ТОЛЬКО в том случае, если текущий ввод
 * считается синтаксически неверным или незавершенным.
 *
 * Баг "Ложного Успеха" (Silent Success):
 * Если при вводе незавершенной строки (например, "#flowers[bl" или "#flowers[")
 * парсер "проглатывает" пустоту/опечатку, применяет значения по умолчанию и
 * УСПЕШНО возвращает Pattern без выбрасывания ошибки — Piston считает аргумент
 * полностью дописанным и ОТКЛЮЧАЕТ автодополнение.
 *
 * ПРАВИЛО:
 * Метод parseFromInput ОБЯЗАН выбрасывать InputParseException, если аргумент
 * пустой (arguments[0].isEmpty()) или содержит недописанный текст.
 * Именно эта ошибка "будит" Piston и заставляет его вызвать getSuggestions().
 */


