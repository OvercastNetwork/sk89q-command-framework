package com.sk89q.minecraft.util.commands;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Throw this exception out of a command method to suggest completions for the command.
 *
 * This is only allowed when {@link CommandContext#isSuggesting()} is true. If it isn't,
 * then the exception is handled like any other uncaught exception.
 */
public class SuggestException extends Exception {

    private final ImmutableList<String> suggestions;

    public SuggestException(Iterable<String> suggestions) {
        this.suggestions = ImmutableList.copyOf(suggestions);
    }

    public List<String> suggestions() {
        return suggestions;
    }
}
