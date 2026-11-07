package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class OptionInstance<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    public OptionInstance(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public T get() {
        return getter.get();
    }

    public void set(T value) {
        setter.accept(value);
    }
}
