package com.eveningoutpost.dexdrip.utils.framework;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests {@link WakeLockThread}.
 */
public class WakeLockThreadTest extends RobolectricTestWithConfig {

    @Test(expected = NullPointerException.class)
    public void constructor_nullWakeLockName() {
        new DoNothingThread(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_emptyWakeLockName() {
        new DoNothingThread("", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeTimeout() {
        new DoNothingThread("name", -1);
    }

    @Test
    public void constructor_zeroTimeout_doesNotThrow() {
        new DoNothingThread("name", 0);
    }

    @Test
    public void constructor_doesNotThrow() {
        new DoNothingThread("name", 0);
    }

    @Test
    public void runRunsOnceAfterThreadStarted() throws InterruptedException {
        final AtomicInteger runCount = new AtomicInteger(0);
        Thread thread = new WakeLockThread("testWakeLock", 10000) {
            @Override
            protected void runWithWakeLock() {
                runCount.incrementAndGet();
            }
        };
        Assert.assertEquals(0, runCount.get());
        thread.start();
        thread.join();
        Assert.assertEquals(1, runCount.get());
    }

    /**
     * Convenience class for test purposes to avoid WakeLockThread having to be non-abstract or
     * test methods needing to implement runWithWakeLock().
     */
    static class DoNothingThread extends WakeLockThread {
        public DoNothingThread(String wakeLockName, int millis) {
            super(wakeLockName, millis);
        }
        @Override protected void runWithWakeLock() { }
    }

}
