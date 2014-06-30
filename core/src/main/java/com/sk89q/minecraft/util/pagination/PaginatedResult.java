/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.minecraft.util.pagination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandSender;

/**
 * Commands that wish to display a paginated list of results can use this class to do
 * the actual pagination, giving a list of items, a page number, and basic formatting information.
 */
public abstract class PaginatedResult<T> {
    protected final int resultsPerPage;

    public PaginatedResult() {
        this(6);
    }

    public PaginatedResult(int resultsPerPage) {
        assert resultsPerPage > 0;
        this.resultsPerPage = resultsPerPage;
    }

    public void display(WrappedCommandSender sender, Collection<? extends T> results, int page) throws CommandException {
        this.display(sender, new ArrayList<T>(results), page);
    }

    public void display(WrappedCommandSender sender, List<? extends T> results, int page) throws CommandException {
        if (results.size() == 0) throw new CommandException("No results match!");

        int maxPages = results.size() / this.resultsPerPage + 1;

        // If the content divides perfectly, eg (18 entries, and 9 per page)
        // we end up with a blank page this handles this case
        if (results.size() % this.resultsPerPage == 0) {
            maxPages--;
        }

        if (page <= 0 || page > maxPages) throw new CommandException("Unknown page selected! " + maxPages + " total pages.");

        sender.sendMessage(this.formatHeader(page, maxPages));
        for (int i = this.resultsPerPage * (page - 1); i < this.resultsPerPage * page && i < results.size(); i++) {
            sender.sendMessage(this.format(results.get(i), i));
        }
    }

    public abstract String formatHeader(int page, int maxPages);

    public abstract String format(T entry, int index);
}

