package dev.ryanhcode.sable.sublevel.render.fancy.task;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelSectionCompiler;
import dev.ryanhcode.sable.sublevel.render.fancy.SubLevelMeshBuilder;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class FancySubLevelTaskScheduler {

    private final SubLevelTask.MeshUploader meshUploader;
    private final Thread[] threads;
    private final AtomicBoolean running;
    private final Lock taskLock;
    private final Condition hasWork;
    private final Queue<Task> tasks;

    public FancySubLevelTaskScheduler(final SubLevelTask.MeshUploader meshUploader, final int threadCount) {
        this.meshUploader = meshUploader;
        this.threads = new Thread[threadCount];
        this.running = new AtomicBoolean(false);
        this.taskLock = new ReentrantLock();
        this.hasWork = this.taskLock.newCondition();
        this.tasks = new PriorityBlockingQueue<>();
    }

    private void runTask() {
        try (final SectionBufferBuilderPack pack = new SectionBufferBuilderPack()) {
            while (true) {
                final Task task;

                try {
                    this.taskLock.lock();
                    task = this.tasks.poll();
                    if (task == null) {
                        if (this.running.get()) {
                            this.hasWork.awaitUninterruptibly();
                            continue;
                        }

                        break;
                    }
                } finally {
                    this.taskLock.unlock();
                }

                try {
                    task.task.process(pack, this.meshUploader);
                } catch (final Throwable t) {
                    Sable.LOGGER.error("Error running sub-level task", t);
                }
                if (task.onComplete != null) {
                    task.onComplete.run();
                }
            }
        }
    }

    public void start() {
        if (!this.running.compareAndSet(false, true)) {
            return;
        }

        for (int i = 0; i < this.threads.length; i++) {
            final Thread thread = new Thread(this::runTask, "FancySubLevelTaskScheduler#" + i);
            thread.setPriority(Thread.NORM_PRIORITY - 2);
            thread.start();
            this.threads[i] = thread;
        }
    }

    public void stop() {
        if (!this.running.compareAndSet(true, false)) {
            return;
        }

        this.hasWork.signalAll();
        for (final Thread thread : this.threads) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                Sable.LOGGER.error("Error shutting down task thread", e);
            }
        }
    }

    public void schedule(final SubLevelTask task, final double distance, @Nullable final Runnable onComplete) {
        if (!this.running.get()) {
            throw new IllegalStateException("SubLevelTaskScheduler is not running");
        }

        try {
            this.taskLock.lock();
            this.tasks.add(new Task(task, distance, onComplete));
            this.hasWork.signal();
        } finally {
            this.taskLock.unlock();
        }
    }

    public void scheduleCompile(final FancySubLevelSectionCompiler.RenderSection section, @Nullable final RenderChunkRegion renderChunkRegion, final double distance, @Nullable final Consumer<FancySubLevelSectionCompiler.RenderSection> onComplete) {
        if (renderChunkRegion == null) {
            section.setCompiledSection(FancySubLevelSectionCompiler.CompiledSection.EMPTY);
            return;
        }

        this.schedule((pack, uploader) -> {
            final SubLevelMeshBuilder.Results results = uploader.getMeshBuilder().compile(section.getOrigin(), section.getPos(), renderChunkRegion, pack);
            section.setCompiledSection(FancySubLevelSectionCompiler.CompiledSection.create(results, uploader));
        }, distance, onComplete != null ? () -> onComplete.accept(section) : null);
    }

    private record Task(SubLevelTask task, double distance, @Nullable Runnable onComplete) implements Comparable<Task> {

        @Override
        public int compareTo(@NotNull final FancySubLevelTaskScheduler.Task o) {
            return Double.compare(this.distance, o.distance);
        }
    }
}
