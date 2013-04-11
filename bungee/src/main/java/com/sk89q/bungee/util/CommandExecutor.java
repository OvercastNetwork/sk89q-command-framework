package com.sk89q.bungee.util;


public interface CommandExecutor<T> {
    void onCommand(T sender, String commandName, String[] args);
}
