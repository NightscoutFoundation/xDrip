package lwld.glucose.profile.util;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JamOrHam
 * <p>
 * Typed job scheduler for background thread
 */

public final class JobScheduler implements AutoCloseable {
    private final HandlerThread thread;
    private final Handler bg;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public enum JobToken {
        READ_CHARACTERISTICS,
        RECONNECT,
        SCAN_REFRESH,
        SCAN_CANCEL,
    }

    public JobScheduler() {
        thread = new HandlerThread("GluPro-JobScheduler");
        thread.start();
        bg = new Handler(thread.getLooper());
    }

    /**
     * Stop all pending work, clear the queue, and terminate the backing thread.
     * After calling this, the instance must not be used.
     */
    public void shutdownNow() {
        if (!closed.compareAndSet(false, true)) return;

        // Drop everything pending in the queue (all tokens, all runnables).
        bg.removeCallbacksAndMessages(null);

        // Terminate the looper thread.
        thread.quitSafely();
    }

    /**
     * Alias for try-with-resources / standard close semantics.
     */
    @Override
    public void close() {
        shutdownNow();
    }

    /**
     * Post a job, deduping by token (only one pending instance per token).
     */
    public void postDeduped(@NonNull Object token, @NonNull Runnable job) {
        if (closed.get()) return;
        bg.removeCallbacksAndMessages(token);
        bg.postAtTime(job, token, android.os.SystemClock.uptimeMillis());
    }

    /**
     * Post a job in the future, deduping by token.
     */
    public void postDedupedDelayed(@NonNull Object token, @NonNull Runnable job, long delayMs) {
        if (closed.get()) return;
        bg.removeCallbacksAndMessages(token);
        bg.postAtTime(job, token, android.os.SystemClock.uptimeMillis() + delayMs);
    }

    /**
     * Cancel any pending jobs of that type.
     */
    public void cancel(@NonNull Object token) {
        if (closed.get()) return;
        bg.removeCallbacksAndMessages(token);
    }
}