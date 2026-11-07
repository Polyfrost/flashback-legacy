package org.polyfrost.flashbacklegacy.shim.net.minecraft.server.packs.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Resource {
    private final URL url;

    public Resource(URL url) {
        this.url = url;
    }

    public InputStream open() throws IOException {
        return url.openStream();
    }
}
