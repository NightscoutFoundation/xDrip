package lwld.glucose.profile.util;

import static lwld.glucose.profile.GluProBle.TAG;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.RequiresPermission;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * General utility functions
 */
public class Util {

    public static String dateTimeText(long timestamp) {
        return android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", timestamp).toString();
    }

    public static boolean isValidMacAddress(String mac) {
        return mac != null &&
                mac.matches("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");
    }

    public static BluetoothDevice getDeviceFromMac(String mac) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return null;
        return adapter.getRemoteDevice(mac);
    }

    // note this returns false if bluetooth is not enabled
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static synchronized boolean isDeviceBonded(BluetoothDevice device) {
        Log.d(TAG, "Checking bonding status for device: " + device.getAddress());
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) return false; // ??
            val pairedDevices = adapter.getBondedDevices();
            if ((pairedDevices != null) && (!pairedDevices.isEmpty())) {
                for (BluetoothDevice pairedDevice : pairedDevices) {
                    if (pairedDevice.getAddress().equals(device.getAddress())) {

                        Log.d(TAG, "Device bonded: " + device.getAddress());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Unable to determine which devices are bonded - this will cause problems");
        }
        Log.d(TAG, "Device not bonded: " + device.getAddress());
        return false;
    }

    public static boolean bluetoothEnabled() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Exception trying to determine if adapter is enabled: " + e);
        }
        return false;
    }
}
