package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

import java.net.URI;
import java.util.Collections;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

public class URIClipboardHolder extends ClipboardHolder {
    private final URI uri;

    public URIClipboardHolder(URI uri, Clipboard clipboard, WorldData worldData) {
        super(clipboard, worldData);
        checkNotNull(uri);
        this.uri = uri;
    }

    public boolean contains(URI uri) {
        checkNotNull(uri);
        return this.uri.equals(uri);
    }

    /**
     * @return The original source of this clipboard (usually a file or url)
     * @deprecated If a holder has multiple sources, this will return an empty URI
     */
    @Deprecated
    public URI getUri() {
        return uri;
    }

    public Set<URI> getURIs() {
        return Collections.singleton(uri);
    }

    public URI getURI(Clipboard clipboard) {
        return getClipboard() == clipboard ? getUri() : null;
    }
}
