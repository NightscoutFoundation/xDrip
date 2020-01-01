package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import static com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity.AUTO_UPDATE_PREFS_NAME;

class CustomUpdater {
    private final static String TAG = CustomUpdater.class.getSimpleName();
    private AppUpdater appUpdater;

    CustomUpdater(Activity activity) {
        appUpdater = new AppUpdater(activity)
                .removeDashedParts()
                .setDisplay(Display.DIALOG)
                .setButtonDoNotShowAgainClickListener((dialogInterface, i) -> {
                    Log.d(TAG, "user wants to disable future updates");
                    PreferenceManager.getDefaultSharedPreferences(activity)
                            .edit()
                            .putBoolean(AUTO_UPDATE_PREFS_NAME, false)
                            .apply();
                    stopMonitoring();
                })
                .setUpdateFrom(UpdateFrom.JSON);
        appUpdater.setUpdateJSON(BuildConfig.XDRIP_UPDATER_URL);
    }

    void resetAppUpdater(Activity activity) {
        // this missing functionality in AppUpdater library
        // should not be needed because we override setButtonDoNotShowAgainClickListener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("prefAppUpdaterShow", true);
        editor.apply();
    }

    void startMonitoring() {
        Log.d(TAG, "activating CustomUpdater from URL=" + BuildConfig.XDRIP_UPDATER_URL);
        appUpdater.start();
    }

    void stopMonitoring() {
        if (appUpdater != null) {
            appUpdater.stop();
        }
    }

    void manualCheck() {
        if (appUpdater != null) {
            // TODO show the dialog also if no update found - problem that it will be active for monitoring too
//            appUpdater.showAppUpdated(true);
            appUpdater.start();
        }
    }
}