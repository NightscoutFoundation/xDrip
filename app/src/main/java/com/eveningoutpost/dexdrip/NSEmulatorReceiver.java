package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Lightweight manifest-registered receiver.
 *
 * This keeps the registered receiver tiny until a broadcast is actually received,
 * then lazily creates and reuses the real receiver.
 */
public final class NSEmulatorReceiver extends BroadcastReceiver {

    private static volatile NSEmulatorReceiverCore receiverInstance;
    private final boolean isStandardInstance; // Identify if this is the code-registered version

    // Default constructor: Used by the Android Manifest
    public NSEmulatorReceiver() {
        this.isStandardInstance = false;
    }

    // Custom constructor: Used when registering in code (Standard path)
    public NSEmulatorReceiver(boolean isStandard) {
        this.isStandardInstance = isStandard;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // If the code-registered version receives data, shut the Manifest door
        if (isStandardInstance) {
            setManifestReceiverEnabled(context, false);
        }

        getReceiverInstance().onReceive(context, intent);
    }

    // The method to enable/disable the Manifest entry
    public static void setManifestReceiverEnabled(Context context, boolean enabled) {
        ComponentName component = new ComponentName(context, NSEmulatorReceiver.class);
        int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        context.getPackageManager().setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP);
    }

    private static NSEmulatorReceiverCore getReceiverInstance() {
        if (receiverInstance == null) {
            synchronized (NSEmulatorReceiver.class) {
                if (receiverInstance == null) {
                    receiverInstance = new NSEmulatorReceiverCore();
                }
            }
        }
        return receiverInstance;
    }
}
