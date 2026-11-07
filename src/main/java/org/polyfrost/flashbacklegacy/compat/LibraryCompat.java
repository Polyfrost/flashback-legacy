package org.polyfrost.flashbacklegacy.compat;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;

public final class LibraryCompat {
    private LibraryCompat() {
    }

    public static boolean isEmpty(JsonObject object) {
        return object.entrySet().isEmpty();
    }

    public static <K, V> CacheBuilder<K, V> expireAfterAccess(CacheBuilder<K, V> builder, Duration duration) {
        return builder.expireAfterAccess(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    public static <K extends Comparable<? super K>, V> NavigableMap<K, V> subMap(NavigableMap<K, V> map, Range<K> range) {
        if (range.hasLowerBound() && range.hasUpperBound()) {
            return map.subMap(range.lowerEndpoint(), range.lowerBoundType() == BoundType.CLOSED, range.upperEndpoint(), range.upperBoundType() == BoundType.CLOSED);
        }
        if (range.hasLowerBound()) return map.tailMap(range.lowerEndpoint(), range.lowerBoundType() == BoundType.CLOSED);
        if (range.hasUpperBound()) return map.headMap(range.upperEndpoint(), range.upperBoundType() == BoundType.CLOSED);
        return map;
    }

    public static byte[] getBytes(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return bytes;
    }
}
