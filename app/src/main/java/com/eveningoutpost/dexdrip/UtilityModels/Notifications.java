package com.eveningoutpost.dexdrip.UtilityModels;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import com.eveningoutpost.dexdrip.*;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.CalibrationRequest;
import com.eveningoutpost.dexdrip.Models.UserNotification;

import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/28/14.
 */
public class Notifications {
    public static final long[] vibratePattern = {0,1000,300,1000,300,1000};
    public static boolean bg_notifications;
    public static boolean bg_ongoing;
    public static boolean bg_vibrate;
    public static boolean bg_lights;
    public static boolean bg_sound;
    public static boolean bg_sound_in_silent;
    public static int bg_snooze;
    public static String bg_notification_sound;

    public static boolean calibration_notifications;
    public static boolean calibration_vibrate;
    public static boolean calibration_lights;
    public static boolean calibration_sound;
    public static int calibration_snooze;
    public static String calibration_notification_sound;

    public static Context mContext;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    public static int currentVolume;
    public static AudioManager manager;

    public static final int BgNotificationId = 001;
    public static final int calibrationNotificationId = 002;
    public static final int doubleCalibrationNotificationId = 003;
    public static final int extraCalibrationNotificationId = 004;
    public static final int exportCompleteNotificationId = 005;
    public static final int ongoingNotificationId = 8811;

    public static void setNotificationSettings(Context context) {
        mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        bg_notifications = prefs.getBoolean("bg_notifications", true);
        bg_vibrate = prefs.getBoolean("bg_vibrate", true);
        bg_lights = prefs.getBoolean("bg_lights", true);
        bg_sound = prefs.getBoolean("bg_play_sound", true);
        bg_snooze = Integer.parseInt(prefs.getString("bg_snooze", "20"));
        bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");
        bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", false);

        calibration_notifications = prefs.getBoolean("calibration_notifications", true);
        calibration_vibrate = prefs.getBoolean("calibration_vibrate", true);
        calibration_lights = prefs.getBoolean("calibration_lights", true);
        calibration_sound = prefs.getBoolean("calibration_play_sound", true);
        calibration_snooze = Integer.parseInt(prefs.getString("calibration_snooze", "20"));
        calibration_notification_sound = prefs.getString("calibration_notification_sound", "content://settings/system/notification_sound");
        bg_ongoing = prefs.getBoolean("run_service_in_foreground", false);
    }

    public static void notificationSetter(Context context) {
        setNotificationSettings(context);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        double high = bgGraphBuilder.highMark;
        double low = bgGraphBuilder.lowMark;
        Sensor sensor = Sensor.currentSensor();

        List<BgReading> bgReadings = BgReading.latest(3);
        List<Calibration> calibrations = Calibration.allForSensorInLastFourDays();
        if(bgReadings == null || bgReadings.size() < 3) { return; }
        if(calibrations == null || calibrations.size() < 2) { return; }
        BgReading bgReading = bgReadings.get(0);
        if (bg_ongoing && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            bgOngoingNotification(bgGraphBuilder);
        }
        if (bg_notifications && sensor != null) {
            if (bgGraphBuilder.unitized(bgReading.calculated_value) >= high || bgGraphBuilder.unitized(bgReading.calculated_value) <= low) {
                if(bgReading.calculated_value > 14) {
                    if (bgReading.hide_slope) {
                        bgAlert(bgReading.displayValue(mContext), "");
                    } else {
                        bgAlert(bgReading.displayValue(mContext), BgReading.slopeArrow(bgReading.calculated_value_slope));
                    }
                }
            } else {
                clearBgAlert();
            }
        } else {
            clearAllBgNotifications();
        }

        if (calibration_notifications) {
            if (bgReadings.size() >= 3) {
                if (calibrations.size() == 0 && (new Date().getTime() - bgReadings.get(2).timestamp <= (60000 * 30)) && sensor != null) {
                    if ((sensor.started_at + (60000 * 60 * 2)) < new Date().getTime()) {
                        doubleCalibrationRequest();
                    } else { clearDoubleCalibrationRequest(); }
                } else { clearDoubleCalibrationRequest(); }
            } else { clearDoubleCalibrationRequest(); }

            if (CalibrationRequest.shouldRequestCalibration(bgReading) && (new Date().getTime() - bgReadings.get(2).timestamp <= (60000 * 24))) {
                extraCalibrationRequest();
            } else { clearExtraCalibrationRequest(); }

            if (calibrations.size() >= 1 && Math.abs((new Date().getTime() - calibrations.get(0).timestamp))/(1000*60*60) > 12) {
                Log.e("NOTIFICATIONS", "Calibration difference in hours: " + ((new Date().getTime() - calibrations.get(0).timestamp))/(1000*60*60));

                calibrationRequest();
            } else { clearCalibrationRequest(); }

        } else {
            clearAllCalibrationNotifications();
        }
    }

