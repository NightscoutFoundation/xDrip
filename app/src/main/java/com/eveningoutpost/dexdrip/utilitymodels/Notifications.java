package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.SpannableString;
import android.widget.RemoteViews;

import com.eveningoutpost.dexdrip.AddCalibration;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.DoubleCalibrationActivity;
import com.eveningoutpost.dexdrip.EditAlertActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.CalibrationRequest;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.services.MissedReadingService;
import com.eveningoutpost.dexdrip.services.SnoozeOnNotificationDismissService;
import com.eveningoutpost.dexdrip.evaluators.PersistentHigh;
import com.eveningoutpost.dexdrip.ui.NumberGraphic;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice;
import com.eveningoutpost.dexdrip.services.broadcastservice.BroadcastEntry;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Date;
import java.util.List;

import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.X;
import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;

/**
 * Created by Emma Black on 11/28/14.
 */
public class Notifications extends IntentService {
    public static final long[] vibratePattern = {0, 1000, 300, 1000, 300, 1000};
    public static boolean bg_notifications;
    public static boolean bg_notifications_watch;
    public static boolean bg_persistent_high_alert_enabled_watch;
    public static boolean bg_ongoing;
    //public static boolean bg_vibrate;
   // public static boolean bg_lights;
   // public static boolean bg_sound;
    public static boolean compact_persistent_notification;
    public static boolean bg_sound_in_silent;
    public static String bg_notification_sound;

    public static boolean calibration_notifications;
    public static boolean calibration_override_silent;
    public static int calibration_snooze;
    public static String calibration_notification_sound;
    public static boolean doMgdl;
    public static boolean smart_snoozing;
    public static boolean smart_alerting;
    private final static String TAG = AlertPlayer.class.getSimpleName();
    private static final boolean use_best_glucose = true;

    private static String last_noise_string = "Startup";


    Context mContext;
    private static volatile PendingIntent wakeIntent;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    private BestGlucose.DisplayGlucose dg;
    int currentVolume;
    AudioManager manager;
    Bitmap iconBitmap;
    Bitmap notifiationBitmap;

    final static int BgNotificationId = 001;
    final static int calibrationNotificationId = 002;
    final static int doubleCalibrationNotificationId = 003;
    final static int extraCalibrationNotificationId = 004;
    public static final int exportCompleteNotificationId = 005;
    final static int ongoingNotificationId = 8811;
    public static final int exportAlertNotificationId = 006;
    public static final int uncleanAlertNotificationId = 007;
    public static final int missedAlertNotificationId = 010;
    public static final int riseAlertNotificationId = 011;
    public static final int failAlertNotificationId = 012;
    public static final int lowPredictAlertNotificationId = 013;
    public static final int parakeetMissingId = 014;
    public static final int persistentHighAlertNotificationId = 015;
    public static final int ob1SessionRestartNotificationId = 016;
    private static boolean low_notifying = false;

    private static final int CALIBRATION_REQUEST_MAX_FREQUENCY = (60 * 60 * 6); // don't bug for extra calibrations more than every 6 hours
    private static final int CALIBRATION_REQUEST_MIN_FREQUENCY = (60 * 60 * 8); // don't bug for general calibrations more than every 8 hours


    SharedPreferences prefs;

