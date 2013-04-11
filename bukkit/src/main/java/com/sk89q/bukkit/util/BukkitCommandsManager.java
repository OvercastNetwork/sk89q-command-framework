package com.sk89q.bukkit.util;

import org.bukkit.command.CommandSender;

import com.sk89q.minecraft.util.commands.CommandsManager;

public class BukkitCommandsManager extends CommandsManager<CommandSender> {
    @Override
    public boolean hasPermission(CommandSender player, String perm) {
        return player.hasPermission(perm);
    }
}
