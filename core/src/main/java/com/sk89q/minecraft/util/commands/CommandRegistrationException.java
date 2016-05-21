package com.sk89q.minecraft.util.commands;

/**
 * A problem registering commands
 */
public class CommandRegistrationException extends RuntimeException {

    public CommandRegistrationException() {
    }

    public CommandRegistrationException(String message) {
        super(message);
    }

    public CommandRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandRegistrationException(Throwable cause) {
        super(cause);
    }
}
