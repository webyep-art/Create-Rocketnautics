package dev.ryanhcode.sable.network.client;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.Sable;

import java.util.Queue;

public class SableClientNetworkEventLoop {
    private final Queue<Runnable> pendingRunnables = Queues.newConcurrentLinkedQueue();

    public void tell(final Runnable runnable) {
        this.pendingRunnables.add(runnable);
    }

    public void runAllTasks() {
        while (this.pollTask());
    }

    public boolean pollTask() {
        final Runnable runnable = this.pendingRunnables.peek();
        if (runnable == null) {
            return false;
        } else {
            this.doRunTask(this.pendingRunnables.remove());
            return true;
        }
    }

    protected void doRunTask(final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Exception var3) {
            Sable.LOGGER.error(LogUtils.FATAL_MARKER, "Error executing packet handle task", var3);
        }
    }

    public void clear() {
        this.pendingRunnables.clear();
    }
}
