package com.almarel.orion.brush.presentation.parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * Паттерн Builder для безопасной генерации строковых команд FAWE/Arceon из DTO.
 * Защищает от потери пробелов, null-алиасов и дублирования логики конкатенации.
 */
public class FaweCommandBuilder {
    private String label;
    private String alias;
    private final List<String> arguments = new ArrayList<>();

    public FaweCommandBuilder(String label) {
        this.label = (label != null && !label.trim().isEmpty()) ? label.trim() : "/br";
    }

    public FaweCommandBuilder withAlias(String alias) {
        if (alias != null && !alias.trim().isEmpty()) {
            this.alias = alias.trim();
        }
        return this;
    }

    public FaweCommandBuilder withArgs(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.trim().isEmpty()) {
                    this.arguments.add(arg.trim());
                }
            }
        }
        return this;
    }

    public FaweCommandBuilder withArg(Object arg) {
        if (arg != null) {
            String str = String.valueOf(arg).trim();
            if (!str.isEmpty()) {
                this.arguments.add(str);
            }
        }
        return this;
    }

    public FaweCommandBuilder withFlag(String flag, boolean condition) {
        if (condition && flag != null && !flag.trim().isEmpty()) {
            this.arguments.add(flag.trim());
        }
        return this;
    }

    public String build() {
        StringBuilder builder = new StringBuilder(label);
        if (alias != null) {
            builder.append(" ").append(alias);
        }
        for (String arg : arguments) {
            builder.append(" ").append(arg);
        }
        return builder.toString();
    }
}