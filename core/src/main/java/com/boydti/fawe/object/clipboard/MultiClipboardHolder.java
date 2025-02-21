package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

import java.net.URI;
import java.util.*;


import static com.google.common.base.Preconditions.checkNotNull;

public class MultiClipboardHolder extends URIClipboardHolder {
    private final List<URIClipboardHolder> holders;
    private Clipboard[] cached;

    public MultiClipboardHolder(WorldData worldData) {
        this(URI.create(""), worldData);
    }

    public MultiClipboardHolder(URI uri, WorldData worldData) {
        super(uri, EmptyClipboard.INSTANCE, worldData);
        holders = new ArrayList<>();
    }

    public MultiClipboardHolder(URI uri, WorldData worldData, URIClipboardHolder... addHolders) {
        this(uri, worldData);
        for (URIClipboardHolder h : addHolders) add(h);
    }

    public MultiClipboardHolder(Clipboard clipboard, WorldData worldData) {
        super(URI.create(""), EmptyClipboard.INSTANCE, worldData);
        holders = new ArrayList<>();
        URI uri = URI.create("");
        if (clipboard instanceof BlockArrayClipboard) {
            FaweClipboard fc = ((BlockArrayClipboard) clipboard).IMP;
            if (fc instanceof DiskOptimizedClipboard) {
                uri = ((DiskOptimizedClipboard) fc).getFile().toURI();
            }
        }
        add(uri, clipboard);
    }

    public void remove(URI uri) {
        cached = null;
        if (getUri().equals(uri)) {
            for (ClipboardHolder holder : holders) holder.close();
            holders.clear();
            return;
        }
        for (int i = holders.size() - 1; i >= 0; i--) {
            URIClipboardHolder holder = holders.get(i);
            if (holder.contains(uri)) {
                if (holder instanceof MultiClipboardHolder) {
                    ((MultiClipboardHolder) holder).remove(uri);
                } else {
                    holders.remove(i).close();
                }
            }
        }
    }

    @Override
    public URI getURI(Clipboard clipboard) {
        for (ClipboardHolder holder : getHolders()) {
            if (holder instanceof URIClipboardHolder) {
                URIClipboardHolder uriHolder = (URIClipboardHolder) holder;
                URI uri = uriHolder.getURI(clipboard);
                if (uri != null) return uri;
            }
        }
        return null;
    }

    public void add(URIClipboardHolder holder) {
        add((ClipboardHolder) holder);
    }

    @Override
    public boolean contains(Clipboard clipboard) {
        for (ClipboardHolder holder : holders) {
            if (holder.contains(clipboard)) return true;
        }
        return false;
    }

    @Deprecated
    public void add(ClipboardHolder holder) {
        checkNotNull(holder);
        if (holder instanceof URIClipboardHolder) {
            holders.add((URIClipboardHolder) holder);
        } else {
            URI uri = URI.create(UUID.randomUUID().toString());
            if (!contains(uri)) {
                holders.add(new URIClipboardHolder(uri, holder.getClipboard(), holder.getWorldData()));
            }
        }
        cached = null;
    }

    public void add(URI uri, Clipboard clip) {
        checkNotNull(clip);
        checkNotNull(uri);
        add(new URIClipboardHolder(uri, clip, getWorldData()));
    }

    @Override
    public List<Clipboard> getClipboards() {
        ArrayList<Clipboard> all = new ArrayList<>();
        for (ClipboardHolder holder : holders) {
            all.addAll(holder.getClipboards());
        }
        return all;
    }

    @Override
    public List<ClipboardHolder> getHolders() {
        ArrayList<ClipboardHolder> holders = new ArrayList<>();
        for (ClipboardHolder holder : this.holders) {
            holders.addAll(holder.getHolders());
        }
        return holders;
    }

    @Override
    public boolean contains(URI uri) {
        if (getUri().equals(uri)) {
            return true;
        }
        for (URIClipboardHolder uch : holders) {
            if (uch.contains(uri)) return true;
        }
        return false;
    }

    @Override
    public Clipboard getClipboard() {
        Clipboard[] available = cached;
        if (available == null) {
            cached = available = getClipboards().toArray(new Clipboard[0]);
        }
        switch (available.length) {
            case 0:
                return EmptyClipboard.INSTANCE;
            case 1:
                return available[0];
        }

        int index = PseudoRandom.random.nextInt(available.length);
        return available[index];
    }

    @Override
    public Set<URI> getURIs() {
        Set<URI> set = new HashSet<>();
        for (ClipboardHolder holder : getHolders()) {
            if (holder instanceof URIClipboardHolder) {
                URI uri = ((URIClipboardHolder) holder).getUri();
                if (!uri.toString().isEmpty()) set.add(uri);
            }
        }
        return set;
    }

    @Override
    public void close() {
        cached = null;
        for (ClipboardHolder holder : holders) {
            holder.close();
        }
    }
}