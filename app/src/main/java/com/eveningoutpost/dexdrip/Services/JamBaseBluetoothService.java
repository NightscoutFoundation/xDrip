package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.bt.HandleBleScanException;
import com.polidea.rxandroidble.exceptions.BleScanException;


// jamorham base class for reactive bluetooth services

public abstract class JamBaseBluetoothService extends Service {

    private final PowerManager.WakeLock wl = JoH.getWakeLock("jam-bluetooth-generic", 1000);
    protected String TAG = this.getClass().getSimpleName();
    private volatile boolean background_launch_waiting = false;

    protected String handleBleScanException(BleScanException bleScanException) {
        return HandleBleScanException.handle(TAG, bleScanException);
    }

    public void background_automata() {
        background_automata(100);
    }

    public synchronized void background_automata(final int timeout) {
        if (background_launch_waiting) {
            UserError.Log.d(TAG, "Blocked by existing background automata pending");
            return;
        }
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG + "-background", timeout + 1000);
        background_launch_waiting = true;
        new Thread(() -> {
            JoH.threadSleep(timeout);
            background_launch_waiting = false;
            automata();
            JoH.releaseWakeLock(wl);
        }).start();
    }

    protected synchronized void automata() {
        throw new RuntimeException("automata stub");
    }

    protected synchronized void extendWakeLock(long ms) {
        JoH.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    protected void releaseWakeLock() {
        JoH.releaseWakeLock(wl);
    }


    protected class OperationSuccess extends RuntimeException {
        public OperationSuccess(String message) {
            super(message);
            UserError.Log.d(TAG, "Operation Success: " + message);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
