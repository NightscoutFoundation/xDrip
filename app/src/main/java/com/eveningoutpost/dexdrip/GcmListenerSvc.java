package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 11/01/16.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.utils.CipherUtils;

import java.util.Date;


public class GcmListenerSvc extends com.google.android.gms.gcm.GcmListenerService {

    private static final String TAG = "jamorham GCMlis";

    @Override
    public void onMessageReceived(String from, Bundle data) {

        String message = data.getString("message");

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        Bundle notification = data.getBundle("notification");
        if (notification != null) {
            Log.d(TAG, "Processing notification bundle");
            sendNotification(notification.getString("body"), notification.getString("title"));
        }

        if (from.startsWith(getString(R.string.gcmtpc))) {

            String xfrom = data.getString("xfrom");

            if (xfrom.equals(GcmActivity.token)) {
                // TODO remove from queued list once we have this ack
                return;
            }

            String payload = data.getString("datum");
            String action = data.getString("action");

            if (payload.length() > 16) {
                String decrypted_payload = CipherUtils.decryptString(payload);
                if (decrypted_payload.length() > 0) {
                    payload = decrypted_payload;
                } else {
                    Log.e(TAG, "Couldn't decrypt payload!");
                    payload = "";
                    Home.toaststaticnext("Having problems decrypting incoming data - check keys");
                }
            }

            Log.i(TAG, "Got action: " + action + " with payload: " + payload);

            // new treatment
            if (action.equals("nt") && (payload != null)) {
                Log.i(TAG, "Attempting GCM push to Treatment");
                GcmActivity.pushTreatmentFromPayloadString(payload);
            } else if (action.equals("dat")) {
                Log.i(TAG, "Attempting GCM delete all treatments");
                Treatments.delete_all();

            } else if (action.equals("cal")) {
                String[] message_array = payload.split("\\s+");
                if ((message_array.length == 3) && (message_array[0].length() > 0)
                        && (message_array[1].length() > 0) && (message_array[2].length() > 0)) {
                    // [0]=timestamp [1]=bg_String [2]=bgAge
                    Intent calintent = new Intent();
                    calintent.setClassName("com.eveningoutpost.dexdrip", "com.eveningoutpost.dexdrip.AddCalibration");
                    calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    long timediff = (long) ((new Date().getTime() - Double.parseDouble(message_array[0])) / 1000);
                    Log.i(TAG, "Remote calibration latency calculated as: " + Long.toString(timediff) + " seconds");
                    if (timediff > 0) {
                        message_array[2] = Long.toString(Long.parseLong(message_array[2]) + timediff);
                    }
                    Log.i(TAG, "Processing remote CAL " + message_array[1] + " age: " + message_array[2]);
                    calintent.putExtra("bg_string", message_array[1]);
                    calintent.putExtra("bg_age", message_array[2]);
                    if (timediff < 3600) {
                        getApplicationContext().startActivity(calintent);
                    }
                } else {
                    Log.e(TAG, "Invalid CAL payload");
                }
            } else if (action.equals("p")) {
                GcmActivity.send_ping_reply();
            } else if (action.equals("bgs")) {
                Log.i(TAG, "Received Backfill packet");
                String bgs[] = payload.split("\\^");
                for (String bgr : bgs) {
                    BgReading.bgReadingInsertFromJson(bgr);
                }
                Home.staticRefreshBGCharts();
            }
        } else {
            // direct downstream message.
            Log.i(TAG, "Received downstream message: " + message);
        }
    }

    private void sendNotification(String body, String title) {
        Intent intent = new Intent(this, Home.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

}

