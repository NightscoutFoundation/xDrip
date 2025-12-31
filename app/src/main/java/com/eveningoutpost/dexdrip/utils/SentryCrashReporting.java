
package com.eveningoutpost.dexdrip.utils;

import android.app.Application;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import io.sentry.Sentry;
import io.sentry.android.core.SentryAndroid;

// jamorham
public class SentryCrashReporting {
    private static final String TAG = "Sentry";
    private static final String serverDomain = "bluejay.website";
    private static final String segment1 = "643697dfe5994e1b8d2788ede119cd5e";
    private static final String segment2 = "27340ce23ddd103028eaa178a466d768";
    private static final String scheme = "https://";
    private static final String DSN = getDsn();
    private static volatile boolean started = false;
    public synchronized static void start(Application app) {
        try {
            if (started) {
                UserError.Log.e(TAG, "Already started!");
                return;
            }
            started = true;
            if (JoH.pratelimit("crash-reporting-start", 240) ||
                    JoH.pratelimit("crash-reporting-start2", 240)) {
                SentryAndroid.init(app, options -> {
                    options.setDsn(DSN);
                    options.enableAllAutoBreadcrumbs(false);
                    options.setEnableActivityLifecycleBreadcrumbs(false);
                    options.setEnableSystemEventBreadcrumbs(false);
                    // options.setEnableAutoSessionTracking(true);
                    // options.setEnableRootCheck(false);
                    // TODO
                });
                addBreadcrumb(DexCollectionType.getBestCollectorHardwareName(),"BootCollector");
                setTag("BootCollector", DexCollectionType.getBestCollectorHardwareName());
                // Sentry.captureMessage("Hello world");
            } else {
                if (JoH.pratelimit("crash-reporting-start-failure", 3600)) {
                    UserError.Log.wtf(TAG, "Unable to start crash reporter as app is restarting too frequently - if you are a developer then you can ignore this message");
                }
            }
        } catch (Throwable e) {
            if (JoH.pratelimit("crash-reporting-start-exception", 3600)) {
                UserError.Log.wtf(TAG, "Unable to start crash reporter: " + e);
            }
        }
    }

    static String getDsn() {
        return String.format("%s%s@%s.%s/1", scheme, segment1, segment2, serverDomain);
    }

    public static void setTag(String key, String value) {
        try {
            Sentry.setTag(key, value);
        } catch (Exception e) {
            //
        }
    }

    public static void addBreadcrumb(String message, String category) {
        try {
            Sentry.addBreadcrumb(message, category);
        } catch (Exception e) {
            //
        }
    }
}
