package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.polidea.rxandroidble.exceptions.BleScanException;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


// jamorham base class for reactive bluetooth services

public abstract class JamBaseBluetoothService extends Service {

    private final PowerManager.WakeLock wl = JoH.getWakeLock("jam-bluetooth-generic", 1000);
    protected String TAG = this.getClass().getSimpleName();
    private volatile boolean background_launch_waiting = false;

    private static long secondsTill(Date retryDateSuggestion) {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.getTime() - System.currentTimeMillis());
    }

    protected String handleBleScanException(BleScanException bleScanException) {
        final String text;

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                text = "Bluetooth is not available";
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                text = "Enable bluetooth and try again";
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                text = "On Android 6.0+ location permission is required. Implement Runtime Permissions";
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                text = "Location services needs to be enabled on Android 6.0+";
                break;
            case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                text = "Scan with the same filters is already started";
                break;
            case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                text = "Failed to register application for bluetooth scan";
                break;
            case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                text = "Scan with specified parameters is not supported";
                break;
            case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                text = "Scan failed due to internal error";
                break;
            case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                text = "Scan cannot start due to limited hardware resources";
                break;
            case BleScanException.UNDOCUMENTED_SCAN_THROTTLE:
                text = String.format(
                        Locale.getDefault(),
                        "Android 7+ does not allow more scans. Try in %d seconds",
                        secondsTill(bleScanException.getRetryDateSuggestion())
                );
                break;
            case BleScanException.UNKNOWN_ERROR_CODE:
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                text = "Unable to start scanning";
                break;
        }
        UserError.Log.w(TAG, text + " " + bleScanException);
        return text;

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
