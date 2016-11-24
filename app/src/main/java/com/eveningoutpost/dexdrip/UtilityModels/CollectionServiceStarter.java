package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.DailyIntentService;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.DoNothingService;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.Services.WifiCollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.IOException;
import java.util.Calendar;

/**
 * Created by stephenblack on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;

    private final static String TAG = CollectionServiceStarter.class.getSimpleName();

    public static boolean isFollower(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("dex_collection_method", "").equals("Follower");
    }

    public static boolean isWifiandBTWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("WifiBlueToothWixel") == 0) {
            return true;
        }
        return false;
    }

    // are we in the specifc mode supporting wifi and dexbridge at the same time
    public static boolean isWifiandDexBridge()
    {
        return (DexCollectionType.getDexCollectionType() == DexCollectionType.WifiDexBridgeWixel);
    }

    // are we in any mode which supports dexbridge
    public static boolean isDexBridgeOrWifiandDexBridge()
    {
        return isWifiandDexBridge() || isDexbridgeWixel(xdrip.getAppContext());
    }

    public static boolean isBTWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        return isBTWixel(collection_method);
    }

    public static boolean isBTWixel(String collection_method) {
        return collection_method.equals("BluetoothWixel")
                || collection_method.equals("LimiTTer");
    }

    public static boolean isDexbridgeWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexbridgeWixel") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isDexbridgeWixel(String collection_method) {
        return collection_method.equals("DexbridgeWixel");
    }

    public static boolean isBTShare(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomShare") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isBTShare(String collection_method) {
        return collection_method.equals("DexcomShare");
    }

    public static boolean isBTG5(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomG5") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isBTG5(String collection_method) {
        return collection_method.equals("DexcomG5");
    }

    public static boolean isWifiWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("WifiWixel") == 0) {
            return true;
        }
        return false;
    }

        /*
     * LimiTTer emulates a BT-Wixel and works with the BT-Wixel service.
     * It would work without any changes but in some cases knowing that the data does not
     * come from a Dexcom sensor but from a Libre sensor might enhance the performance.
     * */

    public static boolean isLimitter() {
        return Home.getPreferencesStringDefaultBlank("dex_collection_method").equals("LimiTTer");
    }

    public static boolean isWifiWixel(String collection_method) {
        return collection_method.equals("WifiWixel");
    }

    public static boolean isFollower(String collection_method) {
        return collection_method.equals("Follower");
    }

    public static void newStart(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.start(context);
    }

    public void start(Context context, String collection_method) {
        this.mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        if (isBTWixel(collection_method) || isDexbridgeWixel(collection_method)) {
            Log.d("DexDrip", "Starting bt wixel collector");
            stopWifWixelThread();
            stopBtShareService();
            stopFollowerThread();
            stopG5ShareService();

            startBtWixelService();
        } else if (isWifiWixel(collection_method)) {
            Log.d("DexDrip", "Starting wifi wixel collector");
            stopBtWixelService();
            stopFollowerThread();
            stopBtShareService();
            stopG5ShareService();

            startWifWixelThread();
        } else if (isBTShare(collection_method)) {
            Log.d("DexDrip", "Starting bt share collector");
            stopBtWixelService();
            stopFollowerThread();
            stopWifWixelThread();
            stopG5ShareService();

            startBtShareService();

        } else if (isBTG5(collection_method)) {
            Log.d("DexDrip", "Starting G5 share collector");
            stopBtWixelService();
            stopWifWixelThread();
            stopBtShareService();

            if (prefs.getBoolean("wear_sync", false)) {//KS
                boolean enable_wearG5 = prefs.getBoolean("enable_wearG5", false);
                boolean force_wearG5 = prefs.getBoolean("force_wearG5", false);
                this.mContext.startService(new Intent(context, WatchUpdaterService.class));
                if (!enable_wearG5 || (enable_wearG5 && !force_wearG5)) { //don't start if Wear G5 Collector Service is active
                    startBtG5Service();
                }
            }
            else {
                startBtG5Service();
            }

        } else if (isWifiandBTWixel(context) || isWifiandDexBridge()) {
            Log.d("DexDrip", "Starting wifi and bt wixel collector");
            stopBtWixelService();
            stopFollowerThread();
            stopWifWixelThread();
            stopBtShareService();
            stopG5ShareService();

            // start both
            Log.d("DexDrip", "Starting wifi wixel collector first");
            startWifWixelThread();
            Log.d("DexDrip", "Starting bt wixel collector second");
            startBtWixelService();
            Log.d("DexDrip", "Started wifi and bt wixel collector");
        } else if (isFollower(collection_method)) {
            stopWifWixelThread();
            stopBtShareService();
            stopBtWixelService();
            stopG5ShareService();

            startFollowerThread();
        }

        if (prefs.getBoolean("broadcast_to_pebble", false) && (PebbleUtil.getCurrentPebbleSyncType(prefs) != 1)) {
            startPebbleSyncService();
        }

        startSyncService();
        startDailyIntentService();
        Log.d(TAG, collection_method);

        // Start logging to logcat
        if (prefs.getBoolean("store_logs", false)) {
            String filePath = Environment.getExternalStorageDirectory() + "/xdriplogcat.txt";
            try {
                String[] cmd = {"/system/bin/sh", "-c", "ps | grep logcat  || logcat -f " + filePath +
                        " -v threadtime AlertPlayer:V com.eveningoutpost.dexdrip.Services.WixelReader:V *:E "};
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e2) {
                Log.e(TAG, "running logcat failed, is the device rooted?", e2);
            }
        }

    }

    public void start(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");

        start(context, collection_method);
    }

    public CollectionServiceStarter(Context context) {
        this.mContext = context;
    }

    public static void restartCollectionService(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopWifWixelThread();
        collectionServiceStarter.stopFollowerThread();
        collectionServiceStarter.stopG5ShareService();
        collectionServiceStarter.start(context);
    }

    public static void restartCollectionService(Context context, String collection_method) {
        Log.d(TAG, "restartCollectionService: " + collection_method);
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopWifWixelThread();
        collectionServiceStarter.stopFollowerThread();
        collectionServiceStarter.stopG5ShareService();
        collectionServiceStarter.start(context, collection_method);
    }

    private void startBtWixelService() {
        Log.d(TAG, "starting bt wixel service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mContext.startService(new Intent(this.mContext, DexCollectionService.class));
        }
    }

    private void stopBtWixelService() {
        Log.d(TAG, "stopping bt wixel service");
        this.mContext.stopService(new Intent(this.mContext, DexCollectionService.class));
    }

    private void startBtShareService() {
        Log.d(TAG, "starting bt share service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mContext.startService(new Intent(this.mContext, DexShareCollectionService.class));
        }
    }

    private void startBtG5Service() {
        Log.d(TAG, "starting G5 share service");
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
        G5CollectionService.keep_running = true;
        this.mContext.startService(new Intent(this.mContext, G5CollectionService.class));
        //}
    }

    private void startPebbleSyncService() {
        Log.d(TAG, "starting PebbleWatchSync service");
        this.mContext.startService(new Intent(this.mContext, PebbleWatchSync.class));
    }

    private void startSyncService() {
        Log.d(TAG, "starting Sync service");
        this.mContext.startService(new Intent(this.mContext, SyncService.class));
    }

    private void startDailyIntentService() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        PendingIntent pi = PendingIntent.getService(this.mContext, 0, new Intent(this.mContext, DailyIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) this.mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    private void stopBtShareService() {
        Log.d(TAG, "stopping bt share service");
        this.mContext.stopService(new Intent(this.mContext, DexShareCollectionService.class));
    }

    private void startWifWixelThread() {
        Log.d(TAG, "starting wifi wixel service");
        this.mContext.startService(new Intent(this.mContext, WifiCollectionService.class));
    }

    private void stopWifWixelThread() {
        Log.d(TAG, "stopping wifi wixel service");
        this.mContext.stopService(new Intent(this.mContext, WifiCollectionService.class));
    }

    private void startFollowerThread() {
        Log.d(TAG, "starting follower service");
        this.mContext.startService(new Intent(this.mContext, DoNothingService.class));
    }

    private void stopFollowerThread() {
        Log.d(TAG, "stopping follower service");
        this.mContext.stopService(new Intent(this.mContext, DoNothingService.class));
    }

    private void stopG5ShareService() {
        Log.d(TAG, "stopping G5  service");
        G5CollectionService.keep_running = false; // ensure zombie stays down
        this.mContext.stopService(new Intent(this.mContext, G5CollectionService.class));
    }

}
