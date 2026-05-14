package com.eveningoutpost.dexdrip.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.android.gms.wearable.DataMap;

/**
 * Created by jamorham on 21/09/2017.
 */

public abstract class G5BaseService extends Service {


    private final PowerManager.WakeLock wl = JoH.getWakeLock("g5-base-bt", 100);

    public static final int G6_SCALING = 34;
    public static final double G6_REV2_SCALING = 0.0001;

    public static final String G5_FIRMWARE_MARKER = "g5-firmware-";

    public static final String G5_BATTERY_MARKER = "g5-battery-";
    public static final String G5_BATTERY_LEVEL_MARKER = "g5-battery-level-";
    public static final String G5_BATTERY_FROM_MARKER = "g5-battery-from";

    public static final String G5_BATTERY_WEARABLE_SEND = "g5-battery-wearable-send";

    protected static final int G5_LOW_BATTERY_WARNING_DEFAULT = 300;
    protected static final int G6_LOW_BATTERY_WARNING_DEFAULT = 290;
    public static final int ALT_LOW_BATTERY_WARNING_DEFAULT = 275;
    // updated by updateBatteryWarningLevel(), accessed by Ob1DexTransmitterBattery
    public static int LOW_BATTERY_WARNING_LEVEL = G5_LOW_BATTERY_WARNING_DEFAULT;

    public static volatile boolean getBatteryStatusNow = false;
    protected static volatile boolean hardResetTransmitterNow = false;
    public static volatile boolean unBondAndStop = false;

    protected static volatile String lastState = "Not running";
    protected static volatile String lastStateWatch = "Not running";
    protected static volatile long static_last_timestamp = 0;
    protected static volatile long static_last_timestamp_watch = 0;

    protected ForegroundServiceStarter foregroundServiceStarter;
    protected Service service;

    static {
        updateBatteryWarningLevel();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        UserError.Log.d("FOREGROUND", "Current Service: " + service.getClass().getSimpleName());
        startInForeground();
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

    public static void updateBatteryWarningLevel() {
        LOW_BATTERY_WARNING_LEVEL = Pref.getStringToInt("g5-battery-warning-level", G5_LOW_BATTERY_WARNING_DEFAULT);
    }

    protected synchronized void extendWakeLock(long ms) {
        JoH.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    protected synchronized void releaseWakeLock() {
        JoH.releaseWakeLock(wl);
    }

    protected void checkPreferenceKey(final String key, final SharedPreferences prefs) {
        if (key.equals("run_service_in_foreground")) {
            UserError.Log.d("FOREGROUND", "run_service_in_foreground changed!");
            if (prefs.getBoolean("run_service_in_foreground", false)) {
                startInForeground();
                UserError.Log.i(service.getClass().getSimpleName(), "Moving to foreground");
            } else {
                stopInForeground();
                UserError.Log.i(service.getClass().getSimpleName(), "Removing from foreground");
            }
        }
    }

    protected void startInForeground() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
        foregroundServiceStarter.start();
        foregroundStatus();
    }

    protected void stopInForeground() {
        // TODO refuse to stop on oreo+ ?
        if (service != null) {
            service.stopForeground(true);
        } else {
            UserError.Log.e("FOREGROUND", "Cannot stop foreground as service is null");
        }
        foregroundStatus();
    }

    protected void foregroundStatus() {
        Inevitable.task("foreground-status", 2000, () -> UserError.Log.d("FOREGROUND", service.getClass().getSimpleName() + (JoH.isServiceRunningInForeground(service.getClass()) ? " is running in foreground" : " is not running in foreground")));
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

    public static boolean usingG6() {
        return Pref.getBooleanDefaultFalse("using_g6");
    }

    public static void setG6bareBones() {
        Pref.setBoolean("using_g6", true);
        // TODO add initiate bonding true in case gets disabled??
        final int battery_warning_level = Pref.getStringToInt("g5-battery-warning-level", G5_LOW_BATTERY_WARNING_DEFAULT);
        if (battery_warning_level == G5_LOW_BATTERY_WARNING_DEFAULT) {
            Pref.setString("g5-battery-warning-level", "" + G6_LOW_BATTERY_WARNING_DEFAULT);
        }
    }

    public static void setG6Defaults() {
        Pref.setBoolean("use_ob1_g5_collector_service", true);
        Pref.setBoolean("ob1_g5_use_transmitter_alg", true);
        Pref.setBoolean("ob1_g5_fallback_to_xdrip", false);
        Pref.setBoolean("display_glucose_from_plugin", false);
        setG6bareBones();
    }

    public static void setHardResetTransmitterNow() {
        hardResetTransmitterNow = true;
    }

    public static String getLastTwoCharacters(final String txid) {
        if (txid == null) return "NULL";
        return txid.length() > 3 ? txid.substring(txid.length() - 2) : "ERR-" + txid;
    }
}
