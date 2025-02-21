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

package com.sk89q.worldedit.session;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.registry.WorldData;

import java.util.Collections;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Holds the clipboard and the current transform on the clipboard.
 */
public class ClipboardHolder {
    private final WorldData worldData;
    private Clipboard clipboard;
    private Transform transform = new Identity();

    /**
     * Create a new instance with the given clipboard.
     *
     * @param clipboard the clipboard
     * @param worldData the mapping of blocks, entities, and so on
     */
    public ClipboardHolder(Clipboard clipboard, WorldData worldData) {
        checkNotNull(clipboard);
        checkNotNull(worldData);
        this.clipboard = clipboard;
        this.worldData = worldData;
    }

    protected ClipboardHolder() {
        worldData = null;
    }

    public static Class<?> inject() {
        return ClipboardHolder.class;
    }

    /**
     * Get the mapping used for blocks, entities, and so on.
     *
     * @return the mapping
     */
    public WorldData getWorldData() {
        return worldData;
    }

    /**
     * Get the clipboard.
     * <p>
     * If there is a transformation applied, the returned clipboard will
     * not contain its effect.
     *
     * @return the clipboard
     * @deprecated FAWE supports multiple loaded schematics {@link #getClipboards()}
     */
    @Deprecated
    public Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * Get all currently held clipboards
     *
     * @return
     */
    public List<Clipboard> getClipboards() {
        return Collections.singletonList(getClipboard());
    }

    public boolean contains(Clipboard clipboard) {
        return this.clipboard == clipboard;
    }

    /**
     * Get all end ClipboardHolders<br/>
     * - Usually this will return itself.<br/>
     * - If this is a multi clipboard, it will return the children
     *
     * @return Set of end ClipboardHolders
     */
    public List<ClipboardHolder> getHolders() {
        return Collections.singletonList(this);
    }

    /**
     * Get the transform.
     *
     * @return the transform
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Set the transform.
     *
     * @param transform the transform
     */
    public void setTransform(Transform transform) {
        checkNotNull(transform);
        this.transform = transform;
    }

    /**
     * Create a builder for an operation to paste this clipboard.
     *
     * @return a builder
     */
    public PasteBuilder createPaste(Extent targetExtent, WorldData targetWorldData) {
        return new PasteBuilder(this, targetExtent, targetWorldData);
    }

    public void close() {
        if (clipboard instanceof BlockArrayClipboard) {
            ((BlockArrayClipboard) clipboard).close();
        }
        clipboard = null;
    }

}
