package com.boydti.fawe.object.changeset;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.change.MutableBiomeChange;
import com.boydti.fawe.object.change.MutableBlockChange;
import com.boydti.fawe.object.change.MutableEntityChange;
import com.boydti.fawe.object.change.MutableFullBlockChange;
import com.boydti.fawe.object.change.MutableTileChange;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class FaweStreamChangeSet extends FaweChangeSet {

    public static final int HEADER_SIZE = 9;
    private final int compression;
    public int entityCreateSize;
    public int entityRemoveSize;
    public int tileCreateSize;
    public int tileRemoveSize;
    private int mode;
    private FaweStreamIdDelegate idDel;
    private FaweStreamPositionDelegate posDel;
    private int blockSize;
    private int originX;
    private int originZ;

    public FaweStreamChangeSet(World world) {
        this(world, Settings.IMP.HISTORY.COMPRESSION_LEVEL, Settings.IMP.HISTORY.STORE_REDO, Settings.IMP.HISTORY.SMALL_EDITS);
    }

    public FaweStreamChangeSet(String world) {
        this(world, Settings.IMP.HISTORY.COMPRESSION_LEVEL, Settings.IMP.HISTORY.STORE_REDO, Settings.IMP.HISTORY.SMALL_EDITS);
    }

    public FaweStreamChangeSet(String world, int compression, boolean storeRedo, boolean smallLoc) {
        super(world);
        this.compression = compression;
        init(storeRedo, smallLoc);
    }

    public FaweStreamChangeSet(World world, int compression, boolean storeRedo, boolean smallLoc) {
        super(world);
        this.compression = compression;
        init(storeRedo, smallLoc);
    }

    private void init(boolean storeRedo, boolean smallLoc) {
        if (storeRedo) {
            if (smallLoc) {
                mode = 4;
            } else {
                mode = 3;
            }
        } else if (smallLoc) {
            mode = 1;
        } else {
            mode = 2;
        }
    }

    private void setupStreamDelegates(int mode) {
        this.mode = mode;
        if (mode == 3 || mode == 4) {
            idDel = new FaweStreamIdDelegate() {
                @Override
                public void writeChange(OutputStream stream, int combinedFrom, int combinedTo) throws IOException {
                    stream.write((combinedFrom) & 0xff);
                    stream.write(((combinedFrom) >> 8) & 0xff);
                    stream.write((combinedTo) & 0xff);
                    stream.write(((combinedTo) >> 8) & 0xff);
                }

                @Override
                public void readCombined(FaweInputStream is, MutableBlockChange change, boolean dir) throws IOException {
                    if (dir) {
                        is.skipFully(2);
                        int to1 = is.read();
                        int to2 = is.read();
                        change.id = (short) ((to2 << 4) + (to1 >> 4));
                        change.data = (byte) (to1 & 0xf);
                    } else {
                        int from1 = is.read();
                        int from2 = is.read();
                        is.skipFully(2);
                        change.id = (short) ((from2 << 4) + (from1 >> 4));
                        change.data = (byte) (from1 & 0xf);
                    }
                }

                @Override
                public void readCombined(FaweInputStream is, MutableFullBlockChange change, boolean dir) throws IOException {
                    change.from = ((byte) is.read() & 0xFF) + ((byte) is.read() << 8);
                    change.to = ((byte) is.read() & 0xFF) + ((byte) is.read() << 8);
                }
            };
        } else {
            idDel = new FaweStreamIdDelegate() {
                @Override
                public void writeChange(OutputStream stream, int combinedFrom, int to) throws IOException {
                    stream.write((combinedFrom) & 0xff);
                    stream.write(((combinedFrom) >> 8) & 0xff);
                }

                @Override
                public void readCombined(FaweInputStream in, MutableBlockChange change, boolean dir) throws IOException {
                    int from1 = in.read();
                    int from2 = in.read();
                    change.id = (short) ((from2 << 4) + (from1 >> 4));
                    change.data = (byte) (from1 & 0xf);
                }

                @Override
                public void readCombined(FaweInputStream is, MutableFullBlockChange change, boolean dir) throws IOException {
                    change.from = ((byte) is.read() & 0xFF) + ((byte) is.read() << 8);
                    change.to = 0;
                }
            };
        }
        if (mode == 1 || mode == 4) { // small
            posDel = new FaweStreamPositionDelegate() {
                int lx, ly, lz;
                byte[] buffer = new byte[4];

                @Override
                public void write(OutputStream out, int x, int y, int z) throws IOException {
                    int rx = -lx + (lx = x);
                    int ry = -ly + (ly = y);
                    int rz = -lz + (lz = z);
                    byte b1 = (byte) (ry);
                    byte b2 = (byte) (rx);
                    byte b3 = (byte) (rz);
                    int x16 = (rx >> 8) & 0xF;
                    int z16 = (rz >> 8) & 0xF;
                    byte b4 = MathMan.pair16(x16, z16);
                    out.write(b1);
                    out.write(b2);
                    out.write(b3);
                    out.write(b4);
                }

                @Override
                public int readX(FaweInputStream in) throws IOException {
                    in.readFully(buffer);
                    return lx = lx + ((((buffer[1] & 0xFF) + ((MathMan.unpair16x(buffer[3])) << 8)) << 20) >> 20);
                }

                @Override
                public int readY(FaweInputStream in) {
                    return (ly = ly + buffer[0]) & 0xFF;
                }

                @Override
                public int readZ(FaweInputStream in) throws IOException {
                    return lz = lz + ((((buffer[2] & 0xFF) + ((MathMan.unpair16y(buffer[3])) << 8)) << 20) >> 20);
                }
            };
        } else {
            posDel = new FaweStreamPositionDelegate() {
                byte[] buffer = new byte[5];
                int lx, ly, lz;

                @Override
                public void write(OutputStream stream, int x, int y, int z) throws IOException {
                    int rx = -lx + (lx = x);
                    int ry = -ly + (ly = y);
                    int rz = -lz + (lz = z);
                    stream.write((rx) & 0xff);
                    stream.write(((rx) >> 8) & 0xff);
                    stream.write((rz) & 0xff);
                    stream.write(((rz) >> 8) & 0xff);
                    stream.write((byte) ry);
                }

                @Override
                public int readX(FaweInputStream is) throws IOException {
                    is.readFully(buffer);
                    return lx = (lx + (buffer[0] & 0xFF) + (buffer[1] << 8));
                }

                @Override
                public int readY(FaweInputStream is) throws IOException {
                    return (ly = (ly + (buffer[4]))) & 0xFF;
                }

                @Override
                public int readZ(FaweInputStream is) throws IOException {
                    return lz = (lz + (buffer[2] & 0xFF) + (buffer[3] << 8));
                }
            };
        }
    }

    public void writeHeader(OutputStream os, int x, int y, int z) throws IOException {
        os.write(mode);
        setOrigin(x, z);
        os.write((byte) (x >> 24));
        os.write((byte) (x >> 16));
        os.write((byte) (x >> 8));
        os.write((byte) (x));
        os.write((byte) (z >> 24));
        os.write((byte) (z >> 16));
        os.write((byte) (z >> 8));
        os.write((byte) (z));
        setupStreamDelegates(mode);
    }

    public void readHeader(InputStream is) throws IOException {
        // skip mode
        int mode = is.read();
        // origin
        int x = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + (is.read() << 0));
        int z = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + (is.read() << 0));
        setOrigin(x, z);
        setupStreamDelegates(mode);
    }

    public FaweOutputStream getCompressedOS(OutputStream os) throws IOException {
        return MainUtil.getCompressedOS(os, compression);
    }

    @Override
    public boolean isEmpty() {
        if (blockSize > 0) {
            return false;
        }
        if (waitingCombined.get() != 0 || waitingAsync.get() != 0) {
            return false;
        }
        flush();
        return blockSize == 0;
    }

    @Override
    public int size() {
        // Flush so we can accurately get the size
        flush();
        return blockSize;
    }

    public abstract int getCompressedSize();

    public abstract long getSizeInMemory();

    public long getSizeOnDisk() {
        return 0;
    }

    public abstract OutputStream getBlockOS(int x, int y, int z) throws IOException;

    public abstract OutputStream getBiomeOS() throws IOException;

    public abstract NBTOutputStream getEntityCreateOS() throws IOException;

    public abstract NBTOutputStream getEntityRemoveOS() throws IOException;

    public abstract NBTOutputStream getTileCreateOS() throws IOException;

    public abstract NBTOutputStream getTileRemoveOS() throws IOException;

    public abstract FaweInputStream getBlockIS() throws IOException;

    public abstract FaweInputStream getBiomeIS() throws IOException;

    public abstract NBTInputStream getEntityCreateIS() throws IOException;

    public abstract NBTInputStream getEntityRemoveIS() throws IOException;

    public abstract NBTInputStream getTileCreateIS() throws IOException;

    public abstract NBTInputStream getTileRemoveIS() throws IOException;

    public void setOrigin(int x, int z) {
        originX = x;
        originZ = z;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginZ() {
        return originZ;
    }

    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        blockSize++;
        try {
            OutputStream stream = getBlockOS(x, y, z);
            //x
            posDel.write(stream, x - originX, y, z - originZ);
            idDel.writeChange(stream, combinedFrom, combinedTo);
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void addBiomeChange(int x, int z, BaseBiome from, BaseBiome to) {
        blockSize++;
        try {
            OutputStream os = getBiomeOS();
            os.write((byte) (x >> 24));
            os.write((byte) (x >> 16));
            os.write((byte) (x >> 8));
            os.write((byte) (x));
            os.write((byte) (z >> 24));
            os.write((byte) (z >> 16));
            os.write((byte) (z >> 8));
            os.write((byte) (z));
            os.write(from.getId());
            os.write(to.getId());
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    public void addTileCreate(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        blockSize++;
        try {
            NBTOutputStream nbtos = getTileCreateOS();
            nbtos.writeTag(tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addTileRemove(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        blockSize++;
        try {
            NBTOutputStream nbtos = getTileRemoveOS();
            nbtos.writeTag(tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addEntityRemove(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        blockSize++;
        try {
            NBTOutputStream nbtos = getEntityRemoveOS();
            nbtos.writeTag(tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addEntityCreate(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        blockSize++;
        try {
            NBTOutputStream nbtos = getEntityCreateOS();
            nbtos.writeTag(tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public Iterator<MutableBlockChange> getBlockIterator(final boolean dir) throws IOException {
        final FaweInputStream is = getBlockIS();
        if (is == null) {
            return new ArrayList<MutableBlockChange>().iterator();
        }
        final MutableBlockChange change = new MutableBlockChange(0, 0, 0, (short) 0, (byte) 0);
        return new Iterator<MutableBlockChange>() {
            private MutableBlockChange last = read();

            public MutableBlockChange read() {
                try {
                    change.x = posDel.readX(is) + originX;
                    change.y = posDel.readY(is);
                    change.z = posDel.readZ(is) + originZ;
                    idDel.readCombined(is, change, dir);
                    return change;
                } catch (EOFException ignoreOEF) {
                } catch (Exception e) {
                    e.printStackTrace();
                    MainUtil.handleError(e);
                }
                try {
                    is.close();
                } catch (IOException e) {
                    MainUtil.handleError(e);
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public MutableBlockChange next() {
                MutableBlockChange tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    public Iterator<MutableBiomeChange> getBiomeIterator(final boolean dir) throws IOException {
        final InputStream is = getBiomeIS();
        if (is == null) {
            return new ArrayList<MutableBiomeChange>().iterator();
        }
        final MutableBiomeChange change = new MutableBiomeChange();
        return new Iterator<MutableBiomeChange>() {
            private MutableBiomeChange last = new MutableBiomeChange();

            public MutableBiomeChange read() {
                try {
                    int int1 = is.read();
                    if (int1 != -1) {
                        int x = ((int1 << 24) + (is.read() << 16) + (is.read() << 8) + (is.read() << 0));
                        int z = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + (is.read() << 0));
                        int from = is.read();
                        int to = is.read();
                        change.setBiome(x, z, from, to);
                        return change;
                    }
                } catch (EOFException ignoreOEF) {
                } catch (Exception e) {
                    e.printStackTrace();
                    MainUtil.handleError(e);
                }
                try {
                    is.close();
                } catch (IOException e) {
                    MainUtil.handleError(e);
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public MutableBiomeChange next() {
                MutableBiomeChange tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    @Override
    public Iterator<Change> getIterator(BlockBag blockBag, int mode, boolean redo) {
        if (blockBag != null && mode > 0) {
            try {
                return (Iterator<Change>) (Iterator<?>) getFullBlockIterator(blockBag, mode, redo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return getIterator(redo);
    }

    public Iterator<MutableFullBlockChange> getFullBlockIterator(BlockBag blockBag, int inventory, final boolean dir) throws IOException {
        final FaweInputStream is = new FaweInputStream(getBlockIS());
        if (is == null) {
            return new ArrayList<MutableFullBlockChange>().iterator();
        }
        final MutableFullBlockChange change = new MutableFullBlockChange(blockBag, inventory, dir);
        return new Iterator<MutableFullBlockChange>() {
            private MutableFullBlockChange last = read();

            public MutableFullBlockChange read() {
                try {
                    change.x = posDel.readX(is) + originX;
                    change.y = posDel.readY(is);
                    change.z = posDel.readZ(is) + originZ;
                    idDel.readCombined(is, change, dir);
                    return change;
                } catch (EOFException ignoreOEF) {
                } catch (Exception e) {
                    e.printStackTrace();
                    MainUtil.handleError(e);
                }
                try {
                    is.close();
                } catch (IOException e) {
                    MainUtil.handleError(e);
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return last != null || ((last = read()) != null);
            }

            @Override
            public MutableFullBlockChange next() {
                MutableFullBlockChange tmp = last;
                if (tmp == null) {
                    tmp = read();
                }
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    public Iterator<MutableEntityChange> getEntityIterator(final NBTInputStream is, final boolean create, final boolean dir) {
        if (is == null) {
            return new ArrayList<MutableEntityChange>().iterator();
        }
        final MutableEntityChange change = new MutableEntityChange(null, create);
        try {
            return new Iterator<MutableEntityChange>() {
                private MutableEntityChange last = read();

                public MutableEntityChange read() {
                    try {
                        CompoundTag tag = (CompoundTag) is.readTag();
                        change.tag = tag;
                        return change;
                    } catch (Exception ignoreOEF) {
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        MainUtil.handleError(e);
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return last != null || ((last = read()) != null);
                }

                @Override
                public MutableEntityChange next() {
                    MutableEntityChange tmp = last;
                    if (tmp == null) {
                        tmp = read();
                    }
                    last = null;
                    return tmp;
                }

                @Override
                public void remove() {
                    throw new IllegalArgumentException("CANNOT REMOVE");
                }
            };
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    public Iterator<MutableTileChange> getTileIterator(final NBTInputStream is, final boolean create, final boolean dir) {
        if (is == null) {
            return new ArrayList<MutableTileChange>().iterator();
        }
        final MutableTileChange change = new MutableTileChange(null, create);
        try {
            return new Iterator<MutableTileChange>() {
                private MutableTileChange last = read();

                public MutableTileChange read() {
                    try {
                        CompoundTag tag = (CompoundTag) is.readTag();
                        change.tag = tag;
                        return change;
                    } catch (Exception ignoreOEF) {
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        MainUtil.handleError(e);
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return last != null || ((last = read()) != null);
                }

                @Override
                public MutableTileChange next() {
                    MutableTileChange tmp = last;
                    if (tmp == null) {
                        tmp = read();
                    }
                    last = null;
                    return tmp;
                }

                @Override
                public void remove() {
                    throw new IllegalArgumentException("CANNOT REMOVE");
                }
            };
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    public Iterator<Change> getIterator(final boolean dir) {
        close();
        try {
            final Iterator<MutableTileChange> tileCreate = getTileIterator(getTileCreateIS(), true, dir);
            final Iterator<MutableTileChange> tileRemove = getTileIterator(getTileRemoveIS(), false, dir);

            final Iterator<MutableEntityChange> entityCreate = getEntityIterator(getEntityCreateIS(), true, dir);
            final Iterator<MutableEntityChange> entityRemove = getEntityIterator(getEntityRemoveIS(), false, dir);

            final Iterator<MutableBlockChange> blockChange = getBlockIterator(dir);

            final Iterator<MutableBiomeChange> biomeChange = getBiomeIterator(dir);

            return new Iterator<Change>() {
                Iterator<Change>[] iterators = new Iterator[]{tileCreate, tileRemove, entityCreate, entityRemove, blockChange, biomeChange};
                int i = 0;
                Iterator<Change> current = iterators[0];

                @Override
                public boolean hasNext() {
                    if (current.hasNext()) {
                        return true;
                    } else if (i >= iterators.length - 1) {
                        return false;
                    } else {
                        current = iterators[++i];
                    }
                    return hasNext();
                }

                @Override
                public void remove() {
                    current.remove();
                }

                @Override
                public Change next() {
                    try {
                        return current.next();
                    } catch (Throwable ignore) {
                        if (i >= iterators.length - 1) {
                            throw new NoSuchElementException("End of iterator");
                        }
                        current = iterators[++i];
                        return next();
                    }
                }
            };
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
        return new ArrayList<Change>().iterator();
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return getIterator(false);
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return getIterator(true);
    }

    public interface FaweStreamPositionDelegate {
        void write(OutputStream out, int x, int y, int z) throws IOException;

        int readX(FaweInputStream in) throws IOException;

        int readY(FaweInputStream in) throws IOException;

        int readZ(FaweInputStream in) throws IOException;
    }

    public interface FaweStreamIdDelegate {
        void writeChange(OutputStream out, int from, int to) throws IOException;

        void readCombined(FaweInputStream in, MutableBlockChange change, boolean dir) throws IOException;

        void readCombined(FaweInputStream in, MutableFullBlockChange change, boolean dir) throws IOException;
    }
}
