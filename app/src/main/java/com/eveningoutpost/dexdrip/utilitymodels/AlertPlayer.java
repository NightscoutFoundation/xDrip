package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.models.JoH.delayedMediaPlayerRelease;
import static com.eveningoutpost.dexdrip.models.JoH.setMediaDataSource;
import static com.eveningoutpost.dexdrip.models.JoH.stopAndReleasePlayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.SnoozeOnNotificationDismissService;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.utilitymodels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.eassist.AlertTracker;
import com.eveningoutpost.dexdrip.ui.FlashLight;
import com.eveningoutpost.dexdrip.ui.helpers.AudioFocusType;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.watch.lefun.LeFun;
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry;
import com.eveningoutpost.dexdrip.watch.miband.MiBand;
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice;
import com.eveningoutpost.dexdrip.services.broadcastservice.BroadcastEntry;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.services.broadcastservice.Const;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Date;

import lombok.Getter;


// A helper class to create the mediaplayer on the UI thread.
// This is needed in order for the callbackst to work.
class MediaPlayerCreaterHelper {
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    private final Object creationThreadLock = new Object();
    private volatile boolean mplayerCreated_ = false;
    private volatile MediaPlayer mediaPlayer_ = null;
    
    synchronized MediaPlayer createMediaPlayer(Context ctx) {
        if (isUiThread()) {
            return new MediaPlayer();
        }
        
        mplayerCreated_ = false;
        mediaPlayer_ = null;
        Handler mainHandler = new Handler(ctx.getMainLooper());

        // TODO use JoH run on ui thread
        Runnable myRunnable = new Runnable() {
            @Override 
            public void run() {
                synchronized(creationThreadLock) {
                    try {
                        mediaPlayer_ = new MediaPlayer();
                        Log.i(TAG, "media player created");
                    } finally {
                        mplayerCreated_ = true;
                        creationThreadLock.notifyAll();
                    }
                    
                }
            }
        };
        mainHandler.post(myRunnable);


        try {
            synchronized(creationThreadLock) {
                // TODO thread deadlock possible here?
                while(mplayerCreated_ == false) {
                    creationThreadLock.wait(30 * Constants.SECOND_IN_MS);
                }
            } 
        }catch (InterruptedException e){
             Log.e(TAG, "Cought exception", e);
        }
        return mediaPlayer_;
    }
    
    boolean isUiThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}

public class AlertPlayer {

    private volatile static AlertPlayer alertPlayerInstance;
    @Getter
    private volatile static long lastVolumeChange = 0;
    private final static String TAG = AlertPlayer.class.getSimpleName();
    private volatile MediaPlayer mediaPlayer = null;
    private final AudioManager manager = (AudioManager)xdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
    volatile int volumeBeforeAlert = -1;
    volatile int volumeForThisAlert = -1;

    final static int ALERT_PROFILE_HIGH = 1;
    final static int ALERT_PROFILE_ASCENDING = 2;
    final static int ALERT_PROFILE_MEDIUM = 3;
    final static int ALERT_PROFILE_VIBRATE_ONLY = 4;
    final static int ALERT_PROFILE_SILENT = 5;

    // when ascending how many minutes since alert started do we wait before escalating
    final static int MAX_VIBRATING_MINUTES = 2;
    final static int MAX_ASCENDING_MINUTES = 5;

    // Ascending without delay profile
    final static float NODELAY_ASCENDING_INTERCEPT = 0.3333f; // Lower volumes are silent on some phones.
    final static float NODELAY_ASCENDING_SLOPE = 0.166675f; // So that the volume reaches 1 (max) in 4 steps (1-2-3-4-5) since in most cases, we have a new reading once every 5 minutes.

    public int streamType = AudioManager.STREAM_MUSIC;

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            focusChange -> Log.d(TAG, "Audio focus changes to: " + focusChange);

    public static synchronized AlertPlayer getPlayer() {
        if(alertPlayerInstance == null) {
            Log.i(TAG,"getPlayer: Creating a new AlertPlayer");
            alertPlayerInstance = new AlertPlayer();
        } else {
            Log.i(TAG,"getPlayer: Using existing AlertPlayer");
        }
        return alertPlayerInstance;
    }

