package com.eveningoutpost.dexdrip.utils.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.lang.reflect.Method;

import androidx.annotation.RequiresApi;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

// jamorham

public class BtReconnect extends BluetoothGattCallback {

    private static final String TAG = "BtReconnect";
    private static final boolean mAutoConnect = false;
    private static BtReconnect instance;

    private static BtReconnect getInstance() {
        if (instance == null) {
            instance = new BtReconnect();
        }
        return instance;
    }

    public static void checkReconnect(final BluetoothDevice device) {
        if (Pref.getBoolean("bluetooth_check_reconnect", true)) {
            UserError.Log.d(TAG, device.getAddress());
            if (!isConnectedToDevice(device.getAddress())) {
                UserError.Log.d(TAG, "We're not connected to: " + device.getAddress() + " when we should be - trying to correct");
                reconnect(device);
            } else {
                UserError.Log.d(TAG, "Seems we are connected as reported");
            }
        }
    }

    private static boolean isConnectedToDevice(final String mac) {
        UserError.Log.d(TAG, "isConnected to device: " + mac);
        if (JoH.emptyString(mac)) {
            return false;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) xdrip.getAppContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }
        boolean foundConnectedDevice = false;
        UserError.Log.d(TAG, "isConnected to device iterate: " + mac);
        for (BluetoothDevice bluetoothDevice : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            //UserError.Log.d(TAG, "Connected device: " + bluetoothDevice.getAddress() + " " + bluetoothDevice.getName());
            if (bluetoothDevice.getAddress().equalsIgnoreCase(mac)) {
                foundConnectedDevice = true;
                break;
            }
        }
        return foundConnectedDevice;
    }


    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        UserError.Log.d(TAG, "Connection state change: " + status + " -> " + newState);
    }

    private static void reconnect(final BluetoothDevice bluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectGattApi26(bluetoothDevice, 7);
        } else {
            connectGattApi21(bluetoothDevice);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static BluetoothGatt connectGattApi26(final BluetoothDevice bluetoothDevice, final int type) {
        try {
            final Method method = bluetoothDevice.getClass().getMethod("connectGatt", Context.class, Boolean.TYPE, BluetoothGattCallback.class, Integer.TYPE, Boolean.TYPE, Integer.TYPE, Handler.class);
            UserError.Log.d(TAG, "Trying reconnect");
            return (BluetoothGatt) method.invoke(bluetoothDevice, null, mAutoConnect, getInstance(), TRANSPORT_LE, Boolean.TRUE, type, null);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Received exception: " + e + " falling back");
            return bluetoothDevice.connectGatt(xdrip.getAppContext(), mAutoConnect, getInstance(), TRANSPORT_LE, type);
        }
    }

    private static BluetoothGatt connectGattApi21(final BluetoothDevice bluetoothDevice) {
        try {
            final Method method = bluetoothDevice.getClass().getMethod("connectGatt", Context.class, Boolean.TYPE, BluetoothGattCallback.class, Integer.TYPE);
            UserError.Log.d(TAG, "Trying connect with api21");
            return (BluetoothGatt) method.invoke(bluetoothDevice, null, mAutoConnect, getInstance(), TRANSPORT_LE);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Connection failed: " + e);
            return bluetoothDevice.connectGatt(xdrip.getAppContext(), mAutoConnect, getInstance());
        }
    }

}