    public Notifications() {
        super("Notifications");
        Log.i("Notifications", "Creating Notifications Intent Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("NotificationsService", 60000);

        boolean unclearReading;
        try {
            Log.d("Notifications", "Running Notifications Intent Service");
            final Context context = getApplicationContext();

            if (Pref.getBoolean("motion_tracking_enabled", false)) {
                // TODO move this
                ActivityRecognizedService.reStartActivityRecogniser(context);
            }

            ReadPerfs(context);
            unclearReading = notificationSetter(context);
            scheduleWakeup(context, unclearReading);
            context.startService(new Intent(context, MissedReadingService.class));

        } finally {
            JoH.releaseWakeLock(wl);
        }
    }




    public void ReadPerfs(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        bg_notifications = prefs.getBoolean("bg_notifications", true);
        bg_notifications_watch = PersistentStore.getBoolean("bg_notifications_watch");
        bg_persistent_high_alert_enabled_watch = PersistentStore.getBoolean("persistent_high_alert_enabled_watch");
        //bg_vibrate = prefs.getBoolean("bg_vibrate", true);
        //bg_lights = prefs.getBoolean("bg_lights", true);
        //bg_sound = prefs.getBoolean("bg_play_sound", true);
        bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");
        bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", false);

        calibration_notifications = prefs.getBoolean("calibration_notifications", false);
        calibration_snooze = Integer.parseInt(prefs.getString("calibration_snooze", "20"));
        calibration_override_silent = prefs.getBoolean("calibration_alerts_override_silent", false);
        calibration_notification_sound = prefs.getString("calibration_notification_sound", "content://settings/system/notification_sound");
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);
        smart_snoozing = prefs.getBoolean("smart_snoozing", true);
        smart_alerting = prefs.getBoolean("smart_alerting", true);
        bg_ongoing = prefs.getBoolean("run_service_in_foreground", false);
        compact_persistent_notification = Pref.getBooleanDefaultFalse("compact_persistent_notification");
    }

/*
 * *************************************************************************************************************
 * Function for new notifications
 */

// TODO REFACTOR
    private void FileBasedNotifications(Context context) {
        ReadPerfs(context);
        Sensor sensor = Sensor.currentSensor();

        final BgReading bgReading = BgReading.last();
        if (bgReading == null) {
            // Sensor is stopped, or there is not enough data
            AlertPlayer.getPlayer().stopAlert(context, true, false);
            return;
        }

        final double calculated_value;
        if (use_best_glucose) {
            this.dg = BestGlucose.getDisplayGlucose();
            if (dg != null) {
                bgReading.calculated_value = dg.mgdl;
                calculated_value = dg.mgdl;
            } else {
                calculated_value = bgReading.calculated_value;
                Log.wtf(TAG, "Could not obtain best glucose value!");
            }
        } else {
            calculated_value = bgReading.calculated_value;
        }

        Log.d(TAG, "FileBasedNotifications called bgReading.calculated_value = " + bgReading.calculated_value + " calculated value: "+calculated_value);


        // TODO: tzachi what is the time of this last bgReading
        // TODO Tzachi: remove sensor != null once sensor data code is checked in.
        // If the last reading does not have a sensor, or that sensor was stopped.
        // or the sensor was started, but the 2 hours did not still pass? or there is no calibrations.
        // In all this cases, bgReading.calculated_value should be 0.
        if (((sensor != null) || (Home.get_follower())) && calculated_value != 0) {
            AlertType newAlert = AlertType.get_highest_active_alert(context, calculated_value);

            if (newAlert == null) {
                Log.d(TAG, "FileBasedNotifications - No active notifcation exists, stopping all alerts");
                // No alert should work, Stop all alerts, but keep the snoozing...
                AlertPlayer.getPlayer().stopAlert(context, false, true);
                return;
            }

            AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
            if (activeBgAlert == null) {
                Log.d(TAG, "FileBasedNotifications we have a new alert, starting to play it... " + newAlert.name);
                // We need to create a new alert  and start playing
                boolean trendingToAlertEnd = trendingToAlertEnd(context, true, newAlert);
                AlertPlayer.getPlayer().startAlert(context, trendingToAlertEnd, newAlert, EditAlertActivity.unitsConvert2Disp(doMgdl, calculated_value));
                return;
            }


            if (activeBgAlert.uuid.equals(newAlert.uuid)) {
                // This is the same alert. Might need to play again...

                //disable alert on stale data
                if(prefs.getBoolean("disable_alerts_stale_data", false)) {
                    int minutes = Integer.parseInt(prefs.getString("disable_alerts_stale_data_minutes", "15")) + 2;
                    if ((new Date().getTime()) - (60000 * minutes) - BgReading.lastNoSenssor().timestamp > 0) {
                        Log.d(TAG, "FileBasedNotifications : active alert found but not replaying it because more than three readings missed :  " + newAlert.name);
                        return;
                    }
                }

                Log.d(TAG, "FileBasedNotifications we have found an active alert, checking if we need to play it " + newAlert.name);
                boolean trendingToAlertEnd = trendingToAlertEnd(context, false, newAlert);
                AlertPlayer.getPlayer().ClockTick(context, trendingToAlertEnd, EditAlertActivity.unitsConvert2Disp(doMgdl, calculated_value));
                return;
            }
            // Currently the ui blocks having two alerts with the same alert value.

            boolean alertSnoozeOver = ActiveBgAlert.alertSnoozeOver();
            if (alertSnoozeOver) {
                Log.d(TAG, "FileBasedNotifications we had two alerts, the snoozed one is over, we fall down to deleting the snoozed and staring the new");
                // in such case it is not important which is higher.

            } else {
                // we have a new alert. If it is more important than the previous one. we need to stop
                // the older one and start a new one (We need to play even if we were snoozed).
                // If it is a lower level alert, we should keep being snoozed.


                // Example, if we have two alerts one for 90 and the other for 80. and we were already alerting for the 80
                // and we were snoozed. Now bg is 85, the alert for 80 is cleared, but we are alerting for 90.
                // We should not do anything if we are snoozed for the 80...
                // If one allert was high and the second one is low however, we alarm in any case (snoozing ignored).
                boolean opositeDirection = AlertType.OpositeDirection(activeBgAlert, newAlert);
                if(!opositeDirection) {
                AlertType newHigherAlert = AlertType.HigherAlert(activeBgAlert, newAlert);
                    if ((newHigherAlert == activeBgAlert)) {
                        // the existing (snoozed) alert is the higher, No need to play it since it is snoozed.
                        Log.d(TAG, "FileBasedNotifications The new alert has the same direcotion, it is lower than the one snoozed, not playing it." +
                              " newHigherAlert = " + newHigherAlert.name + "activeBgAlert = " + activeBgAlert.name);
                        return;
                    }
                }
            }
            // For now, we are stopping the old alert and starting a new one.
            Log.d(TAG, "Found a new alert, that is higher than the previous one will play it. " + newAlert.name);
            AlertPlayer.getPlayer().stopAlert(context, true, false);
            boolean trendingToAlertEnd = trendingToAlertEnd(context, true, newAlert);
            AlertPlayer.getPlayer().startAlert(context, trendingToAlertEnd, newAlert, EditAlertActivity.unitsConvert2Disp(doMgdl, calculated_value));
        } else {
            AlertPlayer.getPlayer().stopAlert(context, true, false);
        }
    }

    boolean trendingToAlertEnd(Context context, Boolean newAlert, AlertType Alert) {
        if (newAlert && !smart_alerting) {
            //  User does not want smart alerting at all.
            return false;
        }
        if ((!newAlert) && (!smart_snoozing)) {
            //  User does not want smart snoozing at all.
            return false;
        }
        return BgReading.trendingToAlertEnd(context, Alert.above);
    }
/*
 * *****************************************************************************************************************
 */

