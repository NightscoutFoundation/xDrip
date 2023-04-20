package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.JoH;

// jamorham

// run a thread at the lowest priority with wake-locking

public class LowPriorityThread extends Thread {

    private final String name;

    public LowPriorityThread(final Runnable main, final String name) {
        super(main);
        this.name = name;

        this.setPriority(Thread.MIN_PRIORITY);
    }

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


