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

package com.sk89q.worldedit.function.mask;

import com.sk89q.minecraft.util.commands.Link;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.UtilityCommands;

import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Tests whether a given vector meets a criteria.
 */
@Link(clazz = UtilityCommands.class, value = "masks")
public interface Mask extends com.sk89q.worldedit.masks.Mask, Serializable {

    public static Class<Mask> inject() {
        return Mask.class;
    }

    /**
     * Returns true if the criteria is met.
     *
     * @param vector the vector to test
     * @return true if the criteria is met
     */
    boolean test(Vector vector);

    /**
     * Get the 2D version of this mask if one exists.
     *
     * @return a 2D mask version or {@code null} if this mask can't be 2D
     */
    @Nullable
    default Mask2D toMask2D() {
        return null;
    }

    default void prepare(LocalSession session, LocalPlayer player, Vector target) {
    }

    default boolean matches(EditSession editSession, Vector position) {
        return test(position);
    }
}
