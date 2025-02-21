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

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.MathUtils;
import com.sk89q.worldedit.world.storage.LegacyChunkStore;
import com.sk89q.worldedit.world.storage.McRegionChunkStore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Commands for working with chunks.
 */
@Command(aliases = {}, desc = "[legacy] Inspect chunks: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Chunk_tools)")
public class ChunkCommands {

    private final WorldEdit worldEdit;

    public ChunkCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    public static Class<ChunkCommands> inject() {
        return ChunkCommands.class;
    }

    @Command(
            aliases = {"chunkinfo"},
            usage = "",
            desc = "Get information about the chunk that you are inside",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.chunkinfo")
    public void chunkInfo(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        Vector pos = player.getBlockIn();
        int chunkX = (int) Math.floor(pos.getBlockX() / 16.0);
        int chunkZ = (int) Math.floor(pos.getBlockZ() / 16.0);

        String folder1 = Integer.toString(MathUtils.divisorMod(chunkX, 64), 36);
        String folder2 = Integer.toString(MathUtils.divisorMod(chunkZ, 64), 36);
        String filename = "c." + Integer.toString(chunkX, 36)
                + "." + Integer.toString(chunkZ, 36) + ".dat";

        player.print(BBC.getPrefix() + "Chunk: " + chunkX + ", " + chunkZ);
        player.print(BBC.getPrefix() + "Old format: " + folder1 + "/" + folder2 + "/" + filename);
        player.print(BBC.getPrefix() + "McRegion: region/" + McRegionChunkStore.getFilename(
                new Vector2D(chunkX, chunkZ)));
    }

    @Command(
            aliases = {"listchunks"},
            usage = "",
            desc = "List chunks that your selection includes",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.listchunks")
    public void listChunks(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        Set<Vector2D> chunks = session.getSelection(player.getWorld()).getChunks();

        for (Vector2D chunk : chunks) {
            player.print(BBC.getPrefix() + LegacyChunkStore.getFilename(chunk));
        }
    }

    @Command(
            aliases = {"delchunks"},
            usage = "",
            desc = "Deprecated, use anvil commands",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.delchunks")
    @Logging(REGION)
    public void deleteChunks(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        player.print(BBC.getPrefix() + "Note that this command does not yet support the mcregion format.");
        LocalConfiguration config = worldEdit.getConfiguration();

        Set<Vector2D> chunks = session.getSelection(player.getWorld()).getChunks();
        FileOutputStream out = null;

        if (config.shellSaveType == null) {
            player.printError("Shell script type must be configured: 'bat' or 'bash' expected.");
        } else if (config.shellSaveType.equalsIgnoreCase("bat")) {
            try {
                out = new FileOutputStream("worldedit-delchunks.bat");
                OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
                writer.write("@ECHO off\r\n");
                writer.write("ECHO This batch file was generated by FAWE.\r\n");
                writer.write("ECHO It contains a list of chunks that were in the selected region\r\n");
                writer.write("ECHO at the time that the /delchunks command was used. Run this file\r\n");
                writer.write("ECHO in order to delete the chunk files listed in this file.\r\n");
                writer.write("ECHO.\r\n");
                writer.write("PAUSE\r\n");

                for (Vector2D chunk : chunks) {
                    String filename = LegacyChunkStore.getFilename(chunk);
                    writer.write("ECHO " + filename + "\r\n");
                    writer.write("DEL \"world/" + filename + "\"\r\n");
                }

                writer.write("ECHO Complete.\r\n");
                writer.write("PAUSE\r\n");
                writer.close();
                player.print(BBC.getPrefix() + "worldedit-delchunks.bat written. Run it when no one is near the region.");
            } catch (IOException e) {
                player.printError("Error occurred: " + e.getMessage());
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } else if (config.shellSaveType.equalsIgnoreCase("bash")) {
            try {
                out = new FileOutputStream("worldedit-delchunks.sh");
                OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
                writer.write("#!/bin/bash\n");
                writer.write("echo This shell file was generated by FAWE.\n");
                writer.write("echo It contains a list of chunks that were in the selected region\n");
                writer.write("echo at the time that the /delchunks command was used. Run this file\n");
                writer.write("echo in order to delete the chunk files listed in this file.\n");
                writer.write("echo\n");
                writer.write("read -p \"Press any key to continue...\"\n");

                for (Vector2D chunk : chunks) {
                    String filename = LegacyChunkStore.getFilename(chunk);
                    writer.write("echo " + filename + "\n");
                    writer.write("rm \"world/" + filename + "\"\n");
                }

                writer.write("echo Complete.\n");
                writer.write("read -p \"Press any key to continue...\"\n");
                writer.close();
                player.print(BBC.getPrefix() + "worldedit-delchunks.sh written. Run it when no one is near the region.");
                player.print(BBC.getPrefix() + "You will have to chmod it to be executable.");
            } catch (IOException e) {
                player.printError("Error occurred: " + e.getMessage());
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } else {
            player.printError(BBC.getPrefix() + "Shell script type must be configured: 'bat' or 'bash' expected.");
        }
    }
}
