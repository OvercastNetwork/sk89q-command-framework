/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.minecraft.util.commands;

import com.sk89q.util.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;

/**
 * Manager for handling commands. This allows you to easily process commands,
 * including nested commands, by correctly annotating methods of a class.
 *
 * <p>To use this, it is merely a matter of registering classes containing
 * the commands (as methods with the proper annotations) with the
 * manager. When you want to process a command, use one of the
 * {@code execute} methods. If something is wrong, such as incorrect
 * usage, insufficient permissions, or a missing command altogether, an
 * exception will be raised for upstream handling.</p>
 *
 * <p>Methods of a class to be registered can be static, but if an injector
 * is registered with the class, the instances of the command classes
 * will be created automatically and methods will be called non-statically.</p>
 *
 * <p>To mark a method as a command, use {@link Command}. For nested commands,
 * see {@link NestedCommand}. To handle permissions, use
 * {@link CommandPermissions}.</p>
 *
 * <p>This uses Java reflection extensively, but to reduce the overhead of
 * reflection, command lookups are completely cached on registration. This
 * allows for fast command handling. Method invocation still has to be done
 * with reflection, but this is quite fast in that of itself.</p>
 *
 * @param <T> command sender class
 */
@SuppressWarnings("ProtectedField")
public abstract class CommandsManager<T> {

    protected static final Logger logger =
            Logger.getLogger(CommandsManager.class.getCanonicalName());

    /**
     * Mapping of commands (including aliases) with a description. Root
     * commands are stored under a key of null, whereas child commands are
     * cached under their respective {@link Method}. The child map has
     * the key of the command name (one for each alias) with the
     * method.
     */
    protected Map<Method, Map<String, Method>> commands = new HashMap<Method, Map<String, Method>>();

    /**
     * Used to store the providers associated with a method.
     */
    protected Map<Class, Object> instances = new HashMap<>();
    protected Map<Method, Provider> providers = new HashMap<>();

    /**
     * Mapping of commands (not including aliases) with a description. This
     * is only for top level commands.
     */
    protected Map<String, String> descs = new HashMap<String, String>();

    /**
     * Stores the injector used to getInstance.
     */
    protected Injector injector;

    /**
     * Mapping of commands (not including aliases) with a description. This
     * is only for top level commands.
     */
    protected Map<String, String> helpMessages = new HashMap<String, String>();

    /**
     * Register an class that contains commands (denoted by {@link Command}.
     * If no dependency injector is specified, then the methods of the
     * class will be registered to be called statically. Otherwise, new
     * instances will be created of the command classes and methods will
     * not be called statically.
     *
     * @param cls the class to register
     */
    public void register(Class<?> cls) {
        registerMethods(cls, null);
    }

    /**
     * Register an class that contains commands (denoted by {@link Command}.
     * If no dependency injector is specified, then the methods of the
     * class will be registered to be called statically. Otherwise, new
     * instances will be created of the command classes and methods will
     * not be called statically. A List of {@link Command} annotations from
     * registered commands is returned.
     *
     * @param cls the class to register
     * @return A List of {@link Command} annotations from registered commands,
     * for use in eg. a dynamic command registration system.
     */
    public List<Command> registerAndReturn(Class<?> cls) {
        return registerMethods(cls, null);
    }

    /**
     * Register the methods of a class. This will automatically construct
     * instances as necessary.
     *
     * @param cls the class to register
     * @param parent the parent method
     * @return Commands Registered
     */
    public List<Command> registerMethods(Class<?> cls, Method parent) {
        return registerMethods(cls, parent, null);
    }

    public <C> List<Command> registerMethods(Class<C> cls, @Nullable Method parent, @Nullable Provider<? extends C> provider) {
        try {
            return registerMethods0(cls, parent, provider);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new CommandRegistrationException("Failed to register commands in class " + cls.getName(), e);
        }
    }

    /**
     * Register the methods of a class.
     *
     * @param cls the class to register
     * @param parent the parent method
     * @param provider provides instances of the command method, or null to use the {@link Injector}
     * @return a list of commands
     */
    private <C> List<Command> registerMethods0(Class<C> cls, Method parent, @Nullable Provider<? extends C> provider) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Map<String, Method> map;
        List<Command> registered = new ArrayList<Command>();

        // Make a new hash map to cache the commands for this class
        // as looking up methods via reflection is fairly slow
        if (commands.containsKey(parent)) {
            map = commands.get(parent);
        } else {
            map = new HashMap<String, Method>();
            commands.put(parent, map);
        }

        for (Method method : cls.getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) {
                continue;
            }

