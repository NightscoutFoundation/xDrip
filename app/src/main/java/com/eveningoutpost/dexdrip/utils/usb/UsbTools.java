package com.eveningoutpost.dexdrip.utils.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.Map;

// jamorham

public class UsbTools {

    private static final String TAG = "UsbTools";
    private final static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public static UsbDevice getUsbDevice(final int vendorId, final int productId, final String search) {

        final UsbManager manager = (UsbManager) xdrip.getAppContext().getSystemService(Context.USB_SERVICE);
        if (manager == null) return null;
        final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        for (Map.Entry<String, UsbDevice> entry : deviceList.entrySet()) {
            final UsbDevice device = entry.getValue();
            if (device.getVendorId() == vendorId && device.getProductId() == productId
                    && device.toString().contains(search)) {
                Log.d(TAG, "Found device: " + entry.getKey() + " " + device.toString());
                return device;
            }
        }
        return null;
    }


    // UsbReceiver helper for one hit permission callback
    public static abstract class PermissionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            onGranted(device);
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                    xdrip.getAppContext().unregisterReceiver(this);
                }
            }
        }

        public abstract void onGranted(UsbDevice device);
    }

    // request permission with callback receiver
    public static void requestPermission(final UsbDevice device, final PermissionReceiver receiver) {
        final UsbManager usbManager = (UsbManager) xdrip.getAppContext().getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Log.d(TAG, "UsbManager is null in requestPermission");
            return;
        }
        final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(xdrip.getAppContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        xdrip.getAppContext().registerReceiver(receiver, filter);
        usbManager.requestPermission(device, mPermissionIntent);
    }

}
