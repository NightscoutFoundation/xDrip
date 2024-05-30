package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.DesertSync;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.eveningoutpost.dexdrip.models.JoH.cancelNotification;
import static com.eveningoutpost.dexdrip.models.JoH.defaultGsonInstance;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.getWakeLock;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.releaseWakeLock;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DESERT_MASTER_UNREACHABLE;
import static com.eveningoutpost.dexdrip.utilitymodels.desertsync.DesertComms.QueueHandler.MasterPing;
import static com.eveningoutpost.dexdrip.utilitymodels.desertsync.DesertComms.QueueHandler.None;
import static com.eveningoutpost.dexdrip.utilitymodels.desertsync.DesertComms.QueueHandler.Pull;
import static com.eveningoutpost.dexdrip.utilitymodels.desertsync.DesertComms.QueueHandler.ToFollower;


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
    private static int backupSpinner = 0;
    private static volatile String lastLoadedIP = null;

    private static final LinkedBlockingDeque<QueueItem> queue = new LinkedBlockingDeque<>();


    public static boolean pushToOasis(String topic, String sender, String payload) {
        if (Home.get_follower()) {
            final String oasisIP = getOasisIP();
            if (oasisIP.length() == 0) return false;

            final String url = HttpUrl.parse(getInitialUrl(oasisIP)).newBuilder()
                    .addPathSegment("sync").addPathSegment("push")
                    .addPathSegment(topic).addPathSegment(sender).addEncodedPathSegment(urlEncode(payload)) // workaround okhttp bug with path encoding containing +
                    .build().toString();

            UserError.Log.d(TAG, "To master: " + url);
            queue.add(new QueueItem(url));
            runInBackground();
        } else if (Home.get_master()) {
            UserError.Log.d(TAG, "We are master so push to followers.");
            for (final String address : DesertSync.getActivePeers()) {
                UserError.Log.d(TAG, "Master attempting to push to follower: " + address);
                final String url = HttpUrl.parse(getInitialUrl(address)).newBuilder()
                        .addPathSegment("sync").addPathSegment("push")
                        .addPathSegment(topic).addPathSegment(sender).addEncodedPathSegment(urlEncode(payload)) // workaround okhttp bug with path encoding containing +
                        .build().toString();

                UserError.Log.d(TAG, "To follower: " + url);
                queue.add(new QueueItem(url).setHandler(ToFollower));

            }
            runInBackground();
        }
        return true;

    }

    public static boolean pullFromOasis(final String topic, final long since) {
        final String oasisIP = getOasisIP();
        if (oasisIP.length() == 0) return false;
        try {
            final String url = HttpUrl.parse(getInitialUrl(oasisIP)).newBuilder().addPathSegment("sync").addPathSegment("pull")
                    .addPathSegment("" + since)
                    .addPathSegment(topic)
                    .build().toString();

            UserError.Log.d(TAG, url);
            queue.add(new QueueItem(url).setHandler(Pull));
            runInBackground();
            return true;
        } catch (NullPointerException e) {
            UserError.Log.e(TAG, "Exception parsing url: -" + oasisIP + "- probably invalid ip");
            return false;
        }
    }

    public static boolean probeOasis(final String topic, final String hint) {
        if (emptyString(hint)) return false;
        if (Home.get_follower()) {
            final String url = HttpUrl.parse(getInitialUrl(hint)).newBuilder().addPathSegment("sync").addPathSegment("id")
                    .addPathSegment(topic)
                    .build().toString();

            UserError.Log.d(TAG, "PROBE: " + url);
            queue.add(new QueueItem(url).setHandler(MasterPing));
            runInBackground();
        } else {
            UserError.Log.e(TAG, "Probe cancelled as not follower");
        }
        return true;
    }

    private static String getOasisIPfromPrefs() {
        final String ip = Pref.getString("desert_sync_master_ip", "").trim().replace("/", "");
        return ip;
    }

    private static String getOasisIP() {
        final String ip = getOasisIPfromPrefs();

        if (emptyString(lastLoadedIP)) {
            lastLoadedIP = ip;
        }
        setCurrentToBackup(ip, lastLoadedIP);
        return ip;
    }

    public static void setOasisIP(final String ip) {
        setCurrentToBackup(ip, null);

        Pref.setString("desert_sync_master_ip", ip);
        UserError.Log.uel(TAG, "Master IP updated to: " + ip);
    }

    private static void setCurrentToBackup(final String newIP, String toSave) {
        if (toSave == null) {
            toSave = getOasisIPfromPrefs();
        }
        if (toSave.equals(newIP)) return;
        final String backup = PersistentStore.getString(PREF_OASISIP);
        if (!toSave.equals(backup) && (!emptyString(toSave))) {
            PersistentStore.setString(PREF_OASISIP, toSave);
            UserError.Log.d(TAG, "Saving to backup: " + toSave);
        }
    }

    public static String getOasisBackupIP() {
        final String backupIP = PersistentStore.getString(PREF_OASISIP);
        return emptyString(backupIP) ? null : backupIP;
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private static void runInBackground() {

        // TODO we should probably do this in parallel for prospective followers
        new Thread(() -> {
            if (queue.size() == 0) return;
            final PowerManager.WakeLock wl = getWakeLock("DesertComms send", 60000);
            UserError.Log.d(TAG, "Queue size: " + queue.size());
            try {
                final String result = httpNext();
                //UserError.Log.d(TAG, "Result: " + result);
                checkCommsFailures(result == null);
                if (((result != null) && queue.size() > 0) || Home.get_master()) {
                    runInBackground();
                }
            } finally {
                releaseWakeLock(wl);
            }
        }).start();

    }

    private static String httpNext() {
        if (queue.peekFirst() == null) return null;
        try {
            final QueueItem item = queue.takeFirst(); // removes from queue
            item.retried++;
            item.updateLastProcessed();
            UserError.Log.d(TAG, "Next item: " + item.toS());
            final String result = httpGet(item.getUrl(Home.get_follower() ? item.handler != MasterPing ? getOasisIP() : null : null));
            // if (result != null) {
            item.result = result;
            item.handler.process(item);
            // }
            if (result != null || item.expired()) {
                queue.remove(item);
            } else {
                //    queue.add(item); // re-add
            }
            return result;
        } catch (InterruptedException e) {
            UserError.Log.e(TAG, "Got interrupted");
            return null;
        }
    }


    private static String httpGet(String url) {
        if (url == null) return null;
        final String hash = XdripWebService.hashPassword(Pref.getString(DesertSync.PREF_WEBSERVICE_SECRET, ""));
        final Request.Builder builder = new Request.Builder().url(url).addHeader("User-Agent", "xDrip+ Desert Comms");
        if (hash != null) builder.addHeader("api-secret", hash);
        try (final Response response = getHttpInstance().newCall(builder.build()).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                if (JoH.ratelimit("desert-error-response", 180)) {
                    UserError.Log.wtf(TAG, "Got error code: " + response.code() + " " + response.message() + " " + response.body().toString());
                }
                return null;
            }
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

            if (!Home.get_master()) {
                if (comms_failures > COMM_FAILURE_NOTIFICATION_THRESHOLD) {
                    if (pratelimit("desert-check-comms", 1800)) {
                        if (!RouteTools.reachable(getOasisIP())) {
                            UserError.Log.e(TAG, "Oasis master IP appears unreachable: " + getOasisIP());
                            showNotification("Desert Sync Failure", "Master unreachable: " + getOasisIP() + " changed?", null, DESERT_MASTER_UNREACHABLE, false, true, false);
                        }
                    }
                }
            }
        } else {
            if (!Home.get_master()) {
                if (comms_failures > 0) {
                    UserError.Log.d(TAG, "Comms restored after " + comms_failures + " failures");
                    if (comms_failures > COMM_FAILURE_NOTIFICATION_THRESHOLD) {
                        cancelNotification(DESERT_MASTER_UNREACHABLE);
                    }
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
        long lastProcessedTime = -1;
        @Expose
        int retried = 0;
        @Expose
        QueueHandler handler = None;

        String result;

        QueueItem(String url) {
            this.url = url;
            entryTime = tsl();
        }

        QueueItem setHandler(QueueHandler handler) {
            this.handler = handler;
            return this;
        }

        // reprocess url for specified host
        String getUrl(final String host) {
            if (emptyString(host)) return url;
            try {
                return HttpUrl.parse(url).newBuilder().host(host).build().toString();
            } catch (NullPointerException e) {
                return null;
            }
        }

        String urlIP() {
            try {
                return HttpUrl.parse(url).host();
            } catch (NullPointerException e) {
                return null;
            }
        }

        void updateLastProcessed() {
            lastProcessedTime = tsl();
        }

        boolean okToProcess() {
            return msSince(lastProcessedTime) > Constants.MINUTE_IN_MS;
        }

        boolean expired() {
            return oneHit() || retried > MAX_RETRIES || msSince(entryTime) > Constants.DAY_IN_MS;
        }

        boolean oneHit() {
            return handler == ToFollower || handler == MasterPing || handler == Pull;
        }

        String toS() {
            return defaultGsonInstance().toJson(this);
        }

    }

    enum QueueHandler {
        None,
        Pull,
        MasterPing,
        ToFollower;

        void process(final QueueItem item) {
            switch (this) {
                case Pull:
                    if (item.result == null) {
                        DesertSync.pullFailed(item.urlIP());
                    } else {
                        DesertSync.fromPull(item.result);
                    }
                    break;
                case ToFollower:
                    DesertSync.checkIpChange(item.result);
                    break;
                case MasterPing:
                    DesertSync.masterIdReply(item.result, item.urlIP());
                    break;
            }
        }
    }

    // megastatus

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        if (Home.get_follower()) {
            if (emptyString(getOasisIP())) {
                l.add(new StatusItem("Desert Master", "Not known yet - needs QR code scan?"));
            } else {
                l.add(new StatusItem("Desert Master", getOasisIP(), RouteTools.reachable(getOasisIP()) ? StatusItem.Highlight.NORMAL : StatusItem.Highlight.BAD));
            }
            if (Home.get_engineering_mode()) {
                l.add(new StatusItem("Desert Backup", getOasisBackupIP()));
                l.add(new StatusItem("Our IP", RouteTools.getBestInterfaceAddress()));
            }
        }

        return l;
    }

}
