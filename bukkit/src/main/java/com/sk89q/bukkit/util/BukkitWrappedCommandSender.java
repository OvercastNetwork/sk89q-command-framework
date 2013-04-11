package com.sk89q.bukkit.util;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.WrappedCommandSender;

public class BukkitWrappedCommandSender implements WrappedCommandSender {
    public BukkitWrappedCommandSender(CommandSender wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String getName() {
        return this.getName();
    }

    @Override
    public void sendMessage(String message) {
        this.wrapped.sendMessage(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        this.wrapped.sendMessage(messages);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.wrapped.hasPermission(permission);
    }

    @Override
    public Type getType() {
        if (this.wrapped instanceof ConsoleCommandSender) {
            return Type.CONSOLE;
        } else if (this.wrapped instanceof Player) {
            return Type.PLAYER;
        } else if (this.wrapped instanceof BlockCommandSender) {
            return Type.BLOCK;
        } else {
            return Type.UNKNOWN;
        }
    }

    @Override
    public Object getCommandSender() {
        return this.wrapped;
    }

    private final CommandSender wrapped;
}
