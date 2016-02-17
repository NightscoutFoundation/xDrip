package com.eveningoutpost.dexdrip;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.utils.WebAppHelper;

/**
 * Created by jamorham on 17/02/2016.
 */
public class ParakeetHelper {

    private static final String TAG = "jamorham parakeethelp";
    private static boolean waiting_for_parakeet = false;
    private static long wait_timestamp = 0;

    public static String getParakeetURL(Context context) {
        String my_receivers = PreferenceManager.getDefaultSharedPreferences(context).getString("wifi_recievers_addresses", "").trim();
        if (my_receivers.equals("")) return null;

        String[] hosts = my_receivers.split(",");
        if (hosts.length == 0) return null;

        for (String host : hosts) {
            host = host.trim();
            if ((host.startsWith("http://") || host.startsWith("https://")) && host.contains("/json.get")) {
                return host;
            }
        }
        return null;
    }

    public static String getParakeetSetupURL(Context context) {
        String url = getParakeetURL(context);
        if (url == null) return null;
        return url.replace("/json.get", "/setcode/2");
    }

    // put parakeet in to setup mode on next checkin
    public static void parakeetSetupMode(Context context) {

        String url = getParakeetSetupURL(context);

        if (url == null) {
            toast(context, "Can't find parakeet app engine URL!");
            return;
        }
        new WebAppHelper(new ParakeetHelper.ServiceCallback()).execute(url);
    }

    public static void checkParakeetNotifications(long timestamp) {
        if (waiting_for_parakeet) {
            if (timestamp > wait_timestamp) {
                sendNotification("The parakeet has connected to the web service.",
                        "Parakeet has connected!");
                waiting_for_parakeet = false;
            }
        }
    }

    public static void notifyOnNextCheckin() {
        waiting_for_parakeet = true;
        wait_timestamp = System.currentTimeMillis();
    }

    public static void toast(Context context, final String msg) {
        try {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            Log.d(TAG, "toast: " + msg);
        } catch (Exception e) {
            Log.d(TAG, "Couldn't display toast: " + msg + " / " + e.toString());
        }
    }

    private static void sendNotification(String body, String title) {
        Intent intent = new Intent(xdrip.getAppContext(), Home.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(xdrip.getAppContext(), 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(xdrip.getAppContext())
                .setSmallIcon(R.drawable.jamorham_parakeet_marker)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    public static class ServiceCallback implements Preferences.OnServiceTaskCompleted {
        @Override
        public void onTaskCompleted(byte[] result) {
            try {
                String string_result = new String(result, "UTF-8");
                if (string_result.startsWith("OK")) {
                    notifyOnNextCheckin();
                    String[] results = string_result.split(" ");
                    if (results[1].contains("0")) {
                        toast(xdrip.getAppContext(), "Parakeet code sent! Now waiting..");
                    } else {
                        toast(xdrip.getAppContext(), "Parakeet code sent! Code: " + results[1]);
                    }
                } else {
                    toast(xdrip.getAppContext(), "Error - is app engine receiver recent enough?");
                }

            } catch (Exception e) {
                Log.e(TAG, "Got error in web helper callback: " + e.toString());
            }
        }
    }

}
