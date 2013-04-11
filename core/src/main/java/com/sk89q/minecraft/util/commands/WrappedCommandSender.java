package com.sk89q.minecraft.util.commands;

public interface WrappedCommandSender {
    String getName();

    void sendMessage(String message);

    void sendMessage(String[] messages);

    boolean hasPermission(String permission);

    Type getType();

    Object getCommandSender();

    public static enum Type {
        CONSOLE,
        PLAYER,
        BLOCK,
        UNKNOWN
    }
}
