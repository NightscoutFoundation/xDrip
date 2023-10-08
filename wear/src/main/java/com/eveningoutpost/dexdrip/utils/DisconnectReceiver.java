package com.eveningoutpost.dexdrip.utils;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.models.UserError;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// jamorham

// Handle disconnection intents that may not be related to an established gatt session

// Thanks to Tebbe for the suggestion

public class DisconnectReceiver extends BroadcastReceiver {

    private static final String TAG = DisconnectReceiver.class.getSimpleName();
    private static final ConcurrentHashMap<String, BtCallBack> callbacks = new ConcurrentHashMap<>();

    public static void addCallBack(BtCallBack callback, String name) {
        callbacks.put(name, callback);
    }

    public static void removeCallBack(String name) {
        callbacks.remove(name);
    }

    private synchronized void processCallBacks(String address, String status) {
        for (Map.Entry<String, BtCallBack> entry : callbacks.entrySet()) {
            UserError.Log.d(TAG, "Callback: " + entry.getKey());
            entry.getValue().btCallback(address, status);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            //noinspection ConstantConditions
            if (intent.getAction().equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
                final String address = ((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).getAddress();
                if (address != null) {
                    UserError.Log.d(TAG, "Disconnection notice: " + address);
                    processCallBacks(address, "DISCONNECTED");
                }
            }
        } catch (NullPointerException e) {
            UserError.Log.e(TAG, "NPE in onReceive: " + e);
        }
    }
}
