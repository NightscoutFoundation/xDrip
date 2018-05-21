package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

/**
 * Created by Emma Black on 11/3/14.
 */
public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DexDrip", "Service auto starter, starting!");
        CollectionServiceStarter.newStart(context);
        PlusSyncService.startSyncService(context, "AutoStart");

        if (Pref.getBooleanDefaultFalse("show_home_on_boot")) {
            Inevitable.task("show_home_on_boot", 5000, new Runnable() {
                @Override
                public void run() {
                    Home.startHomeWithExtra(xdrip.getAppContext(), "auto-start", "start");
                }
            });
        }
    }
}
