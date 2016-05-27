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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public class CommandContext {
    
    // Raw command line words, including the command itself
    // Split on literal ' ', so any element can be blank except the first one
    protected final String[] originalArgs;

    // Name of the command, i.e. the first element of originaArgs
    protected final String command;

    // Final arguments, after all parsing and escaping
    // Does not include the command name, flags, or flag values
    // Last element can be blank if completing
    protected final List<String> parsedArgs = new ArrayList<>();

    // Starting index in originalArgs of each respective element of parsedArgs
    protected final List<Integer> originalArgIndices = new ArrayList<>();

    // Boolean flags present
    protected final Set<Character> booleanFlags = new HashSet<>();

    // Value flags present, and their values
    // One flag may have a blank value if its being completed
    protected final Map<Character, String> valueFlags = new HashMap<>();

    protected final @Nullable SuggestionContext suggestionContext;
    protected final CommandLocals locals;

    public CommandContext(String[] args, Set<Character> valueFlags, boolean completing) throws CommandException {
        this(args, valueFlags, completing, null);
    }

    /**
     * Parse the given array of arguments.
     * 
     * <p>Empty arguments are removed from the list of arguments.</p>
     * 
     * @param args an array with arguments
     * @param valueFlagNames a set containing all value flags (pass null to disable value flag parsing)
     * @param completing true if completing a partial command, false if executing the command
     * @param locals the locals, null to create empty one
     * @throws CommandException thrown on a parsing error
     */
    public CommandContext(String[] args, @Nullable Set<Character> valueFlagNames, boolean completing, @Nullable CommandLocals locals) throws CommandException {
        if (valueFlagNames == null) {
            valueFlagNames = Collections.emptySet();
        }

        this.originalArgs = args;
        this.command = args[0];
        this.locals = locals != null ? locals : new CommandLocals();
        if(args.length < 2) completing = false;

        boolean acceptingFlags = true;
        Character valueFlag = null;
        Character completingFlag = null;
        int completingIndex = -1;

        for(int argIndex = 1; argIndex < args.length; ++argIndex) {
            String arg = args[argIndex];
            final int startIndex = argIndex;

            if(arg.isEmpty()) {
                // If arg is empty, and it's not the last arg being completed, skip it
                if(!completing || argIndex != args.length - 1) continue;
            } else {
                final char c = arg.charAt(0);
                if(c == '\\' || c == '"') {
                    // Start of quoted argument, consume unparsed args until
                    // closing quote, and append them to the parsed arg.
                    boolean first = true;
                    for(;argIndex < args.length; ++argIndex) {
                        final String part;
                        if(first) {
                            // Remove leading quote from first part
                            first = false;
                            part = args[argIndex].substring(1);
                        } else {
                            // Insert space before non-first parts
                            arg += ' ';
                            part = args[argIndex];
                        }

                        if(!part.isEmpty() && part.charAt(part.length() - 1) == c) {
                            // If part ends in a quote, append it without the quote, and terminate
                            arg += part.substring(0, part.length() - 1);
                            break;
                        } else {
                            // Otherwise, just append the part
                            arg += part;
                        }
                    }
                } else if(valueFlag == null) {
                    // If not expecting a flag value, parse flags
                    if("--".equals(arg)) {
                        // Flag terminator '--', don't try to parse flags after this
                        acceptingFlags = false;
                        arg = null;
                    } else if(acceptingFlags && arg.matches("^-[a-zA-Z?]+$")) {
                        // Flag set, parse any number of boolean flags, and up to one value flag
                        for(int iFlag = 1; iFlag < arg.length(); iFlag++) {
                            final char flagName = arg.charAt(iFlag);
                            if(valueFlagNames.contains(flagName)) {
                                if(valueFlags.containsKey(flagName)) {
                                    throw new CommandException("Value flag '" + flagName + "' already given");
                                }
                                if(valueFlag == null) {
                                    valueFlag = flagName;
                                } else {
                                    throw new CommandException("No value specified for the '-" + flagName + "' flag.");
                                }
                            } else {
                                booleanFlags.add(flagName);
                            }
                        }
                        arg = null;
                    }
                }
            }

            if(arg != null) {
                if(valueFlag == null) {
                    // Append the parsed argument and its source index
                    if(completing) {
                        completingIndex = parsedArgs.size();
                        completingFlag = null;
                    }
                    parsedArgs.add(arg);
                    originalArgIndices.add(startIndex);
                } else {
                    // Assign the parsed arg to the preceding value flag
                    if(completing) {
                        completingIndex = -1;
                        completingFlag = valueFlag;
                    }
                    valueFlags.put(valueFlag, arg);
                    valueFlag = null;
                }
            }
        }

        if(valueFlag != null) {
            // Last arg cannot be a value flag
            throw new CommandException("No value specified for the '-" + valueFlag + "' flag.");
        }

        if(completing) {
            String context = "";
            for(int i = 1; i < args.length - 1; i++) {
                context += args[i] + ' ';
            }
            suggestionContext = new SuggestionContext(context, args[args.length - 1], completingIndex, completingFlag);
        } else {
            suggestionContext = null;
        }
    }

    /**
     * Return the context for which command completion is being requested,
     * or null if the command is not being completed.
     */
    public @Nullable SuggestionContext getSuggestionContext() {
        return suggestionContext;
    }

    public String getCommand() {
        return command;
    }

    public boolean matches(String command) {
        return this.command.equalsIgnoreCase(command);
    }

    public String getString(int index) {
        return parsedArgs.get(index);
    }

    public String getString(int index, String def) {
        return index < parsedArgs.size() ? parsedArgs.get(index) : def;
    }

    public String getJoinedStrings(int initialIndex) {
        initialIndex = originalArgIndices.get(initialIndex);
        StringBuilder buffer = new StringBuilder(originalArgs[initialIndex]);
        for (int i = initialIndex + 1; i < originalArgs.length; ++i) {
            buffer.append(" ").append(originalArgs[i]);
        }
        return buffer.toString();
    }
    
    public String getRemainingString(int start) {
        return getString(start, parsedArgs.size() - 1);
    }

    public String getString(int start, int end) {
        StringBuilder buffer = new StringBuilder(parsedArgs.get(start));
        for (int i = start + 1; i < end + 1; ++i) {
            buffer.append(" ").append(parsedArgs.get(i));
        }
        return buffer.toString();
    }

    public int getInteger(int index) throws CommandNumberFormatException {
        final String text = parsedArgs.get(index);
        try {
            return Integer.parseInt(text);
        } catch(NumberFormatException e) {
            throw new CommandNumberFormatException(text);
        }
    }

    public int getInteger(int index, int def) throws CommandNumberFormatException {
        return index < parsedArgs.size() ? getInteger(index) : def;
    }

    public double getDouble(int index) throws CommandNumberFormatException {
        final String text = parsedArgs.get(index);
        try {
            return Double.parseDouble(text);
        } catch(NumberFormatException e) {
            throw new CommandNumberFormatException(text);
        }
    }

    public double getDouble(int index, double def) throws CommandNumberFormatException {
        return index < parsedArgs.size() ? getDouble(index) : def;
    }

    public String[] getSlice(int index) {
        String[] slice = new String[originalArgs.length - index];
        System.arraycopy(originalArgs, index, slice, 0, originalArgs.length - index);
        return slice;
    }

    public String[] getPaddedSlice(int index, int padding) {
        String[] slice = new String[originalArgs.length - index + padding];
        System.arraycopy(originalArgs, index, slice, padding, originalArgs.length - index);
        return slice;
    }

    public String[] getParsedSlice(int index) {
        String[] slice = new String[parsedArgs.size() - index];
        System.arraycopy(parsedArgs.toArray(new String[parsedArgs.size()]), index, slice, 0, parsedArgs.size() - index);
        return slice;
    }

    public String[] getParsedPaddedSlice(int index, int padding) {
        String[] slice = new String[parsedArgs.size() - index + padding];
        System.arraycopy(parsedArgs.toArray(new String[parsedArgs.size()]), index, slice, padding, parsedArgs.size() - index);
        return slice;
    }

    public boolean hasFlag(char ch) {
        return booleanFlags.contains(ch) || valueFlags.containsKey(ch);
    }

    public Set<Character> getFlags() {
        return booleanFlags;
    }

    public Map<Character, String> getValueFlags() {
        return valueFlags;
    }

    public String getFlag(char ch) {
        return valueFlags.get(ch);
    }

    public String getFlag(char ch, String def) {
        final String value = valueFlags.get(ch);
        if (value == null) {
            return def;
        }

        return value;
    }

    public int getFlagInteger(char ch) throws CommandNumberFormatException {
        final String text = valueFlags.get(ch);
        try {
            return Integer.parseInt(text);
        } catch(NumberFormatException e) {
            throw new CommandNumberFormatException(text);
        }
    }

    public int getFlagInteger(char ch, int def) throws CommandNumberFormatException {
        return !valueFlags.containsKey(ch) ? def : getFlagInteger(ch);
    }

    public double getFlagDouble(char ch) throws CommandNumberFormatException {
        final String text = valueFlags.get(ch);
        try {
            return Double.parseDouble(text);
        } catch(NumberFormatException e) {
            throw new CommandNumberFormatException(text);
        }
    }

    public double getFlagDouble(char ch, double def) throws CommandNumberFormatException {
        return !valueFlags.containsKey(ch) ? def : getFlagDouble(ch);
    }

    public int argsLength() {
        return parsedArgs.size();
    }

    public CommandLocals getLocals() {
        return locals;
    }
}
