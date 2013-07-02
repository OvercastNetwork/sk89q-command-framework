package com.sk89q.bukkit.util;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.Console;

import java.lang.reflect.Method;

public class BukkitCommandsManager extends CommandsManager<CommandSender> {
    @Override
    public boolean hasPermission(CommandSender player, String perm) {
        return player.hasPermission(perm);
    }

    @Override
    public void checkSender(CommandSender sender, Method method) throws CommandException {
        if (!method.isAnnotationPresent(Console.class) && sender instanceof ConsoleCommandSender) {
            throw new CommandException("You must be a player to execute this command.");
        }
    }
}
