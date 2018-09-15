package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.activeandroid.ActiveAndroid;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.LibreData;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.PlusAsyncExecutor;
import com.eveningoutpost.dexdrip.UtilityModels.VersionTracker;

import static com.eveningoutpost.dexdrip.utils.VersionFixer.disableUpdates;

//import io.fabric.sdk.android.Fabric;

/**
 * Created by Emma Black on 3/21/15.
 */

public class xdrip extends Application {

    private static final String TAG = "xdrip.java";
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public static PlusAsyncExecutor executor;
    private static boolean fabricInited = false;

    @Override
    public void onCreate() {
        xdrip.context = getApplicationContext();
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        ActiveAndroid.initialize(this);
        updateMigrations();
        DemiGod.isPresent();
        JoH.forceBatteryWhitelisting();
        executor = new PlusAsyncExecutor();
        VersionTracker.updateDevice();
        disableUpdates();
    }

    public static Context getAppContext() {
        return xdrip.context;
    }

    public static boolean checkAppContext(Context context) {
        if (getAppContext() == null) {
            xdrip.context = context;
            return false;
        } else {
            return true;
        }
    }

    private static void updateMigrations() {
        Sensor.InitDb(context);//ensure database has already been initialized
        BgReading.updateDB();
        LibreBlock.updateDB();
        LibreData.updateDB();
    }

}
