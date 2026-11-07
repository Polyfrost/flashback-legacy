package org.polyfrost.flashbacklegacy.shim.net.minecraft.network.codec;

public interface StreamCodec<B, V> {
    V decode(B buffer);

    void encode(B buffer, V value);

    static <B, V> StreamCodec<B, V> unit(V instance) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buffer) {
                return instance;
            }

            @Override
            public void encode(B buffer, V value) {
                if (!value.equals(instance)) throw new IllegalStateException("Can't encode '" + value + "', expected '" + instance + "'");
            }
        };
    }
}
