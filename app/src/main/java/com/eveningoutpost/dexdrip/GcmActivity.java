package com.eveningoutpost.dexdrip;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.DisplayQRCode;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jamorham on 11/01/16.
 */
public class GcmActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String TASK_TAG_CHARGING = "charging";
    public static final String TASK_TAG_UNMETERED = "unmetered";
    private static final String TAG = "jamorham gcmactivity";
    public static double last_sync_request = 0;
    public static AtomicInteger msgId = new AtomicInteger(1);
    public static String token = null;
    public static String senderid = null;
    public static List<GCM_data> gcm_queue = new ArrayList<>();
    private static final Object queue_lock = new Object();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    public static boolean cease_all_activity = false;

    public static synchronized void queueAction(String reference) {
        synchronized (queue_lock) {
            Log.d(TAG, "Received ACK, Queue Size: " + GcmActivity.gcm_queue.size() + " " + reference);
            for (GCM_data datum : gcm_queue) {
                String thisref = datum.bundle.getString("action") + datum.bundle.getString("payload");
                if (thisref.equals(reference)) {
                    gcm_queue.remove(gcm_queue.indexOf(datum));
                    Log.d(TAG, "Removing acked queue item: " + reference);
                    break;
                }
            }
            queueCheckOld(xdrip.getAppContext());
        }
    }

    public static void queueCheckOld(Context context) {

        if (context == null) {
            Log.e(TAG, "Can't process old queue as null context");
            return;
        }

        final double MAX_QUEUE_AGE = (5 * 60 * 60 * 1000); // 5 hours
        final double MIN_QUEUE_AGE = (0 * 60 * 1000); // minutes
        final double MAX_RESENT = 10;
        Double timenow = JoH.ts();
        boolean queuechanged = false;
        synchronized (queue_lock) {
            for (GCM_data datum : gcm_queue) {
                if ((timenow - datum.timestamp) > MAX_QUEUE_AGE
                        || datum.resent > MAX_RESENT) {
                    queuechanged = true;
                    Log.i(TAG, "Removing old unacknowledged queue item: resent: " + datum.resent);
                    gcm_queue.remove(gcm_queue.indexOf(datum));
                    break;
                } else if (timenow - datum.timestamp > MIN_QUEUE_AGE) {
                    try {
                        Log.i(TAG, "Resending unacknowledged queue item: " + datum.bundle.getString("action") + datum.bundle.getString("payload"));
                        datum.resent++;
                        GoogleCloudMessaging.getInstance(context).send(senderid + "@gcm.googleapis.com", Integer.toString(msgId.incrementAndGet()), datum.bundle);
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception during resend: " + e.toString());
                    }
                    break;
                }
            }
        }
        if (queuechanged)  queueCheckOld(context);
    }

    private static String sendMessage(final String action, final String payload) {
        if (cease_all_activity) return null;
        return sendMessage(myIdentity(), action, payload);
    }

    private static String sendMessage(final String identity, final String action, final String payload) {
        if (cease_all_activity) return null;
        new Thread() {
            @Override
            public void run() {
                sendMessageNow(identity, action, payload);
            }
        }.start();
        return "sent async";
    }

    public static void syncBGReading(BgReading bgReading) {
        GcmActivity.sendMessage(GcmActivity.myIdentity(), "bgs", bgReading.toJSON());
    }

    public static void requestBGsync() {
        if (token != null) {
            if ((JoH.ts() - last_sync_request) > (60 * 1000 * 5)) {
                last_sync_request = JoH.ts();
                GcmActivity.sendMessage("bfr", "");
            } else {
                Log.d(TAG, "Already requested BGsync recently");
            }
        } else {
            Log.d(TAG,"No token for BGSync");
        }
    }

    public static void syncBGTable2() {
        new Thread() {
            @Override
            public void run() {

                if ((JoH.ts() - last_sync_request) > (60 * 1000 * 5)) {
                    last_sync_request = JoH.ts();

                    final List<BgReading> bgReadings = BgReading.latestForGraph(300, JoH.ts() - (24 * 60 * 60 * 1000));
                    String mypacket = "";

                    for (BgReading bgReading : bgReadings) {
                        String myrecord = bgReading.toJSON();
                        if (mypacket.length() > 0) {
                            mypacket = mypacket + "^";
                        }
                        mypacket = mypacket + myrecord;
                    }
                    Log.d(TAG, "Total BGreading sync packet size: " + mypacket.length());
                    if (DisplayQRCode.mContext == null)
                        DisplayQRCode.mContext = xdrip.getAppContext();
                    DisplayQRCode.uploadBytes(mypacket.getBytes(Charset.forName("UTF-8")), 2);
                } else {
                    Log.d(TAG, "Ignoring recent sync request");
                }
            }
        }.start();
    }

    // callback function
    public static void backfillLink(String id, String key) {
        sendMessage("bfb", id + "^" + key);
        DisplayQRCode.mContext = null;
    }

    public static void processBFPbundle(String bundle) {
        String[] bundlea = bundle.split("\\^");
        for (String bgr : bundlea) {
            BgReading.bgReadingInsertFromJson(bgr,false);
        }
        Home.staticRefreshBGCharts();
    }

    public static void pushTreatmentAsync(final Treatments thistreatment) {
        new Thread() {
            @Override
            public void run() {
                push_treatment(thistreatment);
            }
        }.start();
    }

    private static void push_treatment(Treatments thistreatment) {
        String json = thistreatment.toJSON();
        sendMessage(myIdentity(), "nt", json);
    }

    public static void send_ping_reply() {
        Log.d(TAG, "Sending ping reply");
        sendMessage(myIdentity(), "q", "");
    }

    public static void push_delete_all_treatments() {
        Log.i(TAG, "Sending push for delete all treatments");
        sendMessage(myIdentity(), "dat", "");
    }

    public static void push_delete_treatment(Treatments treatment) {
        Log.i(TAG, "Sending push for specific treatment");
        sendMessage(myIdentity(), "dt", treatment.uuid);
    }

    public static String myIdentity() {
        // TODO prefs override possible
        return GoogleDriveInterface.getDriveIdentityString();
    }

    public static void pushTreatmentFromPayloadString(String json) {
        if (json.length() < 3) return;
        Log.d(TAG, "Pushing json from GCM: " + json);
        Treatments.pushTreatmentFromJson(json);
    }

    public static void pushCalibration(String bg_value, String seconds_ago) {
        if ((bg_value.length() == 0) || (seconds_ago.length() == 0)) return;
        String currenttime = Double.toString(new Date().getTime());
        String tosend = currenttime + " " + bg_value + " " + seconds_ago;
        sendMessage(myIdentity(), "cal", tosend);
    }

    private static synchronized String sendMessageNow(String identity, String action, String payload) {

        Log.i(TAG, "Sendmessage called: " + identity + " " + action + " " + payload);
        String msg;
        try {
            Bundle data = new Bundle();
            data.putString("action", action);
            data.putString("identity", identity);

            if (payload.length()>0) {
                data.putString("payload", CipherUtils.encryptString(payload));
            } else {
                data.putString("payload","");
            }

            if (xdrip.getAppContext() == null) {
                Log.e(TAG, "mContext is null cannot sendMessage");
                return "";
            }
            if (identity == null) {
                Log.e(TAG, "identity is null cannot sendMessage");
                return "";
            }
            gcm_queue.add(new GCM_data(data));
            final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(xdrip.getAppContext());
            if (token == null) {
                Log.e(TAG, "GCM token is null - cannot sendMessage");
                return "";
            }
            gcm.send(senderid + "@gcm.googleapis.com", Integer.toString(msgId.incrementAndGet()), data);

            msg = "Sent message OK";
        } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
        }
        Log.d(TAG, "Return msg in SendMessage: " + msg);
        return msg;
    }

    public void tryGCMcreate() {
        Log.d(TAG, "try GCMcreate");
        if (cease_all_activity) return;
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.i(TAG, "Token retrieved and sent");
                } else {
                    Log.e(TAG, "Error with token");
                }
            }
        };

        if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        } else {
            cease_all_activity = true;
            final String msg = "xDrip ERROR: Connecting to Google Services";
            JoH.static_toast(this, msg, Toast.LENGTH_LONG);
            Home.toaststatic(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (cease_all_activity) return;
        Log.d(TAG, "onCreate");
        tryGCMcreate();
        try {
            finish();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cease_all_activity) return;
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(PreferencesNames.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported for play services.");
                finish();
            }
            return false;
        }
        return true;
    }

    private static class GCM_data {
        public Bundle bundle;
        public Double timestamp;
        public int resent;

        public GCM_data(Bundle data) {
            bundle = data;
            timestamp = JoH.ts();
            resent = 0;
        }
    }
}

