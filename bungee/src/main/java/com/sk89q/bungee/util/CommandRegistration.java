package com.sk89q.bungee.util;

import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Provider;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandsManager;

public class CommandRegistration {
    public CommandRegistration(Plugin plugin, PluginManager pluginManager, CommandsManager<?> commands, CommandExecutor<CommandSender> executor) {
        this.plugin = plugin;
        this.pluginManager = pluginManager;
        this.commands = commands;
        this.executor = executor;
    }

    public boolean register(Class<?> clazz) {
        return register(clazz, null);
    }

    public <T> boolean register(Class<T> clazz, @Nullable Provider<? extends T> provider) {
        return this.registerAll(this.commands.registerMethods(clazz, null, provider));
    }

    public boolean registerAll(List<Command> registered) {
        for(Command command : registered) {
            CommandWrapper wrapper = new CommandWrapper(this.executor, command.aliases()[0], command.aliases());
            this.pluginManager.registerCommand(this.plugin, wrapper);
        }
        return true;
    }

    private final Plugin plugin;
    private final PluginManager pluginManager;
    private final CommandsManager<?> commands;
    private final CommandExecutor<CommandSender> executor;
}
