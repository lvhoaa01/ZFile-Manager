package com.zfile.manager.core;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton owning the app's background executors.
 *
 * <ul>
 *   <li><b>ioExecutor</b> — bounded pool for general I/O (scan, copy, move, zip).
 *       Core 4 / max 8 to match a typical mid-range device's I/O concurrency.</li>
 *   <li><b>searchExecutor</b> — single thread; queue is cleared on each submission
 *       so a fast-typing user invalidates in-flight searches instead of queueing them.</li>
 *   <li><b>scheduledExecutor</b> — periodic tasks (recycle-bin cleanup, etc.).</li>
 * </ul>
 *
 * <p>Tasks must use {@link androidx.lifecycle.MutableLiveData#postValue} to publish
 * results to observers — never touch UI directly from these threads.</p>
 */
public final class ThreadPoolManager {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 8;
    private static final long KEEP_ALIVE_SECONDS = 60L;

    private static volatile ThreadPoolManager instance;

    @NonNull private final ThreadPoolExecutor ioExecutor;
    @NonNull private final ThreadPoolExecutor searchExecutor;
    @NonNull private final ScheduledExecutorService scheduledExecutor;

    private ThreadPoolManager() {
        ioExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("zfile-io")
        );
        ioExecutor.allowCoreThreadTimeOut(false);

        searchExecutor = new ThreadPoolExecutor(
                1, 1,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("zfile-search")
        );
        searchExecutor.allowCoreThreadTimeOut(true);

        scheduledExecutor = Executors.newScheduledThreadPool(
                1, new NamedThreadFactory("zfile-scheduled"));
    }

    @NonNull
    public static ThreadPoolManager getInstance() {
        ThreadPoolManager local = instance;
        if (local == null) {
            synchronized (ThreadPoolManager.class) {
                local = instance;
                if (local == null) {
                    local = new ThreadPoolManager();
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Fire-and-forget on the I/O pool. */
    public void execute(@NonNull Runnable task) {
        ioExecutor.execute(task);
    }

    /** Submit an I/O task that returns a value. */
    @NonNull
    public <T> Future<T> submitIO(@NonNull Callable<T> callable) {
        return ioExecutor.submit(callable);
    }

    /** Submit an I/O Runnable for cancellation tracking. */
    @NonNull
    public Future<?> submitIO(@NonNull Runnable runnable) {
        return ioExecutor.submit(runnable);
    }

    /**
     * Submit a search task. Drains the search queue first so a newer query
     * supersedes any pending older query — only the in-flight one (if any) keeps running.
     */
    @NonNull
    public <T> Future<T> submitSearch(@NonNull Callable<T> callable) {
        searchExecutor.getQueue().clear();
        return searchExecutor.submit(callable);
    }

    @NonNull
    public Future<?> submitSearch(@NonNull Runnable runnable) {
        searchExecutor.getQueue().clear();
        return searchExecutor.submit(runnable);
    }

    @NonNull
    public ScheduledFuture<?> schedule(@NonNull Runnable runnable, long delay, @NonNull TimeUnit unit) {
        return scheduledExecutor.schedule(runnable, delay, unit);
    }

    @NonNull
    public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable runnable,
                                                  long initialDelay,
                                                  long period,
                                                  @NonNull TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(runnable, initialDelay, period, unit);
    }

    /** Best-effort shutdown — called from {@code Application.onTerminate} on emulators. */
    public void shutdown() {
        ioExecutor.shutdownNow();
        searchExecutor.shutdownNow();
        scheduledExecutor.shutdownNow();
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        @NonNull private final String prefix;
        @NonNull private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(@NonNull String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