    private static Bitmap createWearBitmap(long start, long end) {
        return new BgSparklineBuilder(mContext)
                .setBgGraphBuilder(new BgGraphBuilder(mContext))
                .setStart(start)
                .setEnd(end)
                .showHighLine()
                .showLowLine()
                .showAxes()
                .setWidthPx(400)
                .setHeightPx(400)
                .setSmallDots()
                .build();
    }

    private static Bitmap createWearBitmap(long hours) {
        return createWearBitmap(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
    }

    private static Notification createExtensionPage(long hours) {
        return new NotificationCompat.Builder(mContext)
                .extend(new NotificationCompat.WearableExtender()
                                .setBackground(createWearBitmap(hours))
                                .setHintShowBackgroundOnly(true)
                                .setHintAvoidBackgroundClipping(true)
                )
                .build();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Notification createOngoingNotification(BgGraphBuilder bgGraphBuilder) {
        Intent intent = new Intent(mContext, Home.class);
        List<BgReading> lastReadings = BgReading.latest(2);
        BgReading lastReading = null;
        if (lastReadings != null && lastReadings.size() >= 2) {
            lastReading = lastReadings.get(0);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(Home.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext);
        //b.setOngoing(true);
        b.setCategory(NotificationCompat.CATEGORY_STATUS);
        String titleString = lastReading == null ? "BG Reading Unavailable" : (lastReading.displayValue(mContext) + " " + lastReading.slopeArrow());
        b.setContentTitle(titleString)
                .setContentText("xDrip Data collection service is running.")
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setUsesChronometer(false);
        if (lastReading != null) {
            Bitmap wearBitmap3h = createWearBitmap(3);

            b.setWhen(lastReading.timestamp);
            String deltaString = "Delta: " + bgGraphBuilder.unitizedDeltaString(lastReading.calculated_value - lastReadings.get(1).calculated_value);
            b.setContentText(deltaString);
            b.setLargeIcon(new BgSparklineBuilder(mContext)
                    .setHeight(64)
                    .setWidth(64)
                    .setStart(System.currentTimeMillis() - 60000 * 60 * 3)
                    .setBgGraphBuilder(new BgGraphBuilder(mContext))
                    .build());

            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(new BgSparklineBuilder(mContext)
                    .setBgGraphBuilder(new BgGraphBuilder(mContext))
                    .showHighLine()
                    .showLowLine()
                    .build())
                    .setSummaryText(deltaString)
                    .setBigContentTitle(titleString);
            b.setStyle(bigPictureStyle)
                    .extend(new NotificationCompat.WearableExtender()
                                    .setBackground(wearBitmap3h)
                                    .addPage(createExtensionPage(3))
                                    .addPage(createExtensionPage(6))
                                    .addPage(createExtensionPage(12))
                                    .addPage(createExtensionPage(24))
                    );
        }
        b.setContentIntent(resultPendingIntent);
        return b.build();
    }

    public static void bgOngoingNotification(final BgGraphBuilder bgGraphBuilder) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManagerCompat
                        .from(mContext)
                        .notify(ongoingNotificationId, createOngoingNotification(bgGraphBuilder));
            }
        });
    }

    public static void soundAlert(String soundUri) {
        manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        Uri notification = Uri.parse(bg_notification_sound);
        MediaPlayer player = MediaPlayer.create(mContext, notification);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
        });
        player.start();
    }

    public static void clearAllBgNotifications() {
        notificationDismiss(BgNotificationId);
    }
    public static void clearAllCalibrationNotifications() {
        notificationDismiss(calibrationNotificationId);
        notificationDismiss(extraCalibrationNotificationId);
        notificationDismiss(doubleCalibrationNotificationId);
    }


    public static void bgNotificationCreate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        if (bg_vibrate) { mBuilder.setVibrate(vibratePattern);}
        if (bg_lights) { mBuilder.setLights(0xff00ff00, 300, 1000);}
        if (bg_sound && !bg_sound_in_silent) { mBuilder.setSound(Uri.parse(bg_notification_sound), AudioAttributes.FLAG_AUDIBILITY_ENFORCED);}
        if (bg_sound && bg_sound_in_silent) { soundAlert(bg_notification_sound);}
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    public static void calibrationNotificationCreate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        if (calibration_vibrate) { mBuilder.setVibrate(vibratePattern);}
        if (calibration_lights) { mBuilder.setLights(0xff00ff00, 300, 1000);}
        if (calibration_sound) { mBuilder.setSound(Uri.parse(calibration_notification_sound), AudioAttributes.FLAG_AUDIBILITY_ENFORCED);}
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    public static void notificationUpdate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    public static NotificationCompat.Builder notificationBuilder(String title, String content, Intent intent) {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(notificationIntent(intent));
    }
    public static PendingIntent notificationIntent(Intent intent){
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    public static void notificationDismiss(int notificationId) {
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
    }

    public static void bgAlert(String value, String slopeArrow) {
        UserNotification userNotification = UserNotification.lastBgAlert();

        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * bg_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification newUserNotification = UserNotification.create(value + " " + slopeArrow, "bg_alert");
            String title = value + " " + slopeArrow;
            String content = "BG LEVEL ALERT: " + value + " " + slopeArrow;
            Intent intent = new Intent(mContext, Home.class);
            bgNotificationCreate(title, content, intent, BgNotificationId);

        } else if ((userNotification != null) && (userNotification.timestamp >= ((new Date().getTime()) - (60000 * bg_snooze))))  {
            String title = value + " " + slopeArrow;
            String content = "BG LEVEL ALERT: " + value + " " + slopeArrow;
            Intent intent = new Intent(mContext, Home.class);
            notificationUpdate(title, content, intent, BgNotificationId);
        }
    }

    public static void calibrationRequest() {
        UserNotification userNotification = UserNotification.lastCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification newUserNotification = UserNotification.create("12 hours since last Calibration", "calibration_alert");
            String title = "Calibration Needed";
            String content = "12 hours since last calibration";
            Intent intent = new Intent(mContext, AddCalibration.class);
            calibrationNotificationCreate(title, content, intent, calibrationNotificationId);
        }
    }
    public static void doubleCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification newUserNotification = UserNotification.create("Double Calibration", "double_calibration_alert");
            String title = "Sensor is ready";
            String content = "Sensor is ready, please enter a double calibration";
            Intent intent = new Intent(mContext, DoubleCalibrationActivity.class);
            calibrationNotificationCreate(title, content, intent, calibrationNotificationId);
        }
    }

    public static void extraCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification newUserNotification = UserNotification.create("Extra Calibration Requested", "extra_calibration_alert");
            String title = "Calibration Needed";
            String content = "A calibration entered now will GREATLY increase performance";
            Intent intent = new Intent(mContext, AddCalibration.class);
            calibrationNotificationCreate(title, content, intent, extraCalibrationNotificationId);
        }
    }

    public static void clearCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(calibrationNotificationId);
        }
    }

    public static void clearDoubleCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(doubleCalibrationNotificationId);
        }
    }

    public static void clearExtraCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(extraCalibrationNotificationId);
        }
    }

    public static void clearBgAlert() {
        UserNotification userNotification = UserNotification.lastBgAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(BgNotificationId);
        }
    }
}
