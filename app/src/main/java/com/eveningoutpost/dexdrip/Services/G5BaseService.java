package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.google.android.gms.wearable.DataMap;

/**
 * Created by jamorham on 21/09/2017.
 */

public abstract class G5BaseService extends Service {

    public static final String G5_FIRMWARE_MARKER = "g5-firmware-";

    public static final String G5_BATTERY_MARKER = "g5-battery-";
    public static final String G5_BATTERY_LEVEL_MARKER = "g5-battery-level-";
    public static final String G5_BATTERY_FROM_MARKER = "g5-battery-from";

    public static final String G5_BATTERY_WEARABLE_SEND = "g5-battery-wearable-send";

    protected static final int LOW_BATTERY_WARNING_LEVEL = Pref.getStringToInt("g5-battery-warning-level", 300);

    public static boolean getBatteryStatusNow = false;

    protected static String lastState = "Not running";
    protected static String lastStateWatch = "Not running";
    protected static long static_last_timestamp = 0;
    protected static long static_last_timestamp_watch = 0;

    public static void setWatchStatus(DataMap dataMap) {
        lastStateWatch = dataMap.getString("lastState", "");
        static_last_timestamp_watch = dataMap.getLong("timestamp", 0);
    }

    public static DataMap getWatchStatus() {
        DataMap dataMap = new DataMap();
        dataMap.putString("lastState", lastState);
        dataMap.putLong("timestamp", static_last_timestamp);
        return dataMap;
    }

    protected static String bondState(int bs) {
        String bondState;
        if (bs == BluetoothDevice.BOND_NONE) {
            bondState = " Unpaired";
        } else if (bs == BluetoothDevice.BOND_BONDING) {
            bondState = " Pairing";
        } else if (bs == BluetoothDevice.BOND_BONDED) {
            bondState = " Paired";
        } else if (bs == 0) {
            bondState = " Startup";
        } else {
            bondState = " Unknown bond state: " + bs;
        }
        return bondState;
    }

    private static boolean runningStringCheck(String lastStateCheck) {
        return lastStateCheck.equals("Not Running") || lastStateCheck.contains("Stop") ? false : true;
    }

    public static boolean isRunning() {
        return runningStringCheck(lastState);
    }
    public static boolean isWatchRunning() {
        return runningStringCheck(lastStateWatch);
    }

    public static void resetTransmitterBatteryStatus() {
        final String transmitterId = Pref.getString("dex_txid", "NULL");
        PersistentStore.setString(G5_BATTERY_MARKER + transmitterId, "");
        PersistentStore.setLong(G5_BATTERY_FROM_MARKER + transmitterId, 0);
        PersistentStore.setLong(G5_BATTERY_LEVEL_MARKER + transmitterId, 0);
        PersistentStore.commit();
    }
}
