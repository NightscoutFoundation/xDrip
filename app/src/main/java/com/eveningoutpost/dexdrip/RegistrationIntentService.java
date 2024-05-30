package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 11/01/16.
 */

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.services.PlusSyncService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "jamorham regService";
    private static final String[] PREDEF = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final PowerManager.WakeLock wl = JoH.getWakeLock("registration-intent", 120000);
        try {
            GcmActivity.senderid = getString(R.string.gcm_defaultSenderId);
            String token = FirebaseInstanceId.getInstance().getToken();
            try {
                final JSONObject json = new JSONObject(token);
                final String json_token = json.getString("token");
                if (json_token.length() > 10) token = json_token;
                Log.d(TAG, "Used json method");
            } catch (Exception e) {
                //
            }
            Log.i(TAG, "GCM Registration Token: " + token);
            GcmActivity.token = token;
            subscribeTpcs(token);
            sendRegistrationToServer(token);
            sharedPreferences.edit().putBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, true).apply();
            final Intent registrationComplete = new Intent(PreferencesNames.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        } catch (Exception e) {
            Log.e(TAG, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, false).apply();
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void sendRegistrationToServer(String token) {
        try {
            Log.d(TAG, "Scheduling tasks");
            PeriodicTask task = new PeriodicTask.Builder()
                    .setService(TaskService.class)
                    .setTag(GcmActivity.TASK_TAG_UNMETERED)
                    .setRequiredNetwork(Task.NETWORK_STATE_UNMETERED)
                    .setPeriod(7200L)
                    .build();

            GcmNetworkManager.getInstance(this).cancelAllTasks(TaskService.class);
            GcmNetworkManager.getInstance(this).schedule(task);
            PlusSyncService.startSyncService(getApplicationContext(), "RegistrationToServer");
            GcmActivity.queueCheckOld(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Exception in sendRegistration: " + e.toString());
        }
    }

    private void subscribeTpcs(String token) throws IOException {
        FirebaseMessaging.getInstance().subscribeToTopic(GcmActivity.myIdentity());
        for (String tpc : PREDEF) {
            FirebaseMessaging.getInstance().subscribeToTopic(tpc);
        }
    }
}