package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Lightweight manifest-registered receiver.
 *
 * This keeps the registered receiver tiny until a broadcast is actually received,
 * then lazily creates and reuses the real receiver.
 */
public final class NSEmulatorReceiver extends BroadcastReceiver {

    private static volatile NSEmulatorReceiverCore receiverInstance;

    @Override
    public void onReceive(Context context, Intent intent) {
        getReceiverInstance().onReceive(context, intent);
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
