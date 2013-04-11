package com.sk89q.bungee.util;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class CommandWrapper extends Command {
    public CommandWrapper(CommandExecutor<CommandSender> executor, String commandName, String... aliases) {
        super(commandName, null, aliases);
        this.executor = executor;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.executor.onCommand(sender, this.getName(), args);
    }

    private final CommandExecutor<CommandSender> executor;
}
