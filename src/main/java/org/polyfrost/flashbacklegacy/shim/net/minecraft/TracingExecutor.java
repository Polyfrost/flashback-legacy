package org.polyfrost.flashbacklegacy.shim.net.minecraft;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public record TracingExecutor(ExecutorService service) implements Executor {
    public static final TracingExecutor BACKGROUND = new TracingExecutor(ForkJoinPool.commonPool());

    @Override
    public void execute(Runnable task) {
        service.execute(task);
    }
}
