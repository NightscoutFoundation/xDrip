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

import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jamorham on 11/01/16.
 */
public class GcmActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "jamorham gcmactivity";
    public static AtomicInteger msgId = new AtomicInteger(1);
    public static String token = null;
    public static String senderid = null;
    public static Context mContext;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    public static void setContext(Context xmContext) {
        mContext = xmContext;
    }

    private static String sendMessage(final String identity, final String action, final String payload) {
        new Thread() {
            @Override
            public void run() {
                sendMessageNow(identity, action, payload);
            }
        }.start();
        return "sent async";
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
        sendMessage(myIdentity(), "dt", treatment.uuid.toString());
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

    private static String sendMessageNow(String identity, String action, String payload) {

        Log.i(TAG, "Sendmessage called: " + identity + " " + action + " " + payload);
        if (mContext == null) {
            Log.e(TAG, "mContext is null cannot sendMessage");
            return "";
        }
        if (identity == null) {
            Log.e(TAG, "identity is null cannot sendMessage");
            return "";
        }
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(mContext);
        String msg = "";
        if (token == null) {
            Log.e(TAG, "GCM token is null - cannot sendMessage");
            return "";
        }
        try {
            Bundle data = new Bundle();
            data.putString("action", action);
            data.putString("identity", identity);
            // TODO queue backlog handling
            data.putString("payload", CipherUtils.encryptString(payload));
            String id = Integer.toString(msgId.incrementAndGet());
            gcm.send(senderid + "@gcm.googleapis.com", id, data);

            msg = "Sent message OK";
        } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
        }
        Log.d(TAG, "Return msg in SendMessage: " + msg);
        return msg;
    }

    public void tryGCMcreate() {
        Log.d(TAG, "try GCMcreate");

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
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mContext = this;
        tryGCMcreate();
        try {
            finish();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
}

