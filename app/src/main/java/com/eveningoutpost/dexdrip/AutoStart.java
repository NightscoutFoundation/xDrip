package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.PlusSyncService;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

// jamorham

public class AutoStart extends BroadcastReceiver {

    private static final String TAG = "AutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        // TODO add intent action filter

        try {
            UserError.Log.ueh(TAG, "Device Rebooted - Auto Start: " + intent.getAction());
        } catch (Exception e) {
            //
        }


        try {
            CollectionServiceStarter.restartCollectionServiceBackground();
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Failed to start collector: " + e);
        }


        try {
            PlusSyncService.startSyncService(context, "AutoStart");

        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Failed to start sync service: " + e);
        }


        try {
            if (Pref.getBooleanDefaultFalse("show_home_on_boot")) {
                Inevitable.task("show_home_on_boot", 5000, new Runnable() {
                    @Override
                    public void run() {
                        Home.startHomeWithExtra(xdrip.getAppContext(), "auto-start", "start");
                    }
                });
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Failed to start home: " + e);
        }
    }
}
