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

import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nullable;
import javax.inject.Provider;

/**
 * Constructs new instances.
 */
public interface Injector {

    /**
     * Return a {@link Provider} for the given command class. The framework will
     * call this at registration time, and use the provider to get a {@link T}
     * instance every time a command is executed.
     *
     * If null is returned, then {@link #getInstance(Class)} will be called at
     * registration time to get the instance, and it will be reused forever.
     */
    default @Nullable <T> Provider<? extends T> getProviderOrNull(Class<T> cls) {
        return null;
    }

    /**
     * Constructs a new instance of the given class.
     * 
     * @param cls class
     * @return object
     * @throws IllegalAccessException thrown on injection fault
     * @throws InstantiationException thrown on injection fault
     * @throws InvocationTargetException thrown on injection fault
     */
    public Object getInstance(Class<?> cls) throws InvocationTargetException, IllegalAccessException, InstantiationException;

}
