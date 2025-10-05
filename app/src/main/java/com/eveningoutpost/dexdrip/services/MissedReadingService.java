package com.eveningoutpost.dexdrip.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.DesertSync;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Reminder;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.utilitymodels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.healthconnect.HealthConnectEntry;
import com.eveningoutpost.dexdrip.insulin.inpen.InPenEntry;
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.watch.lefun.LeFun;
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry;
import com.eveningoutpost.dexdrip.services.broadcastservice.BroadcastEntry;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getLocalServiceCollectingState;

public class MissedReadingService extends IntentService {
    //int otherAlertSnooze;
    private final static String TAG = MissedReadingService.class.getSimpleName();
    private static volatile PendingIntent serviceIntent = null;
    private static int aggressive_backoff_timer = 120;

    public MissedReadingService() {
        super("MissedReadingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final boolean bg_missed_alerts;

        final PowerManager.WakeLock wl = JoH.getWakeLock("missed-reading-service", 60000);
        try {

            final boolean sensorActive = Sensor.isActive();
            final boolean weAreAFollower = DexCollectionType.getDexCollectionType() == DexCollectionType.Follower; // For now, this is true only for xDrip Sync Follower

            Log.d(TAG, "MissedReadingService onHandleIntent"); // test debug log

            final long stale_millis = Home.stale_data_millis();


            // send to pebble
            if (Pref.getBoolean("broadcast_to_pebble", false) && (PebbleUtil.getCurrentPebbleSyncType() != 1) && !BgReading.last_within_millis(stale_millis)) {
                if (JoH.ratelimit("peb-miss", 120)) {
                    // TODO replace ratelimit with Inevitable.task?
                    JoH.startService(PebbleWatchSync.class);
                }
                // update pebble even when we don't have data to ensure missed readings show
            }

            if (LeFunEntry.isEnabled() && (!BgReading.last_within_millis(stale_millis))) {
                LeFun.showLatestBG();
            }

            if (BroadcastEntry.isEnabled() && (!BgReading.last_within_millis(stale_millis))) {
                BroadcastEntry.sendLatestBG();
            }


            if ((Pref.getBoolean("aggressive_service_restart", false) || DexCollectionType.isFlakey())) {//!Home.get_enable_wear() &&
                if (!BgReading.last_within_millis(stale_millis) && sensorActive && (!getLocalServiceCollectingState())) {
                    if (JoH.ratelimit("aggressive-restart", aggressive_backoff_timer)) {
                        Log.e(TAG, "Aggressively restarting collector service due to lack of reception: backoff: " + aggressive_backoff_timer);
                        if (aggressive_backoff_timer < 1200) aggressive_backoff_timer += 60;
                        CollectionServiceStarter.restartCollectionServiceBackground();
                    } else {
                        aggressive_backoff_timer = 120; // reset
                    }
                }
            }


            Reminder.processAnyDueReminders();
            BluetoothGlucoseMeter.immortality();
            XdripWebService.immortality(); //
            InPenEntry.immortality();
            DesertSync.pullAsEnabled();
            NanoStatus.keepFollowerUpdated();
            LockScreenWallPaper.timerPoll();
            HealthConnectEntry.ping();

            // TODO functionalize the actual checking
            bg_missed_alerts = Pref.getBoolean("bg_missed_alerts", false);
            if (!bg_missed_alerts) {
                // we should not do anything in this case. if the ui, changes will be called again
                return;
            }
            if (!sensorActive && !weAreAFollower) {
                // sensor not running we should return if we are not a follower
                UserError.Log.d(TAG, "Not processing missed reading alert with no active sensor and not following"); // If this is issued, we may need to expand the list of followers (companion app? Carelink? ...)
                return;
            }

            if (!JoH.upForAtLeastMins(15)) {
                Log.d(TAG, "Uptime less than 15 minutes so not processing for missed reading");
                return;
            }


            if ((Home.get_forced_wear()) && Pref.getBoolean("disable_wearG5_on_missedreadings", false)) {
                int bg_wear_missed_minutes = Pref.getStringToInt("disable_wearG5_on_missedreadings_level", 30);
                if (BgReading.getTimeSinceLastReading() >= (bg_wear_missed_minutes * 1000 * 60)) {
                    Log.d(TAG, "Request WatchUpdaterService to disable force_wearG5 when wear is connected");
                    startWatchUpdaterService(xdrip.getAppContext(), WatchUpdaterService.ACTION_DISABLE_FORCE_WEAR, TAG);
                }
            }

            final int bg_missed_minutes = Pref.getStringToInt("bg_missed_minutes", 30);
            final long now = JoH.tsl();

            // check if readings have been missed
            if (BgReading.getTimeSinceLastReading() >= (bg_missed_minutes * 1000 * 60) &&
                    Pref.getLong("alerts_disabled_until", 0) <= now &&
                    (BgReading.getTimeSinceLastReading() < (Constants.HOUR_IN_MS * 6)) &&
                    inTimeFrame()) {
                Notifications.bgMissedAlert(xdrip.getAppContext());
                checkBackAfterSnoozeTime(xdrip.getAppContext(), now);
            } else {

                long disabletime = Pref.getLong("alerts_disabled_until", 0) - now;

                long missedTime = bg_missed_minutes * 1000 * 60 - BgReading.getTimeSinceLastReading();
                long alarmIn = Math.max(disabletime, missedTime);
                checkBackAfterMissedTime(alarmIn);
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private boolean inTimeFrame() {

        int startMinutes = Pref.getInt("missed_readings_start", 0);
        int endMinutes = Pref.getInt("missed_readings_end", 0);
        boolean allDay = Pref.getBoolean("missed_readings_all_day", true);

        return AlertType.s_in_time_frame(allDay, startMinutes, endMinutes);
    }

    private void checkBackAfterSnoozeTime(Context context, long now) {
        // This is not 100% accurate, need to take in account also the time of when this alert was snoozed.
        UserNotification userNotification = UserNotification.GetNotificationByType("bg_missed_alerts");
        if (userNotification == null) {
            // No active alert exists, should not happen, we have just created it.
            Log.wtf(TAG, "No active alert exists.");
            setAlarm(getOtherAlertReraiseSec(context, "bg_missed_alerts") * 1000, false);
        } else {
            // we have an alert that should be re-raised on userNotification.timestamp
            long alarmIn = (long) userNotification.timestamp - now;
            if (alarmIn < 0) {
                alarmIn = 0;
            }
            setAlarm(alarmIn, true);
        }
    }

    private void checkBackAfterMissedTime(long alarmIn) {
        setAlarm(alarmIn, false);
    }

    // alarmIn is relative time ms
    public void setAlarm(long alarmIn, boolean force) {
        if (!force && (alarmIn < 5 * 60 * 1000)) {
            // No need to check more than once every 5 minutes
            alarmIn = 5 * 60 * 1000;
        }

        alarmIn = Math.max(alarmIn, 5000); // don't try to set less than 5 seconds in the future

        Log.d(TAG, "Setting timer to  " + alarmIn / 60000 + " minutes from now");

        initializeServiceIntent();
        JoH.wakeUpIntent(this, alarmIn, serviceIntent);
    }

    // create the static pending intent if needed
    private void initializeServiceIntent() {
        if (serviceIntent == null) {
            synchronized (this) {
                if (serviceIntent == null) {
                    serviceIntent = PendingIntent.getService(this, Constants.MISSED_READING_SERVICE_ID, new Intent(this, this.getClass()), 0);
                }
            }
        }
    }

    static public long getOtherAlertReraiseSec(Context context, String alertName) {
        boolean enableAlertsReraise = Pref.getBoolean(alertName + "_enable_alerts_reraise", false);
        if (enableAlertsReraise) {
            return Pref.getStringToInt(alertName + "_reraise_sec", 60);
        } else {
            return 60 * getOtherAlertSnoozeMinutes(PreferenceManager.getDefaultSharedPreferences(context), alertName);
        }

    }

    static public long getOtherAlertSnoozeMinutes(SharedPreferences prefs, String alertName) {
        int defaultSnooze = Pref.getStringToInt("other_alerts_snooze", 20);
        return Pref.getStringToInt(alertName + "_snooze", defaultSnooze);
    }


    public static void delayedLaunch() {
        Inevitable.task("launch-missed-readings", 1000, () -> JoH.startService(MissedReadingService.class));
    }
}
