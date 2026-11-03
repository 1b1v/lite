package com.almarel.orion.framework.command.infrastructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Помощники для реализации business-команд (AbstractCommand-наследников):
 * фильтрация готовых списков автодополнения и подсчёт символов в уже
 * распарсенных аргументах (не связано с лексикой сырой командной строки —
 * см. CommandLineFormat для этого).
 */
public class CommandArgsSupport {

    public static List<String> filterSuggestions(List<String> candidates, String token) {
        if (candidates == null || token == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        String lowerToken = token.toLowerCase();

        for (String cand : candidates) {
            if (cand.toLowerCase().startsWith(lowerToken)) {
                result.add(cand);
            }
        }
        return result;
    }

    public static int countOccurrences(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }
}