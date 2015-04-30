package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.WixelReader;

/**
 * Created by stephenblack on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;

    public static boolean isBTWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("BluetoothWixel") == 0) {
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

        if(isBTWixel(context)) {
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
        Log.d("ColServiceStarter", collection_method);
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
        Log.d("ColServiceStarter", "starting bt wixel service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexCollectionService.class));
    	}
    }
    private void stopBtWixelService() {
        Log.d("ColServiceStarter", "stopping bt wixel service");
        mContext.stopService(new Intent(mContext, DexCollectionService.class));
    }
    private void startBtShareService() {
        Log.d("ColServiceStarter", "starting bt share service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexShareCollectionService.class));
        }
    }
    private void stopBtShareService() {
        Log.d("ColServiceStarter", "stopping bt share service");
        mContext.stopService(new Intent(mContext, DexShareCollectionService.class));
    }

    private void startWifWixelThread() {
        WixelReader.sStart(mContext);
    }

    private void stopWifWixelThread() {
        WixelReader.sStop();
    }

}
