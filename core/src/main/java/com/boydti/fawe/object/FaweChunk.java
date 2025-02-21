package com.boydti.fawe.object;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.biome.BaseBiome;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;

public abstract class FaweChunk<T> implements Callable<FaweChunk> {

    public static int HEIGHT = 256;
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<Runnable>(0);
    private FaweQueue parent;
    private int x, z;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     */
    public FaweChunk(FaweQueue parent, int x, int z) {
        this.parent = parent;
        this.x = x;
        this.z = z;
    }

    /**
     * Change the chunk's location<br>
     * - E.g. if you are cloning a chunk and want to set multiple
     *
     * @param parent
     * @param x
     * @param z
     */
    public void setLoc(FaweQueue parent, int x, int z) {
        this.parent = parent;
        this.x = x;
        this.z = z;
    }

    /**
     * Get the parent queue this chunk belongs to
     *
     * @return
     */
    public FaweQueue getParent() {
        return parent;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /**
     * Get a unique hashcode for this chunk
     *
     * @return
     */
    public long longHash() {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    /**
     * Get a hashcode; unique below abs(x/z) < Short.MAX_VALUE
     *
     * @return
     */
    @Override
    public int hashCode() {
        return x << 16 | z & 0xFFFF;
    }

    /**
     * Add the chunk to the queue
     */
    public void addToQueue() {
        parent.setChunk(this);
    }

    /**
     * The modified sections
     *
     * @return
     */
    public abstract int getBitMask();

    /**
     * Get the combined block id at a location<br>
     * combined = (id <<<< 4) + data
     *
     * @param x
     * @param y
     * @param z
     * @return The combined id
     */
    public abstract int getBlockCombinedId(int x, int y, int z);

    public void setBlock(int x, int y, int z, BaseBlock block) {
        setBlock(x, y, z, block.getId(), block.getData());
        if (block.hasNbtData()) {
            setTile(x & 15, y, z & 15, block.getNbtData());
        }
    }

    public BaseBlock getBlock(int x, int y, int z) {
        int combined = getBlockCombinedId(x, y, z);
        int id = FaweCache.getId(combined);
        if (!FaweCache.hasNBT(id)) {
            return FaweCache.CACHE_BLOCK[combined];
        }
        try {
            CompoundTag tile = getTile(x & 15, y, z & 15);
            if (tile != null) {
                return new BaseBlock(id, FaweCache.getData(combined), tile);
            } else {
                return FaweCache.CACHE_BLOCK[combined];
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
            return FaweCache.CACHE_BLOCK[combined];
        }
    }

    /**
     * Get the combined id array at a layer or null if it does not exist
     *
     * @param layer
     * @return char[] or null
     */
    public
    @Nullable
    char[] getIdArray(int layer) {
        char[] ids = new char[4096];
        int by = layer << 4;
        int index = 0;
        for (int y = 0; y < 16; y++) {
            int yy = by + y;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    ids[index++] = (char) getBlockCombinedId(x, yy, z);
                }
            }
        }
        return ids;
    }

    public byte[][] getBlockLightArray() {
        return null;
    }

    public byte[][] getSkyLightArray() {
        return null;
    }

    public abstract byte[] getBiomeArray();

    public void forEachQueuedBlock(FaweChunkVisitor onEach) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int combined = getBlockCombinedId(x, y, z);
                    if (combined == 0) {
                        continue;
                    }
                    onEach.run(x, y, z, combined);
                }
            }
        }
    }

    public char[][] getCombinedIdArrays() {
        char[][] ids = new char[HEIGHT >> 4][];
        for (int y = 0; y < HEIGHT >> 4; y++) {
            int y4 = y >> 4;
            short[][] i1 = FaweCache.CACHE_J[y];
            for (int z = 0; z < 16; z++) {
                short[] i2 = i1[z];
                for (int x = 0; x < 16; x++) {
                    int combined = getBlockCombinedId(x, y, z);
                    if (combined == 0) {
                        continue;
                    }
                    char[] array = ids[y4];
                    if (array == null) {
                        array = ids[y4] = new char[4096];
                    }
                    int index = i2[x];
                    array[index] = (char) combined;
                }
            }
        }
        return ids;
    }

    /**
     * Fill this chunk with a block
     *
     * @param id
     * @param data
     */
    public void fill(int id, byte data) {
        fillCuboid(0, 15, 0, HEIGHT - 1, 0, 15, id, data);
    }

    /**
     * Fill a cuboid in this chunk with a block
     *
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param z1
     * @param z2
     * @param id
     * @param data
     */
    public void fillCuboid(int x1, int x2, int y1, int y2, int z1, int z2, int id, byte data) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    setBlock(x, y, z, id, data);
                }
            }
        }
    }

    /**
     * Add a task to run when this chunk is dispatched
     *
     * @param run
     */
    public void addNotifyTask(Runnable run) {
        if (run != null) {
            tasks.add(run);
        }
    }

    public boolean hasNotifyTasks() {
        return tasks.size() > 0;
    }

    public void executeNotifyTasks() {
        for (Runnable task : tasks) {
            task.run();
        }
        tasks.clear();
    }

    /**
     * Get the underlying chunk object
     *
     * @return
     */
    public abstract T getChunk();

    /**
     * Set a tile entity at a location<br>
     * - May throw an error if an invalid block is at the location
     *
     * @param x
     * @param y
     * @param z
     * @param tile
     */
    public abstract void setTile(int x, int y, int z, CompoundTag tile);

    public abstract void setEntity(CompoundTag entity);

    public abstract void removeEntity(UUID uuid);

    public void setBlock(int x, int y, int z, int id) {
        setBlock(x, y, z, id, 0);
    }

    public abstract void setBlock(final int x, final int y, final int z, final int id, final int data);

    public abstract Set<CompoundTag> getEntities();

    /**
     * Get the UUID of entities being removed
     *
     * @return
     */
    public abstract Set<UUID> getEntityRemoves();

    /**
     * Get the map of location to tile entity<br>
     * - The byte pair represents the location in the chunk<br>
     *
     * @return
     * @see com.boydti.fawe.util.MathMan#unpair16x (get0) => x
     * @see com.boydti.fawe.util.MathMan#unpair16y (get0) => z
     * get1 => y
     */
    public abstract Map<Short, CompoundTag> getTiles();

    /**
     * Get the tile at a location
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    public abstract CompoundTag getTile(int x, int y, int z);

    public void setBiome(final int x, final int z, final BaseBiome biome) {
        setBiome(x, z, (byte) biome.getId());
    }

    public abstract void setBiome(final int x, final int z, final byte biome);

    public void setBiome(final byte biome) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                setBiome(x, z, biome);
            }
        }
    }

    /**
     * Spend time now so that the chunk can be more efficiently dispatched later<br>
     * - Modifications after this call will be ignored
     */
    public void optimize() {
    }

    @Override
    public boolean equals(final Object obj) {
        if ((obj == null) || obj.hashCode() != hashCode() || !(obj instanceof FaweChunk)) {
            return false;
        }
        return longHash() != ((FaweChunk) obj).longHash();
    }

    public abstract FaweChunk<T> copy(boolean shallow);

    public void start() {
    }

    ;

    public void end() {
    }

    ;

    @Override
    public abstract FaweChunk call();
}
