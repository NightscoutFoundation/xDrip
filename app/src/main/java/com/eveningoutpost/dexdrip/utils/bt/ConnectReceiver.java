package com.eveningoutpost.dexdrip.utils.bt;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.models.UserError;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// jamorham

public class ConnectReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectReceiver.class.getSimpleName();
    private static final ConcurrentHashMap<String, BtCallBack3> callbacks = new ConcurrentHashMap<>();

    public static void addCallBack(BtCallBack3 callback, String name) {
        callbacks.put(name, callback);
    }

    public static void removeCallBack(String name) {
        callbacks.remove(name);
    }

    private synchronized void processCallBacks(String address, String status, BluetoothDevice device) {
        for (Map.Entry<String, BtCallBack3> entry : callbacks.entrySet()) {
            UserError.Log.d(TAG, "Callback: " + entry.getKey());
            entry.getValue().btCallback3(address, status, null, null, device);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            //noinspection ConstantConditions
            if (intent.getAction().equals("android.bluetooth.device.action.ACL_CONNECTED")) {
                final BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device != null) {
                    final String address = device.getAddress();
                    if (address != null) {
                        UserError.Log.d(TAG, "Connection notice: " + address);
                        processCallBacks(address, "CONNECTED", device);
                    }
                }
            }
        } catch (NullPointerException e) {
            UserError.Log.e(TAG, "NPE in onReceive: " + e);
        }
    }
}
