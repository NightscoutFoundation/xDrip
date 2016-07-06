package com.eveningoutpost.dexdrip;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.IdempotentMigrations;
import com.eveningoutpost.dexdrip.UtilityModels.PlusAsyncExecutor;

import java.util.Locale;

import io.fabric.sdk.android.Fabric;

/**
 * Created by stephenblack on 3/21/15.
 */

public class xdrip extends Application {

    private static Context context;
    public static PlusAsyncExecutor executor;

    @Override
    public void onCreate() {
        super.onCreate();
        xdrip.context = getApplicationContext();
     try {
         if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_crashlytics", true)) {
             Crashlytics crashlyticsKit = new Crashlytics.Builder()
                     .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                     .build();
             Fabric.with(this, crashlyticsKit);
         }
     } catch (Exception e)
     {
         Log.e("xdrip.java", e.toString());
     }
        executor = new PlusAsyncExecutor();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_source, true);
        PreferenceManager.setDefaultValues(this, R.xml.xdrip_plus_prefs, true);

        if (Locale.getDefault()!=Locale.ENGLISH)
        {
            if (Home.getPreferencesBoolean("force_english",false)) {
                Locale.setDefault(Locale.ENGLISH);
                Configuration config = getResources().getConfiguration();
                config.locale = Locale.ENGLISH;
                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            }
        }


        JoH.ratelimit("policy-never", 3600); // don't on first load
        new IdempotentMigrations(getApplicationContext()).performAll();
        AlertType.fromSettings(getApplicationContext());
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(getApplicationContext());
        collectionServiceStarter.start(getApplicationContext());
        PlusSyncService.startSyncService(context, "xdrip.java");
    }

    public static Context getAppContext()
    {
        return xdrip.context;
    }

}
