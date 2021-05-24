package com.eveningoutpost.dexdrip.UtilityModels;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;

// jamorham

// run a thread at the lowest priority with wake-locking
public class LowPriorityThread extends Thread {

    private final String name;

    public LowPriorityThread(final Runnable main, final String name) {
        super(main);
        this.name = name;

        this.setPriority(Thread.MIN_PRIORITY);
    }

    // TODO: Make final and just call runnable.run() instead of super.run().
    // This is because any subclass that wants to override this method would lose
    // the wakelock functionality, or else have to call super.run() at which point
    // it would be pointless to have the subclass. Alternatively, this whole class
    // should be final.
    @Override
    public void run() {
        final PowerManager.WakeLock wl = JoH.getWakeLock(name, 60000);
        try {
            super.run();
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }
}


