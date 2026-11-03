package com.almarel.orion.math;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;

/**
 * Сервис для ранней валидации математических выражений.
 * Чистая логика (Stateless). Не содержит ссылок на Bukkit или GUI.
 */
@Singleton
public class MathValidationService {

    @Inject
    public MathValidationService() {
        // Чистый DI конструктор без мусорных зависимостей
    }

    /**
     * Проверяет формулу на синтаксические ошибки.
     * @param rawFormula Введенная игроком формула
     * @throws IllegalArgumentException Если формула некорректна.
     */
    public void validateGenFormula(String rawFormula) throws IllegalArgumentException {
        FormulaUtils.ParsingResult result = FormulaUtils.prepare(rawFormula);
        String expressionToCheck = result.expression;

        try {
            Expression.compile(expressionToCheck, "x", "y", "z", "r");
        } catch (ExpressionException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Syntax error: " + e.getMessage());
        }
    }
}