package org.polyfrost.flashbacklegacy.shim.net.minecraft.server.packs.resources;

import java.util.Optional;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

public interface ResourceManager extends ResourceProvider {
    ResourceManager CLASSPATH = id -> {
        String path = "assets/" + id.getNamespace() + "/" + id.getPath();
        return Optional.ofNullable(ResourceManager.class.getClassLoader().getResource(path)).map(Resource::new);
    };

    Optional<Resource> getResource(Identifier id);
}
