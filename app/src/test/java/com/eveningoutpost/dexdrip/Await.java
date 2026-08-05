package com.eveningoutpost.dexdrip;

import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import java.util.function.BooleanSupplier;

/**
 * Waits for a condition to hold instead of sleeping for a fixed duration.
 * <p>
 * Retrofit delivers {@code enqueue()} callbacks on a background OkHttp thread and posts the
 * result to the Android main looper. Robolectric's PAUSED looper does not run on its own, so
 * a test must drain it before the effect of a callback is visible. Sleeping a fixed 300 ms and
 * draining once is a bet that 300 ms is enough — a bet that does not hold on a loaded machine.
 * <p>
 * <b>This class never throws and never asserts.</b> The condition it waits on is normally the
 * same one the test is about to assert, so throwing here would pre-empt the real assertion and
 * replace a precise Truth message ("expected 88.0 but was 65.0") with an uninformative timeout.
 * Waiting is an optimisation; the assertion that follows remains the only oracle.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public final class Await {

    /**
     * Generous on purpose. Nothing is paid when the condition holds quickly, so a high cap
     * costs nothing in the normal case while tolerating a slow or heavily loaded machine.
     */
    private static final long DEFAULT_TIMEOUT_MS = 2000L;

    private static final long POLL_INTERVAL_MS = 5L;

    private Await() {
    }

    /** Waits up to {@link #DEFAULT_TIMEOUT_MS} for the condition to hold. */
    public static void awaitAtMost(final BooleanSupplier condition) {
        awaitAtMost(condition, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Drains the main looper and re-evaluates the condition every {@value #POLL_INTERVAL_MS} ms
     * until it holds or the cap elapses. Returns normally in both cases.
     */
    public static void awaitAtMost(final BooleanSupplier condition, final long timeoutMs) {
        // System.nanoTime(), not JoH.tsl(): this measures real wall clock. JoH.tsl() can be
        // moved by ShadowSystemClock — PersistTest advances it by 100 hours — and a cap
        // measured against a frozen or fast-forwarded clock does not work.
        final long deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L;
        while (true) {
            shadowOf(Looper.getMainLooper()).idle();
            if (condition.getAsBoolean()) return;
            if (System.nanoTime() >= deadlineNanos) return;
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
