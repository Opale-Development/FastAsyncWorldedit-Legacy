package com.boydti.fawe.object;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores changes to a {@link ChangeSet}.
 */
public class HistoryExtent extends AbstractDelegateExtent {

    private final FaweQueue queue;
    private final EditSession session;
    private FaweChangeSet changeSet;

    /**
     * Create a new instance.
     *
     * @param extent    the extent
     * @param changeSet the change set
     */
    public HistoryExtent(final EditSession session, final Extent extent, final FaweChangeSet changeSet, FaweQueue queue) {
        super(extent);
        checkNotNull(changeSet);
        this.queue = queue;
        this.changeSet = changeSet;
        this.session = session;
    }

    public FaweChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(FaweChangeSet fcs) {
        this.changeSet = fcs;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        int combined = queue.getCombinedId4DataDebug(x, y, z, 0, session);
        int id = (combined >> 4);
        if (id == block.getId()) {
            if (!FaweCache.hasData(id)) {
                if (!block.hasNbtData()) {
                    return false;
                }
            } else if (!block.hasNbtData()) {
                int data = combined & 0xF;
                if (data == block.getData()) {
                    return false;
                }
            }
        }
        try {
            if (!FaweCache.hasNBT(id)) {
                if (block.canStoreNBTData()) {
                    this.changeSet.add(x, y, z, combined, block);
                } else {
                    this.changeSet.add(x, y, z, combined, (block.getId() << 4) + block.getData());
                }
            } else {
                try {
                    CompoundTag tag = queue.getTileEntity(x, y, z);
                    this.changeSet.add(x, y, z, new BaseBlock(id, combined & 0xF, tag), block);
                } catch (Throwable e) {
                    e.printStackTrace();
                    this.changeSet.add(x, y, z, combined, block);
                }
            }
        } catch (FaweException ignore) {
            return false;
        }
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Override
    public BaseBlock getLazyBlock(Vector location) {
        return getLazyBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Nullable
    @Override
    public Entity createEntity(final Location location, final BaseEntity state) {
        final Entity entity = super.createEntity(location, state);
        if ((state != null)) {
            this.changeSet.addEntityCreate(state.getNbtData());
        }
        return entity;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.wrapEntities(super.getEntities());
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return this.wrapEntities(super.getEntities(region));
    }

    private List<? extends Entity> wrapEntities(final List<? extends Entity> entities) {
        final List<Entity> newList = new ArrayList<Entity>(entities.size());
        for (final Entity entity : entities) {
            newList.add(new TrackedEntity(entity));
        }
        return newList;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome newBiome) {
        BaseBiome oldBiome = this.getBiome(position);
        if (oldBiome.getId() != newBiome.getId()) {
            this.changeSet.addBiomeChange(position.getBlockX(), position.getBlockZ(), oldBiome, newBiome);
            return getExtent().setBiome(position, newBiome);
        } else {
            return false;
        }
    }

    @Override
    public boolean setBiome(int x, int y, int z, BaseBiome newBiome) {
        BaseBiome oldBiome = this.getBiome(MutableBlockVector2D.get(x, z));
        if (oldBiome.getId() != newBiome.getId()) {
            this.changeSet.addBiomeChange(x, z, oldBiome, newBiome);
            return getExtent().setBiome(x, y, z, newBiome);
        } else {
            return false;
        }
    }

    public class TrackedEntity implements Entity {
        private final Entity entity;

        private TrackedEntity(final Entity entity) {
            this.entity = entity;
        }

        @Override
        public BaseEntity getState() {
            return this.entity.getState();
        }

        @Override
        public Location getLocation() {
            return this.entity.getLocation();
        }

        @Override
        public Extent getExtent() {
            return this.entity.getExtent();
        }

        @Override
        public boolean remove() {
            final Location location = this.entity.getLocation();
            final BaseEntity state = this.entity.getState();
            final boolean success = this.entity.remove();
            if ((state != null) && success) {
                HistoryExtent.this.changeSet.addEntityRemove(state.getNbtData());
            }
            return success;
        }

        @Nullable
        @Override
        public <T> T getFacet(final Class<? extends T> cls) {
            return this.entity.getFacet(cls);
        }
    }
}
