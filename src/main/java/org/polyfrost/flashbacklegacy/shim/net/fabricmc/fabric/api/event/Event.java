package org.polyfrost.flashbacklegacy.shim.net.fabricmc.fabric.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class Event<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private final T invoker;

    public Event(Function<List<T>, T> invoker) {
        this.invoker = invoker.apply(listeners);
    }

    public void register(T listener) {
        listeners.add(listener);
    }

    public T invoker() {
        return invoker;
    }
}
