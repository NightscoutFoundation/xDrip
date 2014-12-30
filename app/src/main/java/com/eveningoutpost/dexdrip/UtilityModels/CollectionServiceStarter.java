package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Services.DexCollectionService;
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
    
    public void start(Context context) {
        mContext = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");

        if(isBTWixel(context)) {
            stopWifWixelThread();
            startBtWixelService();
        } else {
            stopBtWixelService();
            startWifWixelThread();
        }

        Log.d("CollectionServiceStarter ", collection_method);
    }


    private void startBtWixelService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startService(new Intent(mContext, DexCollectionService.class));
    	}
    }
    private void stopBtWixelService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.stopService(new Intent(mContext, DexCollectionService.class));
        }
    }

    private void startWifWixelThread() {
        WixelReader.sStart(mContext);
    }

    private void stopWifWixelThread() {
        WixelReader.sStop();
    }

}
