package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.StringRes;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Reminder;
import com.eveningoutpost.dexdrip.alert.Poller;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.services.BluetoothGlucoseMeter;
import com.eveningoutpost.dexdrip.services.MissedReadingService;
import com.eveningoutpost.dexdrip.services.PlusSyncService;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.IdempotentMigrations;
import com.eveningoutpost.dexdrip.utilitymodels.PlusAsyncExecutor;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.VersionTracker;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utils.AppCenterCrashReporting;
import com.eveningoutpost.dexdrip.utils.NewRelicCrashReporting;
import com.eveningoutpost.dexdrip.utils.jobs.DailyJob;
import com.eveningoutpost.dexdrip.utils.jobs.XDripJobCreator;
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry;
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.services.broadcastservice.BroadcastEntry;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.evernote.android.job.JobManager;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Locale;



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
        JodaTimeAndroid.init(this);
        try {
            if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_crashlytics", true)) {
                //NewRelicCrashReporting.start();
                AppCenterCrashReporting.start(this);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        executor = new PlusAsyncExecutor();

        IdempotentMigrations.migrateOOP2CalibrationPreferences(); // needs to run before preferences get defaults

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


        JobManager.create(this).addJobCreator(new XDripJobCreator());
        DailyJob.schedule();
        //SyncService.startSyncServiceSoon();

        if (!isRunningTest()) {
            MissedReadingService.delayedLaunch();
            NFCReaderX.handleHomeScreenScanPreference(getApplicationContext());
            AlertType.fromSettings(getApplicationContext());
            //new CollectionServiceStarter(getApplicationContext()).start(getApplicationContext());
            CollectionServiceStarter.restartCollectionServiceBackground();
            PlusSyncService.startSyncService(context, "xdrip.java");
            if (Pref.getBoolean("motion_tracking_enabled", false)) {
                ActivityRecognizedService.startActivityRecogniser(getApplicationContext());
            }
            BluetoothGlucoseMeter.startIfEnabled();
            LeFunEntry.initialStartIfEnabled();
            MiBandEntry.initialStartIfEnabled();
            BroadcastEntry.initialStartIfEnabled();
            BlueJayEntry.initialStartIfEnabled();
            XdripWebService.immortality();
            VersionTracker.updateDevice();

        } else {
            Log.d(TAG, "Detected running test mode, holding back on background processes");
        }
        Reminder.firstInit(xdrip.getAppContext());
        PluggableCalibration.invalidateCache();
        Poller.init();
    }


    public static synchronized boolean isRunningTest() {
        if (null == isRunningTestCache) {
            boolean test_framework;
            if ("robolectric".equals(Build.FINGERPRINT)) {
                isRunningTestCache = true;
            } else {
                try {
                    Class.forName("android.support.test.espresso.Espresso");
                    test_framework = true;
                } catch (ClassNotFoundException e) {
                    test_framework = false;
                }
                isRunningTestCache = test_framework;
            }
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
        if (Pref.getBoolean("force_english", false)) {
            final String forced_language = Pref.getString("forced_language", "en");
            final String current_language = Locale.getDefault().getLanguage();
            if (!current_language.equals(forced_language)) {
                Log.i(TAG, "Forcing locale: " + forced_language + " was: " + current_language);
                LOCALE = new Locale(forced_language, "", "");
                Locale.setDefault(LOCALE);
                final Configuration config = context.getResources().getConfiguration();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    config.setLocale(LOCALE);
                } else {
                    config.locale = LOCALE;
                }
                try {
                    ((Application) context).getBaseContext().getResources().updateConfiguration(config, ((Application) context).getBaseContext().getResources().getDisplayMetrics());
                } catch (ClassCastException e) {
                    //
                }
                try {
                    context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
                } catch (ClassCastException e) {
                    //

                }
            }
            Log.d(TAG, "Already set to locale: " + forced_language);
        }
    }

    // force language on oreo activities
    public static Context getLangContext(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Pref.getBooleanDefaultFalse("force_english")) {
                final String forced_language = Pref.getString("forced_language", "en");
                final Configuration config = context.getResources().getConfiguration();

                if (LOCALE == null) LOCALE = new Locale(forced_language);
                Locale.setDefault(LOCALE);
                config.setLocale(LOCALE);
                context = context.createConfigurationContext(config);
                //Log.d(TAG, "Sending language context for: " + LOCALE);
                return new ContextWrapper(context);
            } else {
                return context;
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception in getLangContext: " + e);
            return context;
        }
    }


    public static String gs(@StringRes final int id) {
        return getAppContext().getString(id);
    }

    public static String gs(@StringRes final int id, String... args) {
        return getAppContext().getString(id, (Object[]) args);
    }

    //}
}
