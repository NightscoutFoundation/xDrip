package com.eveningoutpost.dexdrip.utils;

import android.app.Application;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

// JamOrHam
public class AppCenterCrashReporting {

    private static final String TAG = "AppCenter";

    private static final String APPLICATION_T = "85f" + "04671" + "-2414" + "-4723" + "-bcd9" + "-434a45" + "a2e66e";

    private static volatile boolean started = false;

    public synchronized static void start(Application xdrip) {

        try {
            if (started) {
                UserError.Log.e(TAG, "Already started!");
                return;
            }
            started = true;
            if (JoH.pratelimit("crash-reporting-start", 240) ||
                    JoH.pratelimit("crash-reporting-start2", 240)) {
                AppCenter.start(xdrip, APPLICATION_T, Analytics.class, Crashes.class);
            } else {
                if (JoH.pratelimit("crash-reporting-start-failure", 3600)) {
                    UserError.Log.wtf(TAG, "Unable to start crash reporter as app is restarting too frequently - if you are a developer then you can ignore this message");
                }
            }
        } catch (Throwable e) {
            if (JoH.pratelimit("crash-reporting-start-exception", 3600)) {
                UserError.Log.wtf(TAG, "Unable to start crash reporter: " + e);
            }
        } finally {
            Inevitable.task("Commit-start", 100, () -> {
                try {
                    PersistentStore.commit();
                } catch (Exception e) {
                    //
                }
            });
        }
    }
}
