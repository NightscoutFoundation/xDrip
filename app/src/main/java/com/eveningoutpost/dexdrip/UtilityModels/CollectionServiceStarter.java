package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Services.DexCollectionService;

/**
 * Created by stephenblack on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;

    public void start(Context context) {
        mContext = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");

        if(collection_method.compareTo("BluetoothWixel") == 0) {
            startBtWixelService();
        }

            Log.d("CollectionServiceStarter ", collection_method);
    }


    private void startBtWixelService() {
        mContext.startService(new Intent(mContext, DexCollectionService.class));
    }
    private void stopBtWixelService() {
        mContext.stopService(new Intent(mContext, DexCollectionService.class));
    }
}
