package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.UserNotification;
import com.eveningoutpost.dexdrip.R;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * Created by stephenblack on 11/28/14.
 */
public class Notifications {

    public static void bgAlert(double value, String slopeArrow, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = prefs.getBoolean("notifications", false);
        boolean vibrate = prefs.getBoolean("vibrate", false);
        boolean lights = prefs.getBoolean("lights", false);
        boolean sound = prefs.getBoolean("play_sound", false);
        int snooze = Integer.parseInt(prefs.getString("snooze", "20"));

        String notification_sound = prefs.getString("notification_sound", "content://settings/system/notification_sound");
        Log.w("Notification Sound URI ", notification_sound);

        long[] vibratePattern = {0,1000,300,1000,300,1000};

        if (notifications) {

            UserNotification userNotification = UserNotification.lastBgAlert();
            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(0);

            //dont alarm every time, just if its been 20 minutes since the last time we set an alarm or if we have no set an alarm
            if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * snooze)))) {
                UserNotification newUserNotification = UserNotification.create(df.format(BgReading.activePrediction()) + " " + slopeArrow, "bg_alert");
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                                .setContentTitle(newUserNotification.message)
                                .setContentText("BG LEVEL ALERT: " + newUserNotification.message);

                if (vibrate) { mBuilder.setVibrate(vibratePattern); Log.w("Vibrate: ", "TRUE"); }
                if (lights) { mBuilder.setLights(0xff00ff00, 300, 1000); Log.w("Lights: ", "TRUE");}
                if (sound) { mBuilder.setSound(Uri.parse(notification_sound), AudioAttributes.FLAG_AUDIBILITY_ENFORCED); Log.w("Sound: ", "TRUE");}
//                if (sound) { mBuilder.setSound(RingtoneManager.getDefaultUri(Uri.parse(notification_sound))); Log.w("Sound: ", "TRUE");}

                Intent resultIntent = new Intent(context, Home.class);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

                mNotifyMgr.cancel(001);
                mNotifyMgr.notify(001, mBuilder.build());


                //Otherwise update the notification without alarming
            } else if ((userNotification != null) && (userNotification.timestamp >= ((new Date().getTime()) - (60000 * snooze))))  {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                                .setContentTitle(df.format(BgReading.activePrediction()) + " " + slopeArrow)
                                .setContentText("BG LEVEL ALERT: " + df.format(BgReading.activePrediction()) + " " + slopeArrow);

                Intent resultIntent = new Intent(context, Home.class);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

                mNotifyMgr.notify(001, mBuilder.build());
            }
        }
    }

    public static void clearBgAlert(Context context) {
        UserNotification userNotification = UserNotification.lastBgAlert();
        if (userNotification != null) {
            userNotification.cleared = true;
            userNotification.save();
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(001);
        }
    }
}