            if(!(Void.TYPE.equals(method.getReturnType()) ||
                 List.class.isAssignableFrom(method.getReturnType()))) {
                throw new CommandRegistrationException("Command method " + method.getDeclaringClass().getName() + "#" + method.getName() +
                                                       " must return either void or List<String>");
            }

            boolean isStatic = Modifier.isStatic(method.getModifiers());

            Command cmd = method.getAnnotation(Command.class);

            // Cache the aliases too
            for (String alias : cmd.aliases()) {
                map.put(alias, method);
            }

            // We want to be able invoke with an instance
            if (!isStatic) {
                if(provider == null && injector != null) {
                    // If we weren't given a Provider, try to get one from the Injector
                    provider = injector.getProviderOrNull(cls);
                    if(provider == null) {
                        // If we can't get a provider, check if we have already instantiated the class
                        C instance = (C) instances.get(cls);
                        if(instance == null) {
                            // If we haven't, do that now and save it
                            instance = (C) injector.getInstance(cls);
                            instances.put(cls, instance);
                        }
                        // Generate a provider that just returns the saved instance
                        final C finalInstance = instance;
                        provider = () -> finalInstance;
                    }
                }

                if(provider != null) {
                    providers.put(method, provider);
                } else {
                    String text = "Failed to get an instance/provider of " + cls.getName() +
                                  " for command method " + method.getDeclaringClass().getName() + "#" + method.getName();
                    if(injector == null) {
                        text += " (no Injector is available to create it)";
                    } else {
                        text += " (the Injector returned null when asked for one)";
                    }
                    throw new CommandRegistrationException(text);
                }
            }

            // Build a list of commands and their usage details, at least for
            // root level commands
            if (parent == null) {
                final String commandName = cmd.aliases()[0];
                final String desc = cmd.desc();

                final String usage = cmd.usage();
                if (usage.isEmpty()) {
                    descs.put(commandName, desc);
                } else {
                    descs.put(commandName, usage + " - " + desc);
                }

                String help = cmd.help();
                if (help.isEmpty()) {
                    help = desc;
                }

                final CharSequence arguments = getArguments(cmd);
                for (String alias : cmd.aliases()) {
                    final String helpMessage = "/" + alias + " " + arguments + "\n\n" + help;
                    final String key = alias.replaceAll("/", "");
                    String previous = helpMessages.put(key, helpMessage);

                    if (previous != null && !previous.replaceAll("^/[^ ]+ ", "").equals(helpMessage.replaceAll("^/[^ ]+ ", ""))) {
                        helpMessages.put(key, previous + "\n\n" + helpMessage);
                    }
                }

            }

            // Add the command to the registered command list for return
            registered.add(cmd);

