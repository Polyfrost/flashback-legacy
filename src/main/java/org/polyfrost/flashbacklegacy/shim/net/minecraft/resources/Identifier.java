package org.polyfrost.flashbacklegacy.shim.net.minecraft.resources;

public final class Identifier implements Comparable<Identifier> {
    private final String namespace;
    private final String path;

    private Identifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier("minecraft", path);
    }

    public static Identifier parse(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? withDefaultNamespace(id) : new Identifier(colon == 0 ? "minecraft" : id.substring(0, colon), id.substring(colon + 1));
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Identifier id && namespace.equals(id.namespace) && path.equals(id.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public int compareTo(Identifier o) {
        int c = path.compareTo(o.path);
        return c != 0 ? c : namespace.compareTo(o.namespace);
    }
}
