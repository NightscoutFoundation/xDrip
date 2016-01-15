package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 11/01/16.
 */

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "jamorham regService";
    private static final String[] PREDEF = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            GcmActivity.senderid = getString(R.string.gcm_defaultSenderId);
            String token = instanceID.getToken(GcmActivity.senderid,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Registration Token: " + token);
            GcmActivity.token = token;
            subscribeTpcs(token);
            sendRegistrationToServer(token);
            sharedPreferences.edit().putBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, true).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, false).apply();
        }
        Intent registrationComplete = new Intent(PreferencesNames.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void sendRegistrationToServer(String token) {
    }

    private void subscribeTpcs(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        pubSub.subscribe(token, getString(R.string.gcmtpcs) + GcmActivity.myIdentity(), null);
        for (String tpc : PREDEF) {
            pubSub.subscribe(token, getString(R.string.gcmstpcs) + tpc, null);
        }
    }
}