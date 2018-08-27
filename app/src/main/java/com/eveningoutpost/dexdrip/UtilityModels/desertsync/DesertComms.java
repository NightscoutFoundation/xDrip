package com.eveningoutpost.dexdrip.UtilityModels.desertsync;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.DesertSync;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.DESERT_MASTER_UNREACHABLE;
import static com.eveningoutpost.dexdrip.UtilityModels.desertsync.DesertComms.QueueHandler.None;
import static com.eveningoutpost.dexdrip.UtilityModels.desertsync.DesertComms.QueueHandler.Pull;


/**
 * Created by jamorham on 18/08/2018.
 *
 */

public class DesertComms {

    private static final String TAG = DesertComms.class.getSimpleName();
    private static final String PREF_OASISIP = "DesertComms-OasisIP";
    private static OkHttpClient okHttpClient;
    private static final int MAX_RETRIES = 10;
    private static final int COMM_FAILURE_NOTIFICATION_THRESHOLD = 2;
    private static volatile int comms_failures = 0;

    private static final LinkedBlockingDeque<QueueItem> queue = new LinkedBlockingDeque<>();


    public static boolean pushToOasis(String topic, String sender, String payload) {
        if (Home.get_follower()) {
            final String oasisIP = getOasisIP();
            if (oasisIP.length() == 0) return false;

            final String url = HttpUrl.parse(getInitialUrl(oasisIP)).newBuilder()
                    .addPathSegment("sync").addPathSegment("push")
                    .addPathSegment(topic).addPathSegment(sender).addEncodedPathSegment(urlEncode(payload)) // workaround okhttp bug with path encoding containing +
                    .build().toString();

            UserError.Log.d(TAG, url);
            queue.add(new QueueItem(url));
            runInBackground();
        } else if (Home.get_master()) {
            // TODO push to followers
            UserError.Log.d(TAG, "We are master so push to followers TODO");
        }
        return true;

    }

    public static boolean pullFromOasis(final String topic, final long since) {
        final String oasisIP = getOasisIP();
        if (oasisIP.length() == 0) return false;

        final String url = HttpUrl.parse(getInitialUrl(oasisIP)).newBuilder().addPathSegment("sync").addPathSegment("pull")
                .addPathSegment("" + since)
                .addPathSegment(topic)
                .build().toString();

        UserError.Log.d(TAG, url);
        queue.add(new QueueItem(url).setHandler(Pull));
        runInBackground();
        return true;
    }

    private static String getOasisIP() {
        final String stored = PersistentStore.getString(PREF_OASISIP);
        if (stored.length() < 5) {
            return Pref.getString("desert_sync_master_ip", "");
        }
        return stored;
    }

    public static void setOasisIP(String ip) {
        PersistentStore.setString(PREF_OASISIP, ip);

    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private static void runInBackground() {

        new Thread(() -> {
            final PowerManager.WakeLock wl = JoH.getWakeLock("DesertComms send", 60000);
            try {
                final String result = httpNext();
                //UserError.Log.d(TAG, "Result: " + result);
                checkCommsFailures(result == null);
                // TODO log errors if not null and not OK etc
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }).start();

    }

    private static String httpNext() {
        final QueueItem item = queue.getFirst();
        item.retried++;
        UserError.Log.d(TAG, "Next item: " + item.toS());
        final String result = httpGet(item.getUrl(getOasisIP()));
        if (result != null) {
            item.handler.process(result);
        }
        if (result != null || item.expired()) {
            queue.remove(item);
        }
        return result;
    }

    private static String httpGet(String url) {
        if (url == null) return null;
        final String hash = XdripWebService.hashPassword(Pref.getString(DesertSync.PREF_WEBSERVICE_SECRET, ""));
        final Request.Builder builder = new Request.Builder().url(url).addHeader("User-Agent", "xDrip+ Desert Comms");
        if (hash != null) builder.addHeader("api-secret", hash);
        try (Response response = getHttpInstance().newCall(builder.build()).execute()) {
            return response.body().string();
        } catch (IOException | NullPointerException e) {
            return null;
        }
    }

    private static OkHttpClient getHttpInstance() {
        if (okHttpClient == null) {
            final OkHttpClient.Builder b = new OkHttpClient.Builder();
            b.connectTimeout(10, TimeUnit.SECONDS);
            b.readTimeout(40, TimeUnit.SECONDS);
            b.writeTimeout(20, TimeUnit.SECONDS);
            try {
                b.sslSocketFactory(TrustManager.getSSLSocketFactory(), TrustManager.getNaiveTrustManager());
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Failed to set up https!");
            }
            b.hostnameVerifier(TrustManager.getXdripHostVerifier());

            okHttpClient = b.build();
        }
        return okHttpClient;
    }

    // okhttp has an escaping bug when adding a path segment containing + so we do our own encoding
    private static String urlEncode(String source) {
        try {
            return URLEncoder.encode(source, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private static void checkCommsFailures(boolean failed) {
        if (failed) {
            comms_failures++;
            UserError.Log.d(TAG, "Comms failures increased to: " + comms_failures);
            if (comms_failures > COMM_FAILURE_NOTIFICATION_THRESHOLD) {
                if (JoH.pratelimit("desert-check-comms", 1800)) {
                    if (!RouteTools.reachable(getOasisIP())) {
                        UserError.Log.e(TAG, "Oasis master IP appears unreachable: " + getOasisIP());
                        JoH.showNotification("Desert Sync Failure", "Master unreachable: " + getOasisIP() + " changed?", null, DESERT_MASTER_UNREACHABLE, false, true, false);
                    }
                }
            }
        } else {
            if (comms_failures > 0) {
                UserError.Log.d(TAG, "Comms restored after " + comms_failures + " failures");
                if (comms_failures > COMM_FAILURE_NOTIFICATION_THRESHOLD) {
                    JoH.cancelNotification(DESERT_MASTER_UNREACHABLE);
                }
            }
            comms_failures = 0; // reset counter
        }
    }

    private static String getInitialUrl(final String oasisIP) {
        return "http" + (useHTTPS() ? "s" : "") + "://" + oasisIP + ":1758" + (useHTTPS() ? "1" : "0");
    }

    private static boolean useHTTPS() {
        return Pref.getBooleanDefaultFalse("desert_use_https");
    }


    private static class QueueItem {
        @Expose
        final String url;
        @Expose
        final long entryTime;
        @Expose
        int retried = 0;
        @Expose
        QueueHandler handler = None;


        QueueItem(String url) {
            this.url = url;
            entryTime = JoH.tsl();
        }

        QueueItem setHandler(QueueHandler handler) {
            this.handler = handler;
            return this;
        }

        // reprocess url for specified host
        String getUrl(final String host) {
            try {
                return HttpUrl.parse(url).newBuilder().host(host).build().toString();
            } catch (NullPointerException e) {
                return null;
            }
        }

        boolean expired() {
            return JoH.msSince(entryTime) > Constants.DAY_IN_MS;
        }

        String toS() {
            return JoH.defaultGsonInstance().toJson(this);
        }

    }

    enum QueueHandler {
        None,
        Pull;

        void process(final String result) {
            switch (this) {
                case Pull:
                    DesertSync.fromPull(result);
                    break;
            }
        }
    }

}
