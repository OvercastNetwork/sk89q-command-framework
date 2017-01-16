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

import java.util.List;
import javax.annotation.Nullable;

import com.sk89q.util.StringUtil;

/**
 * Extra information about the context in which tab-completion is happening
 */
public class SuggestionContext {

    private final String context;
    private final String prefix;
    private final int index;
    private final @Nullable Character flag;

    public SuggestionContext(String context, String prefix, int index, @Nullable Character flag) {
        this.context = context;
        this.prefix = prefix;
        this.index = index;
        this.flag = flag;
    }

    /**
     * Return the part of the command line before the text that will be replaced by the completion.
     * This will be from the start of the first argument to the start of the text returned by
     * {@link #getPrefix()}, and may include trailing spaces.
     *
     * It is not possible for the completion to change this text.
     */
    public String getContext() {
        return context;
    }

    /**
     * Return the part of the command line that will be replaced by the completion.
     * This will be the last span of non-space characters, or an empty string if the
     * last character is a space.
     *
     * Special characters such as quotes and backslash are treated the same as any
     * other non-space character. It is not possible for completion to replace
     * anything before this.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * True if completing an argument (rather than a value flag)
     */
    public boolean isArgument() {
        return flag == null;
    }

    /**
     * True if completing the value for a flag.
     */
    public boolean isFlag() {
        return flag != null;
    }

    /**
     * The index of the argument being completed, if {@link #isArgument()} is true, otherwise -1.
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * The flag being completed, if {@link #isFlag()} is true, otherwise null.
     */
    public @Nullable Character getFlag() {
        return flag;
    }

    /**
     * Is the given argument being completed?
     */
    public boolean isArgument(int index) {
        return isArgument() && getIndex() == index;
    }

    /**
     * Is the given flag being completed?
     */
    public boolean isFlag(char flag) {
        return isFlag() && getFlag() == flag;
    }

    /**
     * Return the sorted subset of the given choices that are valid completions,
     * i.e. that start with {@link #getPrefix()}.
     */
    public List<String> complete(Iterable<String> choices) {
        return StringUtil.complete(getPrefix(), choices);
    }

    /**
     * Suggest completions based on the given choices, by throwing a {@link SuggestException}.
     *
     * Only valid choices are used, and they are sorted alphabetically.
     */
    public void suggest(Iterable<String> choices) throws SuggestException {
        throw new SuggestException(complete(choices));
    }

    /**
     * If the given argument is being completed, generate suggestions from the given choices,
     * by throwing a {@link SuggestException}.
     */
    public void suggestArgument(int index, Iterable<String> choices) throws SuggestException {
        if(isArgument(index)) {
            suggest(choices);
        }
    }

    /**
     * If the given flag is being completed, generate suggestions from the given choices,
     * by throwing a {@link SuggestException}.
     */
    public void suggestFlag(char flag, Iterable<String> choices) throws SuggestException {
        if(isFlag(flag)) {
            suggest(choices);
        }
    }

    @Override
    public String toString() {
        return isArgument() ? "argument " + getIndex()
                            : "flag -" + getFlag();
    }
}