    public static void defaultSnooze() {
        AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);
    }

    private void requestAudioFocus() {
        try {
            final int focus = AudioFocusType.getAlarmAudioFocusType();
            if (focus != 0) {
                UserError.Log.d(TAG, "Calling request audio focus");
                manager.requestAudioFocus(focusChangeListener, streamType, focus);
            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Failed to get audio focus: " + e);
        }
    }

    private void releaseAudioFocus() {
        UserError.Log.d(TAG, "Calling release audio focus");
        try {
            manager.abandonAudioFocus(focusChangeListener);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Failed to release audio focus: " + e);
        }
    }

    public synchronized void startAlert(Context ctx, boolean trendingToAlertEnd, AlertType newAlert, String bgValue) {
        startAlert(ctx, trendingToAlertEnd, newAlert, bgValue, Pref.getBooleanDefaultFalse("start_snoozed")); // for start snoozed by default!
    }

    public synchronized  void startAlert(Context ctx, boolean trendingToAlertEnd, AlertType newAlert, String bgValue , boolean start_snoozed)  {
        Log.d(TAG, "startAlert called, Threadid " + Thread.currentThread().getId());
        if (trendingToAlertEnd) {
            Log.d(TAG, "startAlert: This alert is trending to it's end will not do anything");
            return;
        }

        stopAlert(ctx, true, false);

        long nextAlertTime = newAlert.getNextAlertTime(ctx);

        ActiveBgAlert.Create(newAlert.uuid, start_snoozed, nextAlertTime);
        if (!start_snoozed) VibrateNotifyMakeNoise(ctx, newAlert, bgValue, 0);
        AlertTracker.evaluate();
    }

    public synchronized void stopAlert(Context ctx, boolean ClearData, boolean clearIfSnoozeFinished) {
        stopAlert(ctx, ClearData, clearIfSnoozeFinished, true);
    }

    public synchronized void stopAlert(Context ctx, boolean ClearData, boolean clearIfSnoozeFinished, boolean cancelNotification) {

        Log.d(TAG, "stopAlert: stop called ClearData " + ClearData + "  ThreadID " + Thread.currentThread().getId());
        if (ClearData) {
            ActiveBgAlert.ClearData();
        }
        if (clearIfSnoozeFinished) {
            ActiveBgAlert.ClearIfSnoozeFinished();
        }
        if (cancelNotification) {
            notificationDismiss(ctx);
        }
        if (mediaPlayer != null) {
            stopAndReleasePlayer(mediaPlayer);
            mediaPlayer = null;
        }
        revertCurrentVolume(streamType);
        releaseAudioFocus();
    }

    // only do something if an alert is active - only call from interactive
    public synchronized boolean OpportunisticSnooze() {
        if (JoH.ratelimit("opp-snooze-check", 3)) {
            if (ActiveBgAlert.getOnly() != null) {
                // there is an alert so do something
                UserError.Log.ueh(TAG, "Opportunistic snooze attempted to snooze alert");
                Snooze(xdrip.getAppContext(), -1);
                if (JoH.ratelimit("opportunistic-snooze-toast", 300)) {
                    JoH.static_toast_long("Opportunistic Snooze");
                }
                return true;
            }
        }
        return false;
    }

    //  default signature for user initiated interactive snoozes only
    public synchronized void Snooze(Context ctx, int repeatTime) {
        Snooze(ctx, repeatTime, true);

        BlueJayEntry.cancelNotifyIfEnabled();

        if (Pref.getBooleanDefaultFalse("bg_notifications_watch") ) {
            startWatchUpdaterService(ctx, WatchUpdaterService.ACTION_SNOOZE_ALERT, TAG, "repeatTime", "" + repeatTime);
        }
        if (Pref.getBooleanDefaultFalse("pref_amazfit_enable_key")
                && Pref.getBooleanDefaultFalse("pref_amazfit_BG_alert_enable_key")) {
            Amazfitservice.start("xDrip_AlarmCancel");
        }

        BroadcastEntry.cancelAlert();
    }

    public synchronized void Snooze(Context ctx, int repeatTime, boolean from_interactive) {
        Log.i(TAG, "Snooze called repeatTime = " + repeatTime);
        stopAlert(ctx, false, false);
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert == null) {
            Log.e(TAG, "Error, snooze was called but no alert is active.");
            if (from_interactive) GcmActivity.sendSnoozeToRemote();
            return;
        }
        if (repeatTime == -1) {
            repeatTime = GuessDefaultSnoozeTime();
        }
        activeBgAlert.snooze(repeatTime);
        if (from_interactive) GcmActivity.sendSnoozeToRemote();
    }

    public synchronized int GuessDefaultSnoozeTime() {
        int repeatTime;
        // try to work out default
        AlertType alert = ActiveBgAlert.alertTypegetOnly();
        if (alert != null) {
            repeatTime = alert.default_snooze;
            Log.d(TAG, "Selecting default snooze time: " + repeatTime);
        } else {
            repeatTime = 30; // pick a number if we cannot even find the default
            Log.e(TAG, "Cannot even find default snooze time so going with: " + repeatTime);
        }

        return repeatTime;
    }

    public synchronized void PreSnooze(Context ctx, String uuid, int repeatTime) {
        Log.i(TAG, "PreSnooze called repeatTime = "+ repeatTime);
        stopAlert(ctx, true, false);
        ActiveBgAlert.Create(uuid, true, new Date().getTime() + repeatTime * 60000);
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            Log.wtf(TAG, "Just created the alert, where did it go...");
            return;
        }
        activeBgAlert.snooze(repeatTime);
    }

    // Check the state and alarm if needed
    public void ClockTick(Context ctx, boolean trendingToAlertEnd, String bgValue)
    {
        if (trendingToAlertEnd) {
            Log.d(TAG,"ClockTick: This alert is trending to it's end will not do anything");
            return;
        }
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            // Nothing to do ...
            return;
        }
        if(activeBgAlert.ready_to_alarm()) {
            stopAlert(ctx, false, false, false); // also don't cancel notification

            final int minutesFromStartPlaying = activeBgAlert.getAndUpdateAlertingMinutes();
            final AlertType alert = AlertType.get_alert(activeBgAlert.alert_uuid);
            if (alert == null) {
                Log.d(TAG, "ClockTick: The alert was already deleted... will not play");
                ActiveBgAlert.ClearData();
                return;
            }
            Log.d(TAG,"ClockTick: Playing the alert again");
            long nextAlertTime = alert.getNextAlertTime(ctx);
            activeBgAlert.updateNextAlertAt(nextAlertTime);
            
            VibrateNotifyMakeNoise(ctx, alert, bgValue, minutesFromStartPlaying);
            AlertTracker.evaluate();
        }

    }


    protected synchronized void playFile(final Context ctx, final String fileName, final float volumeFrac, final boolean forceSpeaker, final boolean overrideSilentMode) {
        Log.i(TAG, "playFile: called fileName = " + fileName);
        if (volumeFrac <= 0) {
            UserError.Log.e(TAG, "Not playing file " + fileName + " as requested volume is " + volumeFrac);
            return;
        }

        if (mediaPlayer != null) {
            Log.i(TAG, "ERROR, playFile sound already playing");
            stopAndReleasePlayer(mediaPlayer);
        }

        mediaPlayer = new MediaPlayerCreaterHelper().createMediaPlayer(ctx);
        if (mediaPlayer == null) {
            Log.wtf(TAG, "MediaPlayerCreaterHelper().createMediaPlayer failed !!");
            return;
        }

        if (Pref.getBooleanDefaultFalse("wake_phone_during_alerts")) {
            mediaPlayer.setWakeMode(ctx, PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP);
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.i(TAG, "playFile: onCompletion called (finished playing) ");
            delayedMediaPlayerRelease(mp);
            JoH.threadSleep(300);
            revertCurrentVolume(streamType);
            releaseAudioFocus();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "playFile: onError called (what: " + what + ", extra: " + extra);
            // possibly media player error; release is handled in onCompletionListener
            return false;
        });

        boolean setDataSourceSucceeded = false;
        if (fileName != null && fileName.length() > 0) {
            setDataSourceSucceeded = setMediaDataSource(ctx, mediaPlayer, Uri.parse(fileName));
        }
        if (!setDataSourceSucceeded) {
            setDataSourceSucceeded = setMediaDataSource(ctx, mediaPlayer, R.raw.default_alert);
        }
        if (!setDataSourceSucceeded) {
            Log.wtf(TAG, "setMediaDataSource failed - cannot play!");
            return;
        }

        streamType = forceSpeaker ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC;

        try {
            requestAudioFocus();
            mediaPlayer.setAudioStreamType(streamType);
            mediaPlayer.setLooping(false);
            mediaPlayer.setOnPreparedListener(mp -> {
                adjustCurrentVolumeForAlert(streamType, volumeFrac, overrideSilentMode);
                mediaPlayer.start();
            });

            mediaPlayer.prepareAsync();
        } catch (NullPointerException e) {
            Log.wtf(TAG, "Playfile: Concurrency related null pointer exception: " + e.toString());
        } catch (IllegalStateException e) {
            Log.wtf(TAG, "Playfile: Concurrency related illegal state exception: " + e.toString());
        }
    }

    private synchronized void adjustCurrentVolumeForAlert(final int streamType, final float volumeFrac, final boolean overrideSilentMode) {
        final int maxVolume = getMaxVolume(streamType);
        if (maxVolume < 0) {
            UserError.Log.wtf(TAG, "Cannot get max volume to adjust current volume!");
            return;
        }
        volumeBeforeAlert = getVolume(streamType);
        volumeForThisAlert = (int) (maxVolume * volumeFrac);
        Log.d(TAG, "before playing volumeBeforeAlert " + volumeBeforeAlert + " volumeForThisAlert " + volumeForThisAlert);
        // adjust volume if we are allowed and it needs adjusting
        if (volumeForThisAlert != 0
                && (volumeBeforeAlert <= 0 && overrideSilentMode)
                || (volumeBeforeAlert > 0 && volumeBeforeAlert != volumeForThisAlert)) {

            setVolume(streamType, volumeForThisAlert);
        }
    }

    private synchronized void revertCurrentVolume(final int streamType) {
        final int currentVolume = getVolume(streamType);
        Log.d(TAG, "revertCurrentVolume volumeBeforeAlert " + volumeBeforeAlert + " volumeForThisAlert " + volumeForThisAlert
                + " currentVolume " + currentVolume);
        if (volumeForThisAlert == currentVolume && volumeBeforeAlert != -1 && volumeForThisAlert != -1) {
            // If the user has changed the volume, don't change it again.
            setVolume(streamType, volumeBeforeAlert);
        }
        volumeBeforeAlert = -1;
        volumeForThisAlert = -1;
    }

    private int getVolume(final int streamType) {
        try {
            return manager.getStreamVolume(streamType);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Exception getting volume: " + e);
            return -1;
        }
    }

    private int getMaxVolume(final int streamType) {
        try {
            return manager.getStreamMaxVolume(streamType);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Exception getting max volume: " + e);
            return -1;
        }
    }

    private void setVolume(final int streamType, final int volume) {
        if (manager == null) {
            UserError.Log.e(TAG, "AudioManager was null when doing setVolume!");
            return;
        }
        try {
            lastVolumeChange = JoH.tsl();
            manager.setStreamVolume(streamType, volume, 0);
            Log.d(TAG, "Adjusted volume to: " + volume);
        } catch (SecurityException e) {
            if (JoH.ratelimit("sound volume error", 12000)) {
                UserError.Log.wtf(TAG, "This device does not allow us to modify the sound volume");
            }
        }
    }

    private PendingIntent notificationIntent(Context ctx, Intent intent){
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }
    private PendingIntent snoozeIntent(Context ctx, int minsSinceStartPlaying){
        final Intent intent = new Intent(ctx, SnoozeOnNotificationDismissService.class);
        intent.putExtra("alertType", "bg_alerts");
        intent.putExtra("raisedTimeStamp", JoH.tsl());
        intent.putExtra("minsSinceStartPlaying", minsSinceStartPlaying);
        return PendingIntent.getService(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static private int getAlertProfile(Context ctx){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String profile = prefs.getString("bg_alert_profile", "ascending");
        if(profile.equals("High")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_HIGH");
            return ALERT_PROFILE_HIGH;
        }
        if(profile.equals("ascending")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_ASCENDING");
            return ALERT_PROFILE_ASCENDING;
        }
        if(profile.equals("medium")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_MEDIUM");
            return ALERT_PROFILE_MEDIUM;
        }
        if(profile.equals("vibrate only")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_VIBRATE_ONLY");
            return ALERT_PROFILE_VIBRATE_ONLY;
        }
        if(profile.equals("Silent")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_SILENT");
            return ALERT_PROFILE_SILENT;
        }
        Log.wtf(TAG, "getAlertProfile unknown value " + profile + " ALERT_PROFILE_ASCENDING");
        return ALERT_PROFILE_ASCENDING;

    }
    
    public static boolean isAscendingMode(Context ctx){
        Log.d(TAG, "(getAlertProfile(ctx) == ALERT_PROFILE_ASCENDING): " + (getAlertProfile(ctx) == ALERT_PROFILE_ASCENDING));
        return getAlertProfile(ctx) == ALERT_PROFILE_ASCENDING;
    }

    static boolean notSilencedDueToCall() {
        return !(Pref.getBooleanDefaultFalse("no_alarms_during_calls") && (JoH.isOngoingCall()));
    }

    protected void VibrateNotifyMakeNoise(Context context, AlertType alert, String bgValue, int minsFromStartPlaying) {
        Log.d(TAG, "VibrateNotifyMakeNoise called minsFromStartedPlaying = " + minsFromStartPlaying);
        Log.d("ALARM", "setting vibrate alarm");
        int profile = getAlertProfile(context);
        if (alert.uuid.equals(AlertType.LOW_ALERT_55)) {
            // boost alerts...
            if (profile == ALERT_PROFILE_VIBRATE_ONLY) {
                profile = ALERT_PROFILE_ASCENDING;
            }
        }

        // We use timeFromStartPlaying as a way to force vibrating/ non vibrating...
        if (profile != ALERT_PROFILE_ASCENDING) {
            // We start from the non ascending part...
            minsFromStartPlaying = MAX_ASCENDING_MINUTES;
        }
        final String highlow = (alert.above ? context.getString(R.string.high) : context.getString(R.string.low)).toUpperCase();
        String title = bgValue + " " + alert.name;
        String content = "BG " + highlow + " ALERT: " + bgValue + "  (@" + JoH.hourMinuteString() + ")";
        final Intent intent = new Intent(context, SnoozeActivity.class);

        boolean localOnly = (Home.get_forced_wear() && PersistentStore.getBoolean("bg_notifications_watch"));
        Log.d(TAG, "NotificationCompat.Builder localOnly=" + localOnly);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.BG_ALERT_CHANNEL)//KS Notification
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                //.addAction(R.drawable.ic_action_communication_invert_colors_on, "SNOOZE", notificationIntent(context, intent))
                .setContentIntent(notificationIntent(context, intent))
                .setLocalOnly(localOnly)

                .setGroup("xDrip level alert")
                .setPriority(Pref.getBooleanDefaultFalse("high_priority_notifications") ? Notification.PRIORITY_MAX : Notification.PRIORITY_HIGH)
                .setDeleteIntent(snoozeIntent(context, minsFromStartPlaying));
        if (Pref.getBoolean("show_buttons_in_alerts", true)) {
             builder.addAction(
                    R.drawable.alert_icon,
                    context.getString(R.string.snooze_alert),
                    snoozeIntent(context, minsFromStartPlaying)
            );
        }
        boolean overrideSilent = alert.override_silent_mode;
        if (profile != ALERT_PROFILE_VIBRATE_ONLY && profile != ALERT_PROFILE_SILENT) {
            float volumeFrac = (float) (minsFromStartPlaying - MAX_VIBRATING_MINUTES) / (MAX_ASCENDING_MINUTES - MAX_VIBRATING_MINUTES);
            // While minsFromStartPlaying <= MAX_VIBRATING_MINUTES, we only vibrate ...
            if (!Pref.getBoolean("delay_ascending_3min", true)) { // If delay_ascending_3min is disabled, linearly increase volume from start.
                volumeFrac = (minsFromStartPlaying * NODELAY_ASCENDING_SLOPE + NODELAY_ASCENDING_INTERCEPT);
            }
            volumeFrac = Math.max(volumeFrac, 0); // Limit volumeFrac to values greater than and equal to 0
            volumeFrac = Math.min(volumeFrac, 1); // Limit volumeFrac to values less than and equal to 1
            if (profile == ALERT_PROFILE_MEDIUM) {
                volumeFrac = (float) 0.7;
            }
            Log.d(TAG, "VibrateNotifyMakeNoise volumeFrac = " + volumeFrac);

            boolean forceSpeaker = alert.force_speaker;

            if (overrideSilent) {
                UserError.Log.d(TAG, "Setting full screen intent");
                builder.setCategory(NotificationCompat.CATEGORY_ALARM);
                builder.setFullScreenIntent(notificationIntent(context, new Intent(context, Home.class)), true);
            }

            if (notSilencedDueToCall()) {
                if (overrideSilent || isLoudPhone(context)) {
                    playFile(context, alert.mp3_file, volumeFrac, forceSpeaker, overrideSilent);
                }
            } else {
                Log.i(TAG, "Silenced Alert Noise due to ongoing call");
            }
        }
        if (profile != ALERT_PROFILE_SILENT && alert.vibrate) {
            if (notSilencedDueToCall()) {
                builder.setVibrate(Notifications.vibratePattern);
            } else {
                Log.i(TAG, "Vibration silenced due to ongoing call");
            }
        } else {
            // In order to still show on all android wear watches, either a sound or a vibrate pattern
            // seems to be needed. This pattern basically does not vibrate:
            builder.setVibrate(new long[]{1, 0});
        }
        Log.ueh("Alerting", content);
        final NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //mNotifyMgr.cancel(Notifications.exportAlertNotificationId); // this appears to confuse android wear version 2.0.0.141773014.gms even though it shouldn't - can we survive without this?
        mNotifyMgr.notify(Notifications.exportAlertNotificationId, XdripNotificationCompat.build(builder));

        // send to bluejay
        BlueJayEntry.sendAlertIfEnabled((alert.above ? "High" : "Low") + " Alert " + bgValue + " " + alert.name); // string text is used to determine alert type

        // send alert to pebble
        if (Pref.getBooleanDefaultFalse("broadcast_to_pebble") && (Pref.getBooleanDefaultFalse("pebble_vibe_alerts"))) {
            if (JoH.ratelimit("pebble_vibe_start", 59)) {
                JoH.startService(PebbleWatchSync.class);
            }
        }

        //send alert to amazfit
        if (Pref.getBooleanDefaultFalse("pref_amazfit_enable_key")
                && Pref.getBooleanDefaultFalse("pref_amazfit_BG_alert_enable_key")) {
            Amazfitservice.start("xDrip_Alarm", alert.name, alert.default_snooze);
        }

        if (LeFunEntry.areAlertsEnabled() && ActiveBgAlert.currentlyAlerting()) {
            LeFun.sendAlert(highlow, bgValue);
        }

        if (MiBandEntry.areAlertsEnabled() && ActiveBgAlert.currentlyAlerting()) {
            MiBand.sendAlert(alert.name, highlow + " " + bgValue, alert.default_snooze);
        }

        if (ActiveBgAlert.currentlyAlerting()) {
            BroadcastEntry.sendAlert(Const.BG_ALERT_TYPE, highlow + " " + bgValue);
        }

        // speak alert
        if (Pref.getBooleanDefaultFalse("speak_alerts")) {
            SpeechUtil.say(highlow + ", " + bgValue, 3000);
        }

        if (Pref.getBooleanDefaultFalse("flash_torch_alerts_charging")) {
            if (PowerStateReceiver.is_power_connected()) {
                FlashLight.torchPulse();
            }
        }
    }

    private void notificationDismiss(Context ctx) {
        final NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            mNotifyMgr.cancel(Notifications.exportAlertNotificationId);
        } catch (NullPointerException e) {
            UserError.Log.e(TAG,"Got null pointer in notificationDismiss !!");
        }
    }

    // True means play the file false means only vibrate.
    private boolean isLoudPhone(Context ctx) {
        final AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
        try {
            switch (am.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT:
                    return false;
                case AudioManager.RINGER_MODE_VIBRATE:
                    return false;
                case AudioManager.RINGER_MODE_NORMAL:
                    return true;
            }
        } catch (NullPointerException e) {
            UserError.Log.e(TAG,"Got null pointer exception from isLoudPhone");
        }
        // unknown mode, not sure let's play just in any case.
        return true;
    }
}
