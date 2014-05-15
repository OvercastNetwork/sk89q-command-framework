package com.sk89q.bungee.util;

import com.sk89q.minecraft.util.commands.WrappedCommandSender;
import net.md_5.bungee.api.CommandSender;

import com.sk89q.minecraft.util.commands.CommandsManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeCommandsManager extends CommandsManager<CommandSender> {
    @Override
    public boolean hasPermission(CommandSender player, String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public WrappedCommandSender.Type getType(CommandSender player) {
        if (player instanceof ProxiedPlayer) {
            return WrappedCommandSender.Type.PLAYER;
        }
        else {
            return WrappedCommandSender.Type.UNKNOWN;
        }
    }
}
