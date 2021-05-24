package com.eveningoutpost.dexdrip.utils.framework;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.google.common.base.Preconditions;

/**
 * A thread that acquires the wakelock of the given name for the duration it runs,
 * or until the given timeout has elapsed, whichever happens sooner.
 */
public abstract class WakeLockThread extends Thread {

    private final String wakeLockName;
    private final int millis;

    public WakeLockThread(String wakeLockName, int millis) {
        super("WakeLockThread (wakeLock='" + wakeLockName + "', timeout=" + millis + "msec");
        this.wakeLockName = wakeLockName;
        this.millis = millis;
        Preconditions.checkArgument(millis >= 0,
                "Expected millis > 0, got " + millis);
        Preconditions.checkArgument(!wakeLockName.isEmpty(), "Empty wakeLockName");
    }

    /**
     * Calls {@link #runWithWakeLock()} exactly once, with the wakelock held.
     *
     * WakeLock acquisition is attempted via {@link JoH#getWakeLock(String, int)},
     * without attempting to catch any unchecked exceptions that method might throw
     * now or in future (it shouldn't).
     */
    @Override
    public final void run() {
        /*
         * Note that because this class isn't exposing the WakeLock, it's impossible
         * for unit tests to assert that it is actually held during runWithWakeLockHeld(),
         * because there doesn't seem to be a way to get hold of the WakeLock by
         * name without acquiring it.
         * We could store the WakeLock acquired here in an AtomicReference<WakeLock> and
         * expose it via an protected final getter, but that might encourage subclasses
         * to inappropriately interact with the wakelock, so we're not doing it for now.
         */
        PowerManager.WakeLock wakeLock = JoH.getWakeLock(wakeLockName, millis);
        try {
            runWithWakeLock();
        } finally {
            JoH.releaseWakeLock(wakeLock);
        }
    }

    /**
     * Called from {@link #run()} once with the wakelock held, assuming the WakeLock
     * was acquired.
     */
    protected abstract void runWithWakeLock();
}
