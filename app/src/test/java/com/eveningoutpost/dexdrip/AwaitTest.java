package com.eveningoutpost.dexdrip;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Behavioural tests for {@link Await}.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class AwaitTest extends RobolectricTestWithConfig {

    // ===== Returns as soon as the condition holds ================================================

    @Test
    public void awaitAtMost_returnsOnTheAttemptWhereTheConditionFirstHolds() {
        // :: Setup — the condition becomes true on the third evaluation
        final AtomicInteger evaluations = new AtomicInteger();

        // :: Act
        final long startNanos = System.nanoTime();
        Await.awaitAtMost(() -> evaluations.incrementAndGet() >= 3, 2000);
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        // :: Verify — it stopped evaluating immediately, well inside the cap
        assertThat(evaluations.get()).isEqualTo(3);
        assertThat(elapsedMs).isLessThan(500L);
    }

    // ===== Gives up at the cap ===================================================================

    @Test
    public void awaitAtMost_returnsAtTheCap_whenTheConditionNeverHolds() {
        // :: Act
        final long startNanos = System.nanoTime();
        Await.awaitAtMost(() -> false, 100);
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        // :: Verify — it waited out the cap and returned rather than throwing
        assertThat(elapsedMs).isAtLeast(100L);
        assertThat(elapsedMs).isLessThan(1500L);
    }
}
