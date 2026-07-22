package com.eveningoutpost.dexdrip.wearintegration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Lightweight manifest-registered receiver.
 *
 * This keeps the registered receiver tiny until a broadcast is actually received,
 * then lazily creates and reuses the real receiver.
 */
public class ExternalStatusBroadcastReceiver extends BroadcastReceiver {

    private static volatile ExternalStatusBroadcastReceiverCore receiverInstance;

    @Override
    public void onReceive(Context context, Intent intent) {
        getReceiverInstance().onReceive(context, intent);
    }

    private static ExternalStatusBroadcastReceiverCore getReceiverInstance() {
        if (receiverInstance == null) {
            synchronized (ExternalStatusBroadcastReceiver.class) {
                if (receiverInstance == null) {
                    receiverInstance = new ExternalStatusBroadcastReceiverCore();
                }
            }
        }
        return receiverInstance;
    }
}
