
package com.eveningoutpost.dexdrip.utils;

import android.app.Application;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.Telemetry;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;
import io.sentry.android.core.SentryAndroidOptions;

import java.util.LinkedHashMap;
import java.util.Map;

// jamorham
public class SentryCrashReporting {
    private static final String TAG = "Sentry";

    /**
     * Ordered mapping of uploader pref key → short code used in Sentry tags.
     * IMPORTANT: When adding a new uploader to xDrip, add a row here too.
     */
    private static final String[][] UPLOADER_PREFS = {
            {"cloud_storage_api_enable",       "NS"},
            {"cloud_storage_mongodb_enable",   "MONGO"},
            {"cloud_storage_influxdb_enable",  "INFLUX"},
            {"cloud_storage_tidepool_enable",  "TIDEPOOL"},
            {"share_upload",                   "SHARE"},
            {"health_connect_enable",          "HEALTH"},
    };
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
                SentryAndroid.init(app, SentryCrashReporting::configureOptions);
                addBreadcrumb(DexCollectionType.getBestCollectorHardwareName(),"BootCollector");
                setTag("BootCollector", DexCollectionType.getBestCollectorHardwareName());

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

    /** Extracted to method to add tests */
    static void configureOptions(SentryAndroidOptions options) {
        options.setDsn(DSN);
        options.enableAllAutoBreadcrumbs(false);
        options.setEnableActivityLifecycleBreadcrumbs(false);
        options.setEnableSystemEventBreadcrumbs(false);
        options.setSendDefaultPii(false);
        // options.setEnableAutoSessionTracking(true);
        // options.setEnableRootCheck(false);
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

    static Map<String, String> buildWeeklyStatusTags() {
        final DexCollectionType collector = DexCollectionType.getDexCollectionType();
        final Map<String, String> tags = new LinkedHashMap<>();
        tags.put("collector_type", collector.toString());
        for (final String[] uploader : UPLOADER_PREFS) {
            if (Pref.getBooleanDefaultFalse(uploader[0])) {
                tags.put("uploader." + uploader[1].toLowerCase(), "true");
            }
        }
        tags.put("wear_sync", String.valueOf(Pref.getBoolean("wear_sync", false)));
        tags.put("local_broadcast", String.valueOf(Pref.getBooleanDefaultFalse("broadcast_service_enabled")));
        tags.put("auto_update", String.valueOf(Pref.getBoolean("auto_update_download", true)));
        return tags;
    }

    static boolean shouldSendWeeklyStatus() {
        return Telemetry.isTelemetryEnabled();
    }

    public static void sendWeeklyStatus() {
        try {
            if (!started) {
                return;
            }
            if (!shouldSendWeeklyStatus()) {
                return;
            }
            if (!JoH.pratelimit("weekly-status", 604800)) {
                return;
            }

            Sentry.withScope(scope -> {
                buildWeeklyStatusTags().forEach(scope::setTag);
                Sentry.captureMessage("weekly-status", SentryLevel.INFO);
            });
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception sending weekly status: " + e);
        }
    }
}
