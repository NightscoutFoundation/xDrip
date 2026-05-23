package com.eveningoutpost.dexdrip.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.models.UserError;

/**
 * Caches BLE permission check results to avoid calling PackageManager during
 * scan/connect operations. Refreshed once per service wake cycle (~5 min).
 * Falls back to a live PackageManager check when the cache is cold or stale.
 */
public class BtPermissionCache {

    private static final String TAG = "BtPermissionCache";
    private static final long MAX_CACHE_AGE_MS = 10 * 60 * 1000L;

    private static volatile boolean cachedScan = false;
    private static volatile boolean cachedConnect = false;
    private static volatile boolean cachedLocation = false;
    private static volatile long lastRefreshedMs = 0;

    /**
     * Read all relevant permissions from PackageManager and store the results.
     * Call this early in the service start path, while the system is healthy.
     */
    public static void refresh(final Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                cachedScan = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
                cachedConnect = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            } else {
                cachedScan = true;
                cachedConnect = true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cachedLocation = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            } else {
                cachedLocation = true;
            }
            lastRefreshedMs = System.currentTimeMillis();
            UserError.Log.d(TAG, "Refreshed: scan=" + cachedScan
                    + " connect=" + cachedConnect + " location=" + cachedLocation);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Failed to refresh permission cache: " + e);
        }
    }

    private static boolean isFresh() {
        return lastRefreshedMs > 0
                && (System.currentTimeMillis() - lastRefreshedMs) < MAX_CACHE_AGE_MS;
    }

    /** Returns cached BLUETOOTH_SCAN grant state; falls back to live check when cache is stale. */
    public static boolean isBluetoothScanGranted(final Context context) {
        if (isFresh()) return cachedScan;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Returns cached BLUETOOTH_CONNECT grant state; falls back to live check when cache is stale. */
    public static boolean isBluetoothConnectGranted(final Context context) {
        if (isFresh()) return cachedConnect;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Returns cached ACCESS_FINE_LOCATION grant state; falls back to live check when cache is stale. */
    public static boolean isLocationGranted(final Context context) {
        if (isFresh()) return cachedLocation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
