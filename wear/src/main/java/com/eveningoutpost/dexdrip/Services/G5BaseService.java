package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.google.android.gms.wearable.DataMap;

/**
 * Created by jamorham on 21/09/2017.
 */

public abstract class G5BaseService extends Service {


    private final PowerManager.WakeLock wl = JoH.getWakeLock("g5-base-bt", 100);

    public static final String G5_FIRMWARE_MARKER = "g5-firmware-";

    public static final String G5_BATTERY_MARKER = "g5-battery-";
    public static final String G5_BATTERY_LEVEL_MARKER = "g5-battery-level-";
    public static final String G5_BATTERY_FROM_MARKER = "g5-battery-from";

    public static final String G5_BATTERY_WEARABLE_SEND = "g5-battery-wearable-send";

    protected static final int G5_LOW_BATTERY_WARNING_DEFAULT = 300;
    protected static final int G6_LOW_BATTERY_WARNING_DEFAULT = 290;
    protected static int LOW_BATTERY_WARNING_LEVEL = G5_LOW_BATTERY_WARNING_DEFAULT; // updated by updateBatteryWarningLevel()

    public static volatile boolean getBatteryStatusNow = false;
    public static volatile boolean hardResetTransmitterNow = false;

    protected static String lastState = "Not running";
    protected static String lastStateWatch = "Not running";
    protected static long static_last_timestamp = 0;
    protected static long static_last_timestamp_watch = 0;

    static {
        updateBatteryWarningLevel();
    }

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

    protected static void updateBatteryWarningLevel() {
        LOW_BATTERY_WARNING_LEVEL = Pref.getStringToInt("g5-battery-warning-level", G5_LOW_BATTERY_WARNING_DEFAULT);
    }

    protected synchronized void extendWakeLock(long ms) {
        JoH.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    protected synchronized void releaseWakeLock() {
        JoH.releaseWakeLock(wl);
    }

    protected static byte[] nn(final byte[] array) {
        if (array == null) {
            if (JoH.ratelimit("never-null", 60)) {
                UserError.Log.wtf("NeverNullG5Base", "Attempt to pass null!!! " + JoH.backTrace());
                return new byte[1];
            }
        }
        return array;
    }

}