            // Look for nested commands -- if there are any, those have
            // to be cached too so that they can be quickly looked
            // up when processing commands
            if (method.isAnnotationPresent(NestedCommand.class)) {
                NestedCommand nestedCmd = method.getAnnotation(NestedCommand.class);

                for (Class<?> nestedCls : nestedCmd.value()) {
                    registerMethods(nestedCls, method);
                }
            }
        }

        if (cls.getSuperclass() != null) {
            registerMethods0(cls.getSuperclass(), parent, provider);
        }

        return registered;
    }

    /**
     * Checks to see whether there is a command named such at the root level.
     * This will check aliases as well.
     *
     * @param command the command
     * @return true if the command exists
     */
    public boolean hasCommand(String command) {
        return commands.get(null).containsKey(command.toLowerCase());
    }

    /**
     * Get a list of command descriptions. This is only for root commands.
     *
     * @return a map of commands
     */
    public Map<String, String> getCommands() {
        return descs;
    }

    /**
     * Get the mapping of methods under a parent command.
     *
     * @return the mapping
     */
    public Map<Method, Map<String, Method>> getMethods() {
        return commands;
    }

    /**
     * Get a map from command name to help message. This is only for root commands.
     *
     * @return a map of help messages for each command
     */
    public Map<String, String> getHelpMessages() {
        return helpMessages;
    }

    /**
     * Get the usage string for a command.
     *
     * @param args the arguments
     * @param level the depth of the command
     * @param cmd the command annotation
     * @return the usage string
     */
    protected String getUsage(String[] args, int level, Command cmd) {
        final StringBuilder command = new StringBuilder();

        command.append('/');

        for (int i = 0; i <= level; ++i) {
            command.append(args[i]);
            command.append(' ');
        }
        command.append(getArguments(cmd));

        final String help = cmd.help();
        if (!help.isEmpty()) {
            command.append("\n\n");
            command.append(help);
        }

        return command.toString();
    }

    protected CharSequence getArguments(Command cmd) {
        final String flags = cmd.flags();

        final StringBuilder command2 = new StringBuilder();
        if (!flags.isEmpty()) {
            String flagString = flags.replaceAll(".:", "");
            if (!flagString.isEmpty()) {
                command2.append("[-");
                for (int i = 0; i < flagString.length(); ++i) {
                    command2.append(flagString.charAt(i));
                }
                command2.append("] ");
            }
        }

        command2.append(cmd.usage());

        return command2;
    }

    /**
     * Get the usage string for a nested command.
     *
     * @param args the arguments
     * @param level the depth of the command
     * @param method the parent method
     * @param player the player
     * @return the usage string
     * @throws CommandException on some error
     */
    protected String getNestedUsage(String[] args, int level, Method method, T player) throws CommandException {
        StringBuilder command = new StringBuilder();

        command.append("/");

        for (int i = 0; i <= level; ++i) {
            command.append(args[i]).append(" ");
        }

        Map<String, Method> map = commands.get(method);
        boolean found = false;

        command.append("<");

        Set<String> allowedCommands = new HashSet<String>();

        for (Map.Entry<String, Method> entry : map.entrySet()) {
            Method childMethod = entry.getValue();
            found = true;

            if (hasPermission(childMethod, player)) {
                Command childCmd = childMethod.getAnnotation(Command.class);

                allowedCommands.add(childCmd.aliases()[0]);
            }
        }

        if (!allowedCommands.isEmpty()) {
            command.append(StringUtil.joinString(allowedCommands, "|", 0));
        } else {
            if (!found) {
                command.append("?");
            } else {
                //command.append("action");
                throw new CommandPermissionsException();
            }
        }

        command.append(">");

        return command.toString();
    }

    private static boolean supportsCompletion(Method method) {
        return List.class.isAssignableFrom(method.getReturnType()) ||
               Stream.of(method.getExceptionTypes())
                     .anyMatch(SuggestException.class::isAssignableFrom);
    }

    /**
     * Attempt to execute a command. This version takes a separate command
     * name (for the root command) and then a list of following arguments.
     *
     * @param cmd command to run
     * @param args arguments
     * @param player command source
     * @param methodArgs method arguments
     * @throws CommandException thrown when the command throws an error
     */
    public void execute(String cmd, String[] args, T player, Object... methodArgs) throws CommandException {
        executeMethod(false, cmd, args, player, methodArgs);
    }

    /**
     * Attempt to complete a command.
     *
     * If null is returned, the server's default completion should be used.
     * Any non-null return should be used as the completion results, even if empty.
     */
    public @Nullable List<String> complete(String cmd, String[] args, T player, Object... methodArgs) {
        try {
            return executeMethod(true, cmd, args, player, methodArgs);
        } catch(CommandException e) {
            return Collections.emptyList();
        }
    }

    private List<String> executeMethod(boolean completing, String cmd, String[] args, T player, Object... methodArgs) throws CommandException {
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = cmd;
        Object[] newMethodArgs = new Object[methodArgs.length + 1];
        System.arraycopy(methodArgs, 0, newMethodArgs, 1, methodArgs.length);

        return executeMethod(null, completing, newArgs, player, newMethodArgs, 0);
    }

    /**
     * Attempt to execute a command.
     *
     * @param parent the parent method
     * @param completing true if completing the command, false if executing
     * @param args an array of arguments
     * @param player the player
     * @param methodArgs the array of method arguments
     * @param level the depth of the command
     *
     * @return A list of completions, or null to use the server's default completion (player names).
     *         Returning an empty list will prevent any completion from happening.
     *
     * @throws CommandException thrown on a command error
     */

    private List<String> executeMethod(Method parent, boolean completing, String[] args, T player, Object[] methodArgs, int level) throws CommandException {
        final String cmdName = args[level];
        final String cmdNameLower = cmdName.toLowerCase();
        final int argsCount = args.length - 1 - level;
        final Map<String, Method> map = commands.get(parent);

        if(completing && argsCount == 0) {
            // Completing with no args means the command itself is being completed.
            // Gather all matching commands, that the player has permission to run,
            // and return them as completion options. If a full command is being
            // completed, it will be returned alone, which will just advance the cursor.
            final List<String> children = new ArrayList<>();
            for(Map.Entry<String, Method> entry : map.entrySet()) {
                final String child = entry.getKey();
                if(child.toLowerCase().startsWith(cmdNameLower) && hasPermission(entry.getValue(), player)) {
                    children.add(child);
                }
            }
            return children;
        }

        final Method method = map.get(cmdNameLower);
        if (method == null) {
            if (parent == null) { // Root
                throw new UnhandledCommandException();
            } else {
                throw new MissingNestedCommandException("Unknown command: " + cmdName,
                        getNestedUsage(args, level - 1, parent, player));
            }
        }

        if(!hasPermission(method, player)) {
            throw new CommandPermissionsException();
        }

        // checks if we need to execute the body of the nested command method (false)
        // or display the help what commands are available (true)
        // this is all for an args count of 0 if it is > 0 and a NestedCommand Annotation is present
        // it will always handle the methods that NestedCommand points to
        // e.g.:
        //  - /cmd - @NestedCommand(executeBody = true) will go into the else loop and execute code in that method
        //  - /cmd <arg1> <arg2> - @NestedCommand(executeBody = true) will always go to the nested command class
        //  - /cmd <arg1> - @NestedCommand(executeBody = false) will always go to the nested command class not matter the args
        final NestedCommand nestedAnnot = method.getAnnotation(NestedCommand.class);

        if (nestedAnnot != null && (argsCount > 0 || nestedAnnot.executeBody())) {
            if (argsCount == 0) {
                throw new MissingNestedCommandException("Sub-command required.",
                                                        getNestedUsage(args, level, method, player));
            } else {
                return executeMethod(method, completing, args, player, methodArgs, level + 1);
            }
        } else if (method.isAnnotationPresent(CommandAlias.class)) {
            CommandAlias aCmd = method.getAnnotation(CommandAlias.class);
            return executeMethod(parent, completing, aCmd.value(), player, methodArgs, level);
        } else {
            Command cmd = method.getAnnotation(Command.class);

            // If the command method doesn't do completions, return null to indicate that
            // the default completion (player name) should be used.
            if(completing && !supportsCompletion(method)) return null;

            String[] newArgs = new String[args.length - level];
            System.arraycopy(args, level, newArgs, 0, args.length - level);

            final Set<Character> valueFlags = new HashSet<>();

            char[] flags = cmd.flags().toCharArray();
            Set<Character> newFlags = new HashSet<Character>();
            for (int i = 0; i < flags.length; ++i) {
                if (flags.length > i + 1 && flags[i + 1] == ':') {
                    valueFlags.add(flags[i]);
                    ++i;
                }
                newFlags.add(flags[i]);
            }

            CommandContext context = new CommandContext(newArgs, valueFlags, completing);

            if(!completing) {
                if (context.argsLength() < cmd.min()) {
                    throw new CommandUsageException("Too few arguments.", getUsage(args, level, cmd));
                }

                if (cmd.max() != -1 && context.argsLength() > cmd.max()) {
                    throw new CommandUsageException("Too many arguments.", getUsage(args, level, cmd));
                }

                if (!cmd.anyFlags()) {
                    for (char flag : context.getFlags()) {
                        if (!newFlags.contains(flag)) {
                            throw new CommandUsageException("Unknown flag: " + flag, getUsage(args, level, cmd));
                        }
                    }
                }
            }

            methodArgs[0] = context;

            Provider provider = providers.get(method);
            Object instance = provider == null ? null : provider.get();

            try {
                // If we get here while completing, it means the method's return type is a List<String>,
                // and we never want to use the default completion. So if it returns null, convert it to
                // an empty list.
                List<String> completions = (List<String>) method.invoke(instance, methodArgs);
                return completions != null ? completions : Collections.emptyList();
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logger.log(Level.SEVERE, "Failed to execute command", e);
                return Collections.emptyList();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof SuggestException && context.isSuggesting()) {
                    return ((SuggestException) e.getCause()).suggestions();
                } else if (e.getCause() instanceof CommandException) {
                    if(e.getCause() instanceof CommandUsageException) {
                        ((CommandUsageException) e.getCause()).offerUsage(getUsage(args, argsCount, method.getAnnotation(Command.class)));
                    }
                    throw (CommandException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new WrappedCommandException(e.getCause());
                }
            }
        }
    }

    /**
     * Returns whether a player has access to a command.
     *
     * @param method the method
     * @param player the player
     * @return true if permission is granted
     */
    protected boolean hasPermission(Method method, T player) {
        CommandPermissions perms = method.getAnnotation(CommandPermissions.class);
        if (perms == null) {
            return true;
        }

        for (String perm : perms.value()) {
            if (hasPermission(player, perm)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether a player permission..
     *
     * @param player the player
     * @param permission the permission
     * @return true if permission is granted
     */
    public abstract boolean hasPermission(T player, String permission);

    /**
     * Get the injector used to create new instances. This can be
     * null, in which case only classes will be registered statically.
     *
     * @return an injector instance
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Set the injector for creating new instances.
     *
     * @param injector injector or null
     */
    public void setInjector(Injector injector) {
        this.injector = injector;
    }
}