    // returns weather unclear bg reading was detected
    private boolean notificationSetter(Context context) {
        ReadPerfs(context);
        final long end = System.currentTimeMillis() + (60000 * 5);
        final long start = end - (60000 * 60 * 3) - (60000 * 10);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context, start, end);
        //BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        if (bg_ongoing) {
            bgOngoingNotification(bgGraphBuilder);
        }
        if (prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()) {
            Log.d("NOTIFICATIONS", "Notifications are currently disabled!!");
            return false;
        }
        
        boolean unclearReading = BgReading.getAndRaiseUnclearReading(context);

        boolean forced_wear = Home.get_forced_wear();
        Log.d(TAG, "forced_wear=" + forced_wear + " bg_notifications_watch=" + bg_notifications_watch + " persistent_high_alert_enabled_watch=" + bg_persistent_high_alert_enabled_watch);

        //boolean watchAlert = (Home.get_forced_wear() && bg_notifications_watch);
        if (unclearReading) {
            AlertPlayer.getPlayer().stopAlert(context, false, true);
        } else {
            FileBasedNotifications(context);
            BgReading.checkForDropAllert(context);
            BgReading.checkForRisingAllert(context);
        }
        // TODO: Add this alerts as well to depend on unclear sensor reading.
        //if (watchAlert && bg_persistent_high_alert_enabled_watch) {
        PersistentHigh.checkForPersistentHigh();
        evaluateLowPredictionAlarm();
        reportNoiseChanges();


        Sensor sensor = Sensor.currentSensor();
        // TODO need to check performance of rest of this method when in follower mode
        final List<BgReading> bgReadings = BgReading.latest(3);
        final List<Calibration> calibrations = Calibration.allForSensorLimited(3);
        if (bgReadings == null || bgReadings.size() < 3) {
            return unclearReading;
        }
        if (calibrations == null || calibrations.size() < 2) {
            return unclearReading;
        }
        BgReading bgReading = bgReadings.get(0);

        if (calibration_notifications) {

            int calibration_reminder_secs = 0;
            try {
                calibration_reminder_secs = Integer.parseInt(Pref.getString("calibration_reminder_hours","0")) * 60 * 60;
                Log.d(TAG,"Calibration reminder seconds: "+calibration_reminder_secs);
            } catch (Exception e) {
                Log.wtf(TAG,"Could not parse calibration_reminder_hours");
            }

            // TODO this should only clear double calibration once after calibrations are achieved
            if (bgReadings.size() >= 3) {
                if (calibrations.size() == 0 && (new Date().getTime() - bgReadings.get(2).timestamp <= (60000 * 30)) && sensor != null) {
                    if ((sensor.started_at + (60000 * 60 * 2)) < new Date().getTime()) {
                        doubleCalibrationRequest();
                    } else {
                        // TODO should be aware of state
                        clearDoubleCalibrationRequest();
                    }
                } else {
                    clearDoubleCalibrationRequest();
                }
            } else {
                clearDoubleCalibrationRequest();
            }
            // bgreadings criteria possibly needs a review
            if (CalibrationRequest.shouldRequestCalibration(bgReading) && (new Date().getTime() - bgReadings.get(2).timestamp <= (60000 * 24))) {
                if ((!PowerStateReceiver.is_power_connected()) || (Pref.getBooleanDefaultFalse("calibration_alerts_while_charging"))) {
                    if (JoH.pratelimit("calibration-request-notification", Math.max(CALIBRATION_REQUEST_MAX_FREQUENCY, calibration_reminder_secs))) {
                        extraCalibrationRequest();
                    }
                }
            } else {
                // TODO should be aware of state
                clearExtraCalibrationRequest();
            }
            // questionable use of abs for time since
            if (calibrations.size() >= 1 && (Math.abs(JoH.msSince(Math.max(calibrations.get(0).timestamp,
                    PersistentStore.getLong("last-calibration-pipe-timestamp")))) > (calibration_reminder_secs * 1000))
                    && (CalibrationRequest.isSlopeFlatEnough(BgReading.last(true)))) {
                Log.d("NOTIFICATIONS", "Calibration difference in hours: " + ((new Date().getTime() - calibrations.get(0).timestamp)) / (1000 * 60 * 60));
                if ((!PowerStateReceiver.is_power_connected()) || (Pref.getBooleanDefaultFalse("calibration_alerts_while_charging"))) {
                    if (JoH.pratelimit("calibration-request-notification", Math.max(CALIBRATION_REQUEST_MIN_FREQUENCY, calibration_reminder_secs)) || Pref.getBooleanDefaultFalse("calibration_alerts_repeat")) {
                        calibrationRequest();
                    }
                }
            } else {
                // TODO should be aware of state
                clearCalibrationRequest();
            }

        } else {
            clearAllCalibrationNotifications();
        }
        return unclearReading;
    }

    // This is the absolute time, not time from now.
    private long calcuatleArmTimeUnclearalert(Context ctx, long now, boolean unclearAlert) {
        if (!unclearAlert) {
            return Long.MAX_VALUE;
        }
        Long wakeTimeUnclear = Long.MAX_VALUE;

        UserNotification userNotification = UserNotification.GetNotificationByType("bg_unclear_readings_alert");
        if (userNotification == null) {
            // This is the case, that we are in unclear sensor reading, but for small time, so there is no call 
        	Log.i(TAG, "No active alert exists. returning Long.MAX_VALUE");
        	return Long.MAX_VALUE;
        } else {
            // This alert is snoozed
            // reminder - userNotification.timestamp is the time that the alert should be played again
            wakeTimeUnclear = (long)userNotification.timestamp;
        }
        
        if(wakeTimeUnclear < now ) {
            // we should alert now,
            wakeTimeUnclear = now;
        }
        if( wakeTimeUnclear == Long.MAX_VALUE) {
            // Should not happen
            Log.e(TAG ,"calcuatleArmTimeUnclearalert wakeTimeUnclear bad value setting it to one minute from now " + new Date(wakeTimeUnclear) + " in " +  ((wakeTimeUnclear - now)/60000d) + " minutes" );
            return now + 60 * 1000;
        }
        Log.w(TAG ,"calcuatleArmTimeUnclearalert returning " + new Date(wakeTimeUnclear) + " in " +  ((wakeTimeUnclear - now)/60000d) + " minutes" );
        return wakeTimeUnclear;
    }
    
    // This is the absolute time, not time from now.
    private long calcuatleArmTimeBg(long now) {
        Long wakeTimeBg = Long.MAX_VALUE;
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert != null) {
            AlertType alert = AlertType.get_alert(activeBgAlert.alert_uuid);
            if (alert != null) {
                wakeTimeBg = activeBgAlert.next_alert_at ;
                Log.d(TAG , "ArmTimer BG alert -waking at: "+ new Date(wakeTimeBg) +" in " +  (wakeTimeBg - now)/60000d + " minutes");
                if (wakeTimeBg < now) {
                    // next alert should be at least one minute from now.
                    wakeTimeBg = now + 60000;
                    Log.w(TAG , "setting next alert to 1 minute from now (no problem right now, but needs a fix someplace else)");
                }
                
            }
        }
        Log.d("Notifications" , "calcuatleArmTimeBg returning: "+ new Date(wakeTimeBg) +" in " +  (wakeTimeBg - now)/60000d + " minutes");
        return wakeTimeBg;
    }
    
    
    
 // This is the absolute time, not time from now.
    private long calcuatleArmTime(Context ctx, long now, boolean unclearAlert) {
        Long wakeTimeBg = calcuatleArmTimeBg(now);
        Long wakeTimeUnclear = calcuatleArmTimeUnclearalert(ctx, now, unclearAlert);
        Long wakeTime = Math.min(wakeTimeBg, wakeTimeUnclear);
        
        Log.d("Notifications" , "calcuatleArmTime returning: "+ new Date(wakeTime) +" in " +  (wakeTime - now)/60000d + " minutes");
        return wakeTime;

/*
 *
 *       leaving this code here since this is a code for a more correct calculation
 *       I guess that we will have to return to this once.
      // check snooze ending values
      long alerts_disabled_until = prefs.getLong("alerts_disabled_until", 0);
      if (alerts_disabled_until != 0) {
        wakeTime = Math.min(wakeTime, alerts_disabled_until);
      }
      long high_alerts_disabled_until = prefs.getLong("high_alerts_disabled_until", 0);
      if (high_alerts_disabled_until != 0) {
        wakeTime = Math.min(wakeTime, high_alerts_disabled_until);
      }
      long low_alerts_disabled_until = prefs.getLong("low_alerts_disabled_until", 0);
      if (low_alerts_disabled_until != 0) {
        wakeTime = Math.min(wakeTime, low_alerts_disabled_until);
      }

      // All this requires listeners on snooze changes...
      // check when the first alert should be fired. take care of that ???
  */
    }
    
    private synchronized void scheduleWakeup(Context context, boolean unclearAlert) {
       // Calendar calendar = Calendar.getInstance();
        final long now = JoH.tsl();
        long wakeTime = calcuatleArmTime(context, now, unclearAlert);

        // TODO make this neater - immediate wake time not needed as handled in JoH wakeup?
        if(wakeTime < now ) {
            Log.e("Notifications" , "ArmTimer recieved a negative time, will fire in 6 minutes");
            wakeTime = now + 6 * 60000;
        } else if  (wakeTime >=  now + 6 * 60000) {
        	 Log.i("Notifications" , "ArmTimer recieved a biger time, will fire in 6 minutes");
             wakeTime = now + 6 * 60000;
        }  else if (wakeTime == now) {
            Log.e("Notifications", "should arm right now, waiting one more second to avoid infinitue loop");
            wakeTime = now + 1000;
        }
        
        //AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);

        // TODO use JoH wakeup
        Log.d("Notifications" , "ArmTimer waking at: "+ new Date(wakeTime ) +" in " +
            (wakeTime - now) /60000d + " minutes");

        if (wakeIntent == null) {
            // TODO request code??
            wakeIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        }
        JoH.wakeUpIntent(context, wakeTime - now, wakeIntent);


       /* if (wakeIntent != null)
            alarm.cancel(wakeIntent);
        wakeIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, wakeIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, wakeIntent);
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, wakeIntent);
        }*/
    }

    private Bitmap createWearBitmap(long start, long end) {
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

    private Bitmap createWearBitmap(long hours) {
        return createWearBitmap(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
    }

   /* private Notification createExtensionPage(long hours) {
        return new NotificationCompat.Builder(mContext)
                .extend(new NotificationCompat.WearableExtender()
                                .setBackground(createWearBitmap(hours))
                                .setHintShowBackgroundOnly(true)
                                .setHintAvoidBackgroundClipping(true)
                )
                .build();
    }*/

    private boolean useOngoingChannel() {
        return (Pref.getBooleanDefaultFalse("use_notification_channels") &&
                Pref.getBooleanDefaultFalse("ongoing_notification_channel") &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }

    //@TargetApi(Build.VERSION_CODES.O)
    public synchronized Notification createOngoingNotification(BgGraphBuilder bgGraphBuilder, Context context) {
        mContext = context;
        ReadPerfs(mContext);
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

        //final NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, NotificationChannels.ONGOING_CHANNEL);
        //final NotificationCompat.Builder b = new NotificationCompat.Builder(mContext); // temporary fix until ONGOING CHANNEL is silent by default on android 8+
        //final Notification.Builder b = new Notification.Builder(mContext); // temporary fix until ONGOING CHANNEL is silent by default on android 8+
        final Notification.Builder b;
        if (useOngoingChannel() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(mContext, NotificationChannels.ONGOING_CHANNEL);
            b.setSound(null);
        } else {
            b = new Notification.Builder(mContext);
        }
        b.setOngoing(Pref.getBoolean("use_proper_ongoing", true));
        try {
            b.setGroup("xDrip ongoing");
        } catch (Exception e) {
            //
        }
        b.setVisibility(Pref.getBooleanDefaultFalse("public_notifications") ? Notification.VISIBILITY_PUBLIC : Notification.VISIBILITY_PRIVATE);
        b.setCategory(NotificationCompat.CATEGORY_STATUS);
        if (Pref.getBooleanDefaultFalse("high_priority_notifications")) {
            b.setPriority(Notification.PRIORITY_HIGH);
        }
        final BestGlucose.DisplayGlucose dg = (use_best_glucose) ? BestGlucose.getDisplayGlucose() : null;
        final boolean use_color_in_notification = false; // could be preference option
        final SpannableString titleString = new SpannableString(lastReading == null ? "BG Reading Unavailable" : (dg != null) ? (dg.spannableString(dg.unitized + " " + dg.delta_arrow,use_color_in_notification))
                : (lastReading.displayValue(mContext) + " " + lastReading.slopeArrow()));
        if (!compact_persistent_notification) {
            b.setContentTitle(titleString)
                    .setContentText("xDrip Data collection service is running.")
                    .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                    .setUsesChronometer(false);
        } else {
            b.setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                    .setUsesChronometer(false);
        }

        Bitmap numberIcon = null;

        // in case the graphic crashes the system-ui we wont do it immediately after reboot so the
        // user has a chance to disable the feature
        if (SystemClock.uptimeMillis() > Constants.MINUTE_IN_MS * 15) {
            if (NumberGraphic.numberIconEnabled()) {
                if ((dg != null) && (!dg.isStale())) {
                    final Bitmap icon_bitmap = NumberGraphic.getSmallIconBitmap(dg.unitized);
                    if (icon_bitmap != null) b.setSmallIcon(Icon.createWithBitmap(icon_bitmap));
                }
            }

            if (!compact_persistent_notification) {
                if (NumberGraphic.largeWithArrowEnabled()) {
                    if ((dg != null) && (!dg.isStale())) {
                        numberIcon = NumberGraphic.getLargeWithArrowBitmap(dg.unitized, dg.delta_arrow);
                    }
                } else if (NumberGraphic.largeNumberIconEnabled()) {
                    if ((dg != null) && (!dg.isStale())) {
                        numberIcon = NumberGraphic.getLargeIconBitmap(dg.unitized);
                    }
                }
            }
        }

        if (lastReading != null) {
            if (!compact_persistent_notification) {

                b.setWhen(lastReading.timestamp);
                b.setShowWhen(true);

                final SpannableString deltaString = new SpannableString("Delta: " + ((dg != null) ? (dg.spannableString(dg.unitized_delta + (dg.from_plugin ? " " + context.getString(R.string.p_in_circle) : "")))
                        : bgGraphBuilder.unitizedDeltaString(true, true)));

                b.setContentText(deltaString);

                notifiationBitmap = new BgSparklineBuilder(mContext)
                        .setBgGraphBuilder(bgGraphBuilder)
                        .showHighLine()
                        .showLowLine()
                        .setStart(System.currentTimeMillis() - 60000 * 60 * 3)
                        .showAxes(true)
                        .setBackgroundColor(getCol(X.color_notification_chart_background))
                        .setShowFiltered(DexCollectionType.hasFiltered() && Pref.getBooleanDefaultFalse("show_filtered_curve"))
                        .build();

                Notification.DecoratedCustomViewStyle customViewStyle = new Notification.DecoratedCustomViewStyle();

                iconBitmap = numberIcon != null ? numberIcon : new BgSparklineBuilder(mContext)
                        .setHeight(64)
                        .showLowLine()
                        .showHighLine()
                        .setStart(System.currentTimeMillis() - 60000 * 60 * 3)
                        .setBgGraphBuilder(bgGraphBuilder)
                        .setBackgroundColor(getCol(X.color_notification_chart_background))
                        .build();

                RemoteViews collapsedViews = new RemoteViews(context.getPackageName(), R.layout.notification_bg_collapsed);
                collapsedViews.setImageViewBitmap(R.id.notification_image, iconBitmap);
                collapsedViews.setTextViewText(R.id.notification_title, titleString);
                collapsedViews.setTextViewText(R.id.notification_summary, deltaString);

                RemoteViews expandedViews = new RemoteViews(context.getPackageName(), R.layout.notification_bg_expanded);
                expandedViews.setImageViewBitmap(R.id.notification_image, notifiationBitmap);
                expandedViews.setTextViewText(R.id.notification_title, titleString);
                expandedViews.setTextViewText(R.id.notification_summary, deltaString);

                b.setStyle(customViewStyle)
                        .setCustomContentView(collapsedViews)
                        .setCustomBigContentView(expandedViews);
            }
        }

        b.setContentIntent(resultPendingIntent);
        b.setLocalOnly(true);
        b.setOnlyAlertOnce(true);
        // strips channel ID if disabled
        return XdripNotification.build(b);
    }

    private synchronized void bgOngoingNotification(final BgGraphBuilder bgGraphBuilder) {
        try {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        NotificationManagerCompat
                                .from(mContext)
                                .notify(ongoingNotificationId, createOngoingNotification(bgGraphBuilder, mContext));
                        if (iconBitmap != null)
                            iconBitmap.recycle();
                        if (notifiationBitmap != null)
                            notifiationBitmap.recycle();
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Got runtime exception in bgOngoingNotification runnable: ", e);
                        Home.toaststaticnext("Problem displaying ongoing notification");
                    }
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Got runtime exception in bgOngoingNotification: ", e);
            Home.toaststaticnext("Problem displaying ongoing notification");
        }
    }

    private void soundAlert(String soundUri) {
        manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        Uri notification = Uri.parse(soundUri);
        MediaPlayer player = MediaPlayer.create(mContext, notification);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
        });
        player.start();
    }

    // Private helper: returns true if the system may block sound for an alert
    private static boolean isSoundBlockedBySystem(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int ringerMode = am.getRingerMode();
                if (ringerMode == AudioManager.RINGER_MODE_SILENT ||
                        ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    return true;
                }
            }

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                int filter = nm.getCurrentInterruptionFilter();
                if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                        filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                    return true;
                }
            }
        } catch (Exception e) {
            // If state cannot be determined, assume sound is not blocked
        }
        return false;
    }

    // TODO move to BgGraphBuilder?
    private void reportNoiseChanges()
    {
        final String this_noise_string=BgGraphBuilder.noiseString(BgGraphBuilder.last_noise);
        if (!BgGraphBuilder.noiseString(BgGraphBuilder.last_noise).equals(last_noise_string))
        {
            Log.uel("Noise","Changed from: "+last_noise_string+" to "+this_noise_string);
            last_noise_string = this_noise_string;
        }
    }

    private void evaluateLowPredictionAlarm() {

        if (!prefs.getBoolean("predict_lows_alarm", false)) return;


        // force BgGraphBuilder to calculate `low_occurs_at` and `last_noise`
        // Workaround trying to resolve race donditions as by design they are static but updated/read asynchronously.

        final double low_occurs_at = BgGraphBuilder.getCurrentLowOccursAt();

        if ((low_occurs_at > 0) && (BgGraphBuilder.last_noise < BgGraphBuilder.NOISE_TOO_HIGH_FOR_PREDICT)) {
            final double low_predicted_alarm_minutes = JoH.tolerantParseDouble(prefs.getString("low_predict_alarm_level", "40"), 40);
            final double now = JoH.ts();
            final double predicted_low_in_mins = (low_occurs_at - now) / 60000;
            android.util.Log.d(TAG, "evaluateLowPredictionAlarm: mins: " + predicted_low_in_mins);
            if (predicted_low_in_mins > 1) {
                if (predicted_low_in_mins < low_predicted_alarm_minutes) {
                    Notifications.lowPredictAlert(xdrip.getAppContext(), true, getString(R.string.low_predicted)
                            +" "+getString(R.string.in)+" " + (int) predicted_low_in_mins + getString(R.string.space_mins));
                    low_notifying = true;
                } else {
                    Notifications.lowPredictAlert(xdrip.getAppContext(), false, ""); // cancel it
                }
            } else {
                if (low_notifying) {
                    Notifications.lowPredictAlert(xdrip.getAppContext(), false, ""); // cancel it
                    low_notifying = false;
                }
            }
        } else {
            if (low_notifying) {
                Notifications.lowPredictAlert(xdrip.getAppContext(), false, ""); // cancel it
                low_notifying = false;
            }
        }
    }

    private void clearAllCalibrationNotifications() {
        notificationDismiss(calibrationNotificationId);
        notificationDismiss(extraCalibrationNotificationId);
        notificationDismiss(doubleCalibrationNotificationId);
    }

    private void calibrationNotificationCreate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent, NotificationChannels.CALIBRATION_CHANNEL);
        mBuilder.setVisibility(Pref.getBooleanDefaultFalse("public_notifications") ? Notification.VISIBILITY_PUBLIC : Notification.VISIBILITY_PRIVATE);
        mBuilder.setVibrate(vibratePattern);
        mBuilder.setLights(0xff00ff00, 300, 1000);
        if(calibration_override_silent) {
            mBuilder.setSound(Uri.parse(calibration_notification_sound), AudioAttributes.USAGE_ALARM);
        } else {
            mBuilder.setSound(Uri.parse(calibration_notification_sound));
        }



        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        //mNotifyMgr.cancel(notificationId);
        mNotifyMgr.notify(notificationId, XdripNotificationCompat.build(mBuilder));
    }

    private NotificationCompat.Builder notificationBuilder(String title, String content, Intent intent) {
        return notificationBuilder(title, content, intent, null);
    }

    private NotificationCompat.Builder notificationBuilder(String title, String content, Intent intent, String channelId) {
        return new NotificationCompat.Builder(mContext, channelId)
                .setVisibility(Pref.getBooleanDefaultFalse("public_notifications") ? Notification.VISIBILITY_PUBLIC : Notification.VISIBILITY_PRIVATE)
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(Pref.getBooleanDefaultFalse("high_priority_notifications") ? Notification.PRIORITY_MAX : Notification.PRIORITY_HIGH)
                .setContentIntent(notificationIntent(intent));
    }

    private PendingIntent notificationIntent(Intent intent){
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void notificationDismiss(int notificationId) {
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
    }

    private void calibrationRequest() {
        UserNotification userNotification = UserNotification.lastCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) {
                userNotification.delete();
            }
            final long calibration_hours = Calibration.msSinceLastCalibration() / (1000 * 60 * 60);
            UserNotification.create(calibration_hours + " hours since last Calibration  (@" + JoH.hourMinuteString() + ")", "calibration_alert", new Date().getTime());
            String title = "Calibration Needed";
            String content = calibration_hours + " hours since last calibration";
            Intent intent = new Intent(mContext, AddCalibration.class);
            calibrationNotificationCreate(title, content, intent, calibrationNotificationId);
        }
    }

    private void doubleCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification.create("Double Calibration", "double_calibration_alert", new Date().getTime());
            String title = "Sensor is ready";
            String content = getString(R.string.sensor_is_ready_please_enter_double_calibration) + "  (@" + JoH.hourMinuteString() + ")";
            Intent intent = new Intent(mContext, DoubleCalibrationActivity.class);
            calibrationNotificationCreate(title, content, intent, calibrationNotificationId);
        }
    }

    private void extraCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification.create("Extra Calibration Requested", "extra_calibration_alert", new Date().getTime());
            String title = "Calibration Requested";
            String content = "Increase performance by calibrating now" + "  (@" + JoH.hourMinuteString() + ")";
            Intent intent = new Intent(mContext, AddCalibration.class);
            calibrationNotificationCreate(title, content, intent, extraCalibrationNotificationId);
        }
    }

    public static void bgUnclearAlert(Context context) {
        long otherAlertReraiseSec = MissedReadingService.getOtherAlertReraiseSec(context, "bg_unclear_readings_alert");
        OtherAlert(context, "bg_unclear_readings_alert", "Unclear Sensor Readings" + "  (@" + JoH.hourMinuteString() + ")", uncleanAlertNotificationId, NotificationChannels.BG_ALERT_CHANNEL, true, otherAlertReraiseSec);
    }

    public static void bgMissedAlert(Context context) {
        final String type = "bg_missed_alerts";
        long otherAlertReraiseSec = MissedReadingService.getOtherAlertReraiseSec(context, type);
        OtherAlert(context, type, context.getString(R.string.bg_reading_missed) + "  (@" + JoH.hourMinuteString() + ")", missedAlertNotificationId, NotificationChannels.BG_MISSED_ALERT_CHANNEL, true, otherAlertReraiseSec);
    }

    public static void ob1SessionRestartRequested() {
        Context context = xdrip.getAppContext();
        OtherAlert(context, "ob1_session_restart", context.getString(R.string.ob1_session_restarted_title), context.getString(R.string.ob1_session_restarted_msg), ob1SessionRestartNotificationId, NotificationChannels.CALIBRATION_CHANNEL, true, 0);
    }

    public static void RisingAlert(Context context, boolean on) {
        RiseDropAlert(context, on, "bg_rise_alert", context.getString(R.string.bg_rising_fast) + "  (@" + JoH.hourMinuteString() + ")", riseAlertNotificationId);
    }
    public static void DropAlert(Context context, boolean on) {
        RiseDropAlert(context, on, "bg_fall_alert", context.getString(R.string.bg_falling_fast) + "  (@" + JoH.hourMinuteString() + ")", failAlertNotificationId);
    }

    public static void lowPredictAlert(Context context, boolean on, String msg) {
        final String type = "bg_predict_alert";
        if (on) {
            if ((Pref.getLong("alerts_disabled_until", 0) < JoH.tsl()) && (Pref.getLong("low_alerts_disabled_until", 0) < JoH.tsl())) {
                OtherAlert(context, type, msg, lowPredictAlertNotificationId, NotificationChannels.BG_PREDICTED_LOW_CHANNEL, false, 20 * 60);
                if (Pref.getBooleanDefaultFalse("speak_alerts")) {
                   if (JoH.pratelimit("low-predict-speak", 1800)) SpeechUtil.say(msg, 4000);
                }
            } else {
                Log.ueh(TAG, "Not Low predict alerting due to snooze: " + msg);
            }
        } else {
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(lowPredictAlertNotificationId);
            UserNotification.DeleteNotificationByType(type);
        }
    }

    public static void persistentHighAlert(Context context, boolean on, String msg) {
        final String type = "persistent_high_alert";
        if (on) {
            if ((Pref.getLong("alerts_disabled_until", 0) < JoH.tsl()) && (Pref.getLong("high_alerts_disabled_until", 0) < JoH.tsl())) {
                int snooze_time = 20;
                try {
                    snooze_time = Integer.parseInt(Pref.getString("persistent_high_repeat_mins", "20"));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid snooze time for persistent high");
                }
                if (snooze_time < 1) snooze_time = 1;       // not less than 1 minute
                if (snooze_time > 1440) snooze_time = 1440; // not more than 1 day
                OtherAlert(context, type, msg, persistentHighAlertNotificationId, NotificationChannels.BG_PERSISTENT_HIGH_CHANNEL, false, snooze_time * 60);
                if (Pref.getBooleanDefaultFalse("speak_alerts")) {
                    if (JoH.pratelimit("persist-high-speak", 1800)) {
                        SpeechUtil.say(msg, 4000);
                    }
                }
            } else {
                Log.ueh(TAG, "Not persistent high alerting due to snooze: " + msg);
            }
        } else {
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(persistentHighAlertNotificationId);
            UserNotification.DeleteNotificationByType(type);
        }
    }

    public static void RiseDropAlert(Context context, boolean on, String type, String message, int notificatioId) {
        if(on) {
         // This alerts will only happen once. Want to have maxint, but not create overflow.
            OtherAlert(context, type, message, notificatioId, NotificationChannels.BG_RISE_DROP_CHANNEL, false, Integer.MAX_VALUE / 100000);
        } else {
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(notificatioId);
            UserNotification.DeleteNotificationByType(type);
        }
    }

    private static void OtherAlert(Context context, String type, String message, int notificatioId, String channelId, boolean addDeleteIntent, long reraiseSec) {
        OtherAlert(context, type, message, message, notificatioId, channelId, addDeleteIntent, reraiseSec);
    }

    private static void OtherAlert(Context context, String type, String title, String message, int notificatioId, String channelId, boolean addDeleteIntent, long reraiseSec) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String otherAlertsSound = prefs.getString(type+"_sound",prefs.getString("other_alerts_sound", "content://settings/system/notification_sound"));
        Boolean otherAlertsOverrideSilent = prefs.getBoolean("other_alerts_override_silent", false);
        Boolean extraAlertsOverrideSilent = prefs.getBoolean(type+"_override_silent", otherAlertsOverrideSilent); // Inherit from other alerts if the alert itself does not have a dedicated setting

        Log.d(TAG,"OtherAlert called " + type + " " + message + " reraiseSec = " + reraiseSec);
        UserNotification userNotification = UserNotification.GetNotificationByType(type); //"bg_unclear_readings_alert"
        if ((userNotification == null) || userNotification.timestamp <= new Date().getTime() ) {
            if (userNotification != null) {
                try {
                    userNotification.delete();
                } catch (NullPointerException e) {
                    // ignore null pointer exception during delete as we emulate database records
                }
                Log.d(TAG, "Delete");
            }
            UserNotification.create(message, type, new Date().getTime() + reraiseSec * 1000);


            boolean localOnly =false;
            if (notificatioId == persistentHighAlertNotificationId) {
                localOnly = (Home.get_forced_wear() && bg_notifications_watch && bg_persistent_high_alert_enabled_watch);
            }
            Log.d(TAG,"OtherAlert forced_wear localOnly=" + localOnly);
            Intent intent = new Intent(context, Home.class);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, channelId)
                            .setVisibility(Pref.getBooleanDefaultFalse("public_notifications") ? Notification.VISIBILITY_PUBLIC : Notification.VISIBILITY_PRIVATE)
                            .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setLocalOnly(localOnly)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                            .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            if (addDeleteIntent) {
                Intent deleteIntent = new Intent(context, SnoozeOnNotificationDismissService.class);
                deleteIntent.putExtra("alertType", type);
                deleteIntent.putExtra("raisedTimeStamp", JoH.tsl());
                mBuilder.setDeleteIntent(PendingIntent.getService(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            }
            mBuilder.setVibrate(vibratePattern);
            mBuilder.setLights(0xff00ff00, 300, 1000);
            if (AlertPlayer.notSilencedDueToCall()) {
                if (extraAlertsOverrideSilent) {
                    mBuilder.setSound(Uri.parse(otherAlertsSound), AudioAttributes.USAGE_ALARM);
                } else {
                    mBuilder.setSound(Uri.parse(otherAlertsSound));
                    if (isSoundBlockedBySystem(context)) {
                        Log.ueh(TAG, "No " + type + " in silent mode");
                    }
                }
            }
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            //mNotifyMgr.cancel(notificatioId);
            //Log.d(TAG, "Notify");
            Log.ueh("Other Alert",message);
            mNotifyMgr.notify(notificatioId, XdripNotificationCompat.build(mBuilder));

            if (Pref.getBooleanDefaultFalse("pref_amazfit_enable_key")
                    && Pref.getBooleanDefaultFalse("pref_amazfit_other_alert_enable_key")) {
                Amazfitservice.start("xDrip_Otheralert", message, 30);
            }

            BroadcastEntry.sendAlert(type, message);
        }
    }

    private void clearCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(calibrationNotificationId);
        }
    }

    private void clearDoubleCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(doubleCalibrationNotificationId);
        }
    }

    private void clearExtraCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(extraCalibrationNotificationId);
        }
    }

    // rate limited
    public static void start() {
        // TODO consider how inevitable task could change dynamic of this instead of rate limit
        if (JoH.ratelimit("start-notifications",10)) {
            JoH.startService(Notifications.class);
        }
    }

    // not rate limited - force recheck
    public static void staticUpdateNotification() {
        try {
            JoH.startService(Notifications.class);
        } catch (Exception e) {
            Log.e(TAG, "Got exception in staticupdatenotification: " + e);
        }
    }
}
