package com.eveningoutpost.dexdrip;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.XdripNotificationCompat;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.utils.WebAppHelper;

/**
 * Created by jamorham on 17/02/2016.
 */
public class ParakeetHelper {

    private static final String TAG = "jamorham parakeethelp";
    private static boolean waiting_for_parakeet = false;
    public static boolean parakeet_not_checking_in = true; // default no information
    private static long wait_timestamp = 0;
    private static final double PARAKEET_ALERT_MISSING_MINUTES = 60;
    private static long highest_timestamp = 0;
    private static long highest_parakeet_timestamp = -1;

    public static String getParakeetURL(Context context) {
        String my_receivers = PreferenceManager.getDefaultSharedPreferences(context).getString("wifi_recievers_addresses", "").trim();
        if (my_receivers.equals("")) return null;

        String[] hosts = my_receivers.split(",");
        if (hosts.length == 0) return null;

        for (String host : hosts) {
            host = host.trim();
            if ((host.startsWith("http://") || host.startsWith("https://")) && (host.contains("/json.get") || host.contains("Parakeet"))) {
                return host;
            }
        }
        return null;
    }

    public static boolean isParakeetCheckingIn()
    {
        return !parakeet_not_checking_in;
    }

    private static int parakeetMinutesSinceCheckin()
    {
        return (int)((JoH.ts()-highest_parakeet_timestamp)/60000);
    }

    public static String parakeetStatusString()
    {
        if (isParakeetCheckingIn())
        {
            return "Parakeet seen "+parakeetMinutesSinceCheckin()+" mins ago";
        } else {
            return "Parakeet no data";
        }
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
        new WebAppHelper(new ParakeetHelper.ServiceCallback()).executeOnExecutor(xdrip.executor, url);
    }

    public static void checkParakeetNotifications(long timestamp, String geo_location) {
        Log.d(TAG, "CheckParakeetNotifications: " + timestamp + " / " + geo_location + " not checking in? " + parakeet_not_checking_in);
        if (waiting_for_parakeet) {
            Log.d(TAG, "checkParakeetNotifications:" + waiting_for_parakeet + " " + timestamp + " vs " + wait_timestamp);
            if (timestamp > wait_timestamp) {
                Log.d(TAG, "sending notification");
                sendNotification("The parakeet has connected to the web service.",
                        "Parakeet has connected!");
                waiting_for_parakeet = false;
                if (!PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).getBoolean("parakeet_first_run_done", false)) {
                    PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().putBoolean("parakeet_first_run_done", true).apply();
                }
            }
        } else {
            // look at any record which looks newer than we have considered so far

                // ignore everything except parakeet sourced datums
                if ((timestamp > highest_parakeet_timestamp) && (!geo_location.equals("-15,-15"))) {
                highest_parakeet_timestamp=timestamp;
                }
                    final int minutes_since = (int) ((JoH.ts() - highest_parakeet_timestamp) / (1000 * 60));
                    if (highest_parakeet_timestamp>0) Log.d(TAG, "Not waiting for parakeet Minutes since: " + minutes_since);


                    if (!parakeet_not_checking_in) {
                        if ((minutes_since > PARAKEET_ALERT_MISSING_MINUTES) && (highest_parakeet_timestamp > 0)) {
                            if (timestamp >= highest_timestamp) {
                                parakeet_not_checking_in = true;
                                Log.i(TAG, "Parakeet missing for: " + minutes_since + " mins");
                                sendNotification("The parakeet has not connected > " + minutes_since + " mins",
                                        "Parakeet missing");
                                // TODO some more sophisticated persisting notification
                            }
                        }
                    } else {
                        if (timestamp < highest_parakeet_timestamp) Log.d(TAG,"Timestamp less than highest");
                        if ((timestamp >= highest_parakeet_timestamp) && (minutes_since < PARAKEET_ALERT_MISSING_MINUTES)
                                && (!geo_location.equals("-15,-15"))) {
                            Log.d(TAG, "Parakeet now checking in: " + minutes_since + " mins ago");
                            parakeet_not_checking_in = false;
                            cancelParakeetMissingNotification();
                        }
                    }

                    if (timestamp > highest_timestamp) {
                        highest_timestamp = timestamp;
                    }

            }

    }

    public static void notifyOnNextCheckin(boolean always) {
        if ((always) || (!PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).getBoolean("parakeet_first_run_done", false))) {
            waiting_for_parakeet = true;
            wait_timestamp = System.currentTimeMillis();
        }
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
        if (Pref.getBooleanDefaultFalse("parakeet_status_alerts")) {
            Intent intent = new Intent(xdrip.getAppContext(), Home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(xdrip.getAppContext(), 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(xdrip.getAppContext(), NotificationChannels.PARAKEET_STATUS_CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(xdrip.getAppContext().getResources(), R.drawable.jamorham_parakeet_marker))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                 //   .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            if (!((PowerStateReceiver.is_power_connected()) && (Pref.getBooleanDefaultFalse("parakeet_charge_silent"))))
            {
                notificationBuilder.setSound(defaultSoundUri);
            }

            NotificationManager notificationManager =
                    (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(Notifications.parakeetMissingId);
            notificationManager.notify(Notifications.parakeetMissingId, XdripNotificationCompat.build(notificationBuilder));
        } else {
            Log.d(TAG, "Not sending parakeet notification as they are disabled: " + body);
        }
    }

    private static void cancelParakeetMissingNotification()
    {
        NotificationManager notificationManager =
                (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Notifications.parakeetMissingId);
    }

    public static boolean isRealParakeetDevice() {
        return (!parakeet_not_checking_in);
    }

    public static class ServiceCallback implements Preferences.OnServiceTaskCompleted {
        @Override
        public void onTaskCompleted(byte[] result) {
            try {
                String string_result = new String(result, "UTF-8");
                if (string_result.startsWith("OK")) {
                    notifyOnNextCheckin(true);
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
