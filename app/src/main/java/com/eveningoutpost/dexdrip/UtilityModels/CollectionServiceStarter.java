package com.eveningoutpost.dexdrip.UtilityModels;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.Services.WixelReader;

/**
 * Created by stephenblack on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;

    private final static String TAG = CollectionServiceStarter.class.getSimpleName();

    public static boolean isBTWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("BluetoothWixel") == 0) {
            return true;
        }
        return false;
    }
    public static boolean isDexbridgeWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("DexbridgeWixel") == 0) {
            return true;
        }
        return false;
    }
    public static boolean isBTShare(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("DexcomShare") == 0) {
            return true;
        }
        return false;
    }
    public static boolean isWifiWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("WifiWixel") == 0) {
            return true;
        }
        return false;
    }
    public static void newStart(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.start(context);
    }

    public void start(Context context) {
        mContext = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");

        if(isBTWixel(context)||isDexbridgeWixel(context)) {
            Log.d("DexDrip", "Starting bt wixel collector");
            stopWifWixelThread();
            stopBtShareService();
            startBtWixelService();
        } else if(isWifiWixel(context)){
            Log.d("DexDrip", "Starting wifi wixel collector");
            stopBtWixelService();
            stopBtShareService();
            startWifWixelThread();
        } else if(isBTShare(context)) {
            Log.d("DexDrip", "Starting bt share collector");
            stopBtWixelService();
            stopWifWixelThread();
            startBtShareService();
        }
        if(prefs.getBoolean("broadcast_to_pebble", false)){
            startPebbleSyncService();
        }
        startSyncService();
        Log.d(TAG, collection_method);

       // Start logging to logcat
        if(prefs.getBoolean("store_logs",false)) {
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

    public CollectionServiceStarter(Context context) {
        mContext = context;
    }

    public static void restartCollectionService(Context context) {
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopWifWixelThread();
        collectionServiceStarter.start(context);
    }

    private void startBtWixelService() {
        Log.d(TAG, "starting bt wixel service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexCollectionService.class));
    	}
    }
    private void stopBtWixelService() {
        Log.d(TAG, "stopping bt wixel service");
        mContext.stopService(new Intent(mContext, DexCollectionService.class));
    }

    private void startBtShareService() {
        Log.d(TAG, "starting bt share service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexShareCollectionService.class));
        }
    }
    private void startPebbleSyncService() {
        Log.d(TAG, "starting PebbleSync service");
        mContext.startService(new Intent(mContext, PebbleSync.class));
    }
    private void startSyncService() {
        Log.d(TAG, "starting Sync service");
        mContext.startService(new Intent(mContext, SyncService.class));
    }
    private void stopBtShareService() {
        Log.d(TAG, "stopping bt share service");
        mContext.stopService(new Intent(mContext, DexShareCollectionService.class));
    }

    private void startWifWixelThread() {
        WixelReader.sStart(mContext);
    }

    private void stopWifWixelThread() {
        WixelReader.sStop();
    }

}
