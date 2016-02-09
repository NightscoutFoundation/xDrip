package com.eveningoutpost.dexdrip;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

//import com.crashlytics.android.Crashlytics;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.IdempotentMigrations;

//import io.fabric.sdk.android.Fabric;

/**
 * Created by stephenblack on 3/21/15.
 */

public class xdrip extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
      //  Fabric.with(this, new Crashlytics());
        xdrip.context = getApplicationContext();
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(getApplicationContext());
        collectionServiceStarter.start(getApplicationContext());
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_source, false);
        PreferenceManager.setDefaultValues(this, R.xml.xdrip_plus_prefs, false);
        new IdempotentMigrations(getApplicationContext()).performAll();
        Home.set_is_follower();
        PlusSyncService.startSyncService(context, "xdrip.java");
    }

    public static Context getAppContext()
    {
        return xdrip.context;
    }

}
