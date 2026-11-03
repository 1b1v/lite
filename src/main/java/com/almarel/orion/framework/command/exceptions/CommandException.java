package com.almarel.orion.framework.command.exceptions; // Или framework.message, как удобнее

import com.almarel.orion.framework.message.OrionMessage;

public class CommandException extends RuntimeException {
    private final OrionMessage message;
    private final Object[] args;

    public CommandException(OrionMessage message, Object... args) {
        super(message.getKey());
        this.message = message;
        this.args = args;
    }

    public OrionMessage getOrionMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }
}