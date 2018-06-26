package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Reminder;
import com.eveningoutpost.dexdrip.Services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.Services.BluetoothGlucoseMeter;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.IdempotentMigrations;
import com.eveningoutpost.dexdrip.UtilityModels.PlusAsyncExecutor;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;

import java.util.Locale;

import io.fabric.sdk.android.Fabric;

//import com.bugfender.sdk.Bugfender;

/**
 * Created by Emma Black on 3/21/15.
 */

public class xdrip extends Application {

    private static final String TAG = "xdrip.java";
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static boolean fabricInited = false;
    private static boolean bfInited = false;
    private static Locale LOCALE;
    public static PlusAsyncExecutor executor;
    public static boolean useBF = false;
    private static Boolean isRunningTestCache;


    @Override
    public void onCreate() {
        xdrip.context = getApplicationContext();
        super.onCreate();
        try {
            if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_crashlytics", true)) {
                initCrashlytics(this);
                initBF();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        executor = new PlusAsyncExecutor();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_advanced_settings, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_source, true);
        PreferenceManager.setDefaultValues(this, R.xml.xdrip_plus_defaults, true);
        PreferenceManager.setDefaultValues(this, R.xml.xdrip_plus_prefs, true);

        checkForcedEnglish(xdrip.context);


        JoH.ratelimit("policy-never", 3600); // don't on first load
        new IdempotentMigrations(getApplicationContext()).performAll();


        if (!isRunningTest()) {
            MissedReadingService.delayedLaunch();
            NFCReaderX.handleHomeScreenScanPreference(getApplicationContext());
            AlertType.fromSettings(getApplicationContext());
            new CollectionServiceStarter(getApplicationContext()).start(getApplicationContext());
            PlusSyncService.startSyncService(context, "xdrip.java");
            if (Pref.getBoolean("motion_tracking_enabled", false)) {
                ActivityRecognizedService.startActivityRecogniser(getApplicationContext());
            }
            BluetoothGlucoseMeter.startIfEnabled();
            XdripWebService.immortality();

        } else {
            Log.d(TAG, "Detected running test mode, holding back on background processes");
        }
        Reminder.firstInit(xdrip.getAppContext());
        PluggableCalibration.invalidateCache();
    }

    public synchronized static void initCrashlytics(Context context) {
        if ((!fabricInited) && !isRunningTest()) {
            try {
                Crashlytics crashlyticsKit = new Crashlytics.Builder()
                        .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                        .build();
                Fabric.with(context, crashlyticsKit);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            fabricInited = true;
        }
    }

    public static synchronized boolean isRunningTest() {
        if (null == isRunningTestCache) {
            boolean test_framework;
            try {
                Class.forName("android.support.test.espresso.Espresso");
                test_framework = true;
            } catch (ClassNotFoundException e) {
                test_framework = false;
            }
            isRunningTestCache = test_framework;
        }
        return isRunningTestCache;
    }

    public synchronized static void initBF() {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_bugfender", false)) {
                new Thread() {
                    @Override
                    public void run() {
                        String app_id = PreferenceManager.getDefaultSharedPreferences(xdrip.context).getString("bugfender_appid", "").trim();
                        if (!useBF && (app_id.length() > 10)) {
                            if (!bfInited) {
                                //Bugfender.init(xdrip.context, app_id, BuildConfig.DEBUG);
                                bfInited = true;
                            }
                            useBF = true;
                        }
                    }
                }.start();
            } else {
                useBF = false;
            }
        } catch (Exception e) {
            //
        }
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

    public static void checkForcedEnglish(Context context) {
        //    if (Locale.getDefault() != Locale.ENGLISH) {
        //       Log.d(TAG, "Locale is non-english");
        if (Pref.getBoolean("force_english", false)) {
            final String forced_language = Pref.getString("forced_language", "en");
            final String current_language = Locale.getDefault().getLanguage();
            if (!current_language.equals(forced_language)) {
                Log.i(TAG, "Forcing locale: " + forced_language + " was: " + current_language);
                LOCALE = new Locale(forced_language, "", "");
                Locale.setDefault(LOCALE);
                Configuration config = context.getResources().getConfiguration();
                config.locale = LOCALE;
                try {
                    ((Application) context).getBaseContext().getResources().updateConfiguration(config, ((Application) context).getBaseContext().getResources().getDisplayMetrics());
                } catch (ClassCastException e) {
                    Log.i(TAG, "Using activity context instead of base for Locale change");
                    context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
                }
            }
            Log.d(TAG, "Already set to locale: " + forced_language);
        }
    }

    public static String gs(int id) {
        return getAppContext().getString(id);
    }

    public static String gs(int id, String... args) {
        return getAppContext().getString(id, (Object[]) args);
    }

    //}
}
