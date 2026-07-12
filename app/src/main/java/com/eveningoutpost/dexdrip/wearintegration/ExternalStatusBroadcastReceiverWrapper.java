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
public class ExternalStatusBroadcastReceiverWrapper extends BroadcastReceiver {

    private static volatile ExternalStatusBroadcastReceiver receiverInstance;

    @Override
    public void onReceive(Context context, Intent intent) {
        getReceiverInstance().onReceive(context, intent);
    }

    private static ExternalStatusBroadcastReceiver getReceiverInstance() {
        if (receiverInstance == null) {
            synchronized (ExternalStatusBroadcastReceiverWrapper.class) {
                if (receiverInstance == null) {
                    receiverInstance = new ExternalStatusBroadcastReceiver();
                }
            }
        }
        return receiverInstance;
    }
}
