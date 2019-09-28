package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.util.Log;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

class CustomUpdater {
    private final static String TAG = CustomUpdater.class.getSimpleName();
    private AppUpdater appUpdater;

    void monitorUpdate(Activity activity) {
        Log.d(TAG, "activating CustomUpdater from URL=" + BuildConfig.XDRIP_UPDATER_URL);
        appUpdater = new AppUpdater(activity)
                .setDisplay(Display.NOTIFICATION)
                .setUpdateFrom(UpdateFrom.JSON);
        appUpdater.setUpdateJSON(BuildConfig.XDRIP_UPDATER_URL);
//        appUpdater.showAppUpdated(true);
        appUpdater.start();
    }
}