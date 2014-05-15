package com.sk89q.bukkit.util;

import com.sk89q.minecraft.util.commands.WrappedCommandSender;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import com.sk89q.minecraft.util.commands.CommandsManager;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class BukkitCommandsManager extends CommandsManager<CommandSender> {
    @Override
    public boolean hasPermission(CommandSender player, String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public WrappedCommandSender.Type getType(CommandSender player) {
        if (player instanceof Player) {
            return WrappedCommandSender.Type.PLAYER;
        } else if (player instanceof ConsoleCommandSender) {
            return WrappedCommandSender.Type.CONSOLE;
        } else if (player instanceof Block) {
            return WrappedCommandSender.Type.BLOCK;
        } else {
            return WrappedCommandSender.Type.UNKNOWN;
        }
    }
}
