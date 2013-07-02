package com.sk89q.bungee.util;

import net.md_5.bungee.api.CommandSender;

import com.sk89q.minecraft.util.commands.CommandsManager;

import java.lang.reflect.Method;

public class BungeeCommandsManager extends CommandsManager<CommandSender> {
    @Override
    public void checkSender(CommandSender sender, Method method) {
        //  Sender will never be the console
    }

    @Override
    public boolean hasPermission(CommandSender player, String perm) {
        return player.hasPermission(perm);
    }
}
