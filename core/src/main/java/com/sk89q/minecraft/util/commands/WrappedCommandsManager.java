package com.sk89q.minecraft.util.commands;

public class WrappedCommandsManager extends CommandsManager<WrappedCommandSender> {
    @Override
    public boolean hasPermission(WrappedCommandSender player, String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public WrappedCommandSender.Type getType(WrappedCommandSender player) {
        return player.getType();
    }
}
