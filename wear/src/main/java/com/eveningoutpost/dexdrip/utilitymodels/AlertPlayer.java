package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ListenerService;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.SnoozeOnNotificationDismissService;
import com.eveningoutpost.dexdrip.SnoozeActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.eveningoutpost.dexdrip.ListenerService.SendData;
import static com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer.getAlertPlayerStreamType;

//KS import com.eveningoutpost.dexdrip.EditAlertActivity;
//KS import com.eveningoutpost.dexdrip.GcmActivity;
//KS import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;

// A helper class to create the mediaplayer on the UI thread.
// This is needed in order for the callbackst to work.
class MediaPlayerCreaterHelper {
    
    private final static String TAG = AlertPlayer.class.getSimpleName();

    final Object lock1_ = new Object();
    boolean mplayerCreated_ = false;
    volatile MediaPlayer mediaPlayer_ = null;
    
    MediaPlayer createMediaPlayer(Context ctx) {
        if (isUiThread()) {
            return new MediaPlayer();
        }
        
        mplayerCreated_ = false;
        mediaPlayer_ = null;
        Handler mainHandler = new Handler(ctx.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override 
            public void run() {
                synchronized(lock1_) {
                    try {
                        mediaPlayer_ = new MediaPlayer();
                        Log.i(TAG, "media player created");
                    } finally {
                        mplayerCreated_ = true;
                        lock1_.notifyAll();
                    }
                    
                }
            }
        };
        mainHandler.post(myRunnable);
        
        try {
            synchronized(lock1_) {
                while(mplayerCreated_ == false) {
                   
                        lock1_.wait();
                }
            } 
        }catch (InterruptedException e){
             Log.e(TAG, "Cought exception", e);
        }

        try {
            mediaPlayer_.setAudioStreamType(getAlertPlayerStreamType());
        } catch (Exception e) {
            UserError.Log.e(TAG, "Set mediaplayer stream type: " + e);
        }
        return mediaPlayer_;
    }
    
    boolean isUiThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}


public class AlertPlayer {

    private static volatile AlertPlayer singletone;

    private final static String TAG = AlertPlayer.class.getSimpleName();
    private volatile MediaPlayer mediaPlayer = null;
    private int volumeBeforeAlert = -1;
    private int volumeForThisAlert = -1;

    final static int ALERT_PROFILE_HIGH = 1;
    final static int ALERT_PROFILE_ASCENDING = 2;
    final static int ALERT_PROFILE_MEDIUM = 3;
    final static int ALERT_PROFILE_VIBRATE_ONLY = 4;
    final static int ALERT_PROFILE_SILENT = 5;

    final static int  MAX_VIBRATING = 2;
    final static int  MAX_ASCENDING = 5;

    private static synchronized void createPlayer() {
        if (singletone == null) {
            singletone = new AlertPlayer();
        }
    }

    public static AlertPlayer getPlayer() {
        if (singletone == null) {
            Log.i(TAG, "getPlayer: Creating a new AlertPlayer");
            createPlayer();
        } else {
            Log.i(TAG, "getPlayer: Using existing AlertPlayer");
        }
        return singletone;
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
        if (!start_snoozed) Vibrate(ctx, newAlert, bgValue, newAlert.override_silent_mode, 0);
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
            if (mediaPlayer.isPlaying()) {
                try {
                    mediaPlayer.stop();
                } catch (IllegalStateException e) {
                    UserError.Log.e(TAG, "Exception when stopping media player: " + e);
                }
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        revertCurrentVolume(ctx);
    }

    //  default signature for user initiated interactive snoozes only
    public synchronized void Snooze(Context ctx, int repeatTime) {
        Snooze(ctx, repeatTime, true);
        //   if (Home.get_forced_wear() && Pref.getBooleanDefaultFalse("bg_notifications") ) {
        SendData(ctx, ListenerService.WEARABLE_SNOOZE_ALERT, ("" + repeatTime).getBytes(StandardCharsets.UTF_8));
        //   }
    }

    public synchronized void Snooze(Context ctx, int repeatTime, boolean from_interactive) {
        Log.i(TAG, "Snooze called repeatTime = " + repeatTime);
        stopAlert(ctx, false, false);
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert == null) {
            Log.e(TAG, "Error, snooze was called but no alert is active.");
            //KS TODO if (from_interactive) GcmActivity.sendSnoozeToRemote();
            return;
        }
        if (repeatTime == -1) {
            // try to work out default
            AlertType alert = ActiveBgAlert.alertTypegetOnly();
            if (alert != null) {
                repeatTime = alert.default_snooze;
                Log.d(TAG, "Selecting default snooze time: " + repeatTime);
            } else {
                repeatTime = 30; // pick a number if we cannot even find the default
                Log.e(TAG, "Cannot even find default snooze time so going with: " + repeatTime);
            }
        }
        activeBgAlert.snooze(repeatTime);
        //KS if (from_interactive) GcmActivity.sendSnoozeToRemote();
    }

    public synchronized  void PreSnooze(Context ctx, String uuid, int repeatTime) {
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

    public static int getAlertPlayerStreamType() {
        return AudioManager.STREAM_ALARM;
    }


    // Check the state and alrarm if needed
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

            int timeFromStartPlaying = activeBgAlert.getUpdatePlayTime();
            AlertType alert = AlertType.get_alert(activeBgAlert.alert_uuid);
            if (alert == null) {
                Log.d(TAG, "ClockTick: The alert was already deleted... will not play");
                ActiveBgAlert.ClearData();
                return;
            }
            Log.d(TAG,"ClockTick: Playing the alert again");
            long nextAlertTime = alert.getNextAlertTime(ctx);
            activeBgAlert.updateNextAlertAt(nextAlertTime);
            
            Vibrate(ctx, alert, bgValue, alert.override_silent_mode, timeFromStartPlaying);
        }

    }

    
    private boolean setDataSource(Context context, MediaPlayer mp, Uri uri) {
        try {
            mp.setDataSource(context, uri);
            return true;
        } catch (IOException ex) {
            Log.e(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.e(TAG, "create failed:", ex);
            // fall through
        }
        return false;
    }
    
    private boolean setDataSource(Context context, MediaPlayer mp, int resid) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) return false;

            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            return true;
        } catch (IOException ex) {
            Log.e(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.e(TAG, "create failed:", ex);
            // fall through
        }
        return false;
    }

    protected synchronized void PlayFile(final Context ctx, String FileName, float VolumeFrac) {
        Log.i(TAG, "PlayFile: called FileName = " + FileName);

        if(mediaPlayer != null) {
            Log.i(TAG, "ERROR, PlayFile:going to leak a mediaplayer !!!");
            mediaPlayer.release();
            mediaPlayer = null;
        }
        
        mediaPlayer = new MediaPlayerCreaterHelper().createMediaPlayer(ctx);
        if(mediaPlayer == null) {
            Log.e(TAG, "MediaPlayerCreaterHelper().createMediaPlayer failed");
            return;
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.i(TAG, "playFile: onCompletion called (finished playing) ");
            mediaPlayer.release();
            mediaPlayer = null;
            revertCurrentVolume(ctx);
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "playFile: onError called (what: " + what + ", extra: " + extra);
            // possibly media player error; release is handled in onCompletionListener
            return false;
        });

        boolean setDataSourceSucceeded = false;
        if(FileName != null && FileName.length() > 0) {
            setDataSourceSucceeded = setDataSource(ctx, mediaPlayer, Uri.parse(FileName));
        }
        if (setDataSourceSucceeded == false) {
            //KS TODO setDataSourceSucceeded = setDataSource(ctx, mediaPlayer, R.raw.default_alert);
        }
        if(setDataSourceSucceeded == false) {
            Log.e(TAG, "setDataSource failed");
            return;
        }
            
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Cought exception preparing meidaPlayer", e);
            return;
        }

        if (mediaPlayer != null) {
            AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volumeBeforeAlert = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeForThisAlert = (int) (maxVolume * VolumeFrac);

            Log.i(TAG, "before playing volumeBeforeAlert " + volumeBeforeAlert + " volumeForThisAlert " + volumeForThisAlert);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeForThisAlert, 0);
            try {
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.i(TAG, "PlayFile: onCompletion called (finished playing) ");
                        revertCurrentVolume(ctx);
                    }
                });
                Log.i(TAG, "PlayFile: calling mediaPlayer.start() ");
                mediaPlayer.start();
            } catch (NullPointerException e) {
                Log.wtf(TAG, "Playfile: Concurrency related null pointer exception: " + e.toString());
            } catch (IllegalStateException e) {
                Log.wtf(TAG, "Playfile: Concurrency related illegal state exception: " + e.toString());
            }

        } else {
            // TODO, what should we do here???
            Log.wtf(TAG, "PlayFile: Starting an alert failed, what should we do !!!");
        }
    }
    
    private void revertCurrentVolume(final Context ctx) {
        AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.i(TAG, "revertCurrentVolume volumeBeforeAlert " + volumeBeforeAlert + " volumeForThisAlert " + volumeForThisAlert
                + " currentVolume " + currentVolume);
        if (volumeForThisAlert == currentVolume && (volumeBeforeAlert != -1) && (volumeForThisAlert != -1)) {
            // If the user has changed the volume, don't change it again.
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeAlert, 0);
        }
        volumeBeforeAlert = -1;
        volumeForThisAlert = - 1;
        
    }

    private PendingIntent notificationIntent(Context ctx, Intent intent){
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }
    private PendingIntent snoozeIntent(Context ctx){
        Intent intent = new Intent(ctx, SnoozeOnNotificationDismissService.class);
        intent.putExtra("alertType", "bg_alerts");
        return PendingIntent.getService(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static private int getAlertProfile(Context ctx){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String profile = prefs.getString("bg_alert_profile", "vibrate only");//KS ascending
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

    public static boolean notSilencedDueToCall()
    {
        return !(Pref.getBooleanDefaultFalse("no_alarms_during_calls") && (JoH.isOngoingCall()));
    }

    private void Vibrate(Context ctx, AlertType alert, String bgValue, Boolean overrideSilent, int timeFromStartPlaying) {
        //KS Watch currently only supports Vibration, no audio; Use VibrateAudio to support audio
        String title = bgValue + " " + alert.name;
        String content = "BG LEVEL ALERT: " + bgValue + "  (@" + JoH.hourMinuteString() + ")";
        Intent intent = new Intent(ctx, SnoozeActivity.class);

        boolean localOnly = (Home.get_forced_wear() && Pref.getBooleanDefaultFalse("bg_notifications"));//KS
        Log.d(TAG, "NotificationCompat.Builder localOnly=" + localOnly);
        NotificationCompat.Builder  builder = new NotificationCompat.Builder(ctx)//KS Notification
                .setSmallIcon(R.drawable.ic_launcher)//KS ic_action_communication_invert_colors_on
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(notificationIntent(ctx, intent))
                .setLocalOnly(localOnly)//KS
                .setDeleteIntent(snoozeIntent(ctx));
        builder.setVibrate(Notifications.vibratePattern);
        Log.ueh("Alerting",content);
        NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        //mNotifyMgr.cancel(Notifications.exportAlertNotificationId); // this appears to confuse android wear version 2.0.0.141773014.gms even though it shouldn't - can we survive without this?
        mNotifyMgr.notify(Notifications.exportAlertNotificationId, builder.build());
        if (Pref.getBooleanDefaultFalse("alert_use_sounds")) {
            try {
                if (JoH.ratelimit("wear-alert-sound", 10)) {
                    JoH.playResourceAudio(R.raw.warning);
                }
            } catch (Exception e) {
                //
            }
        }
    }

    private void VibrateAudio(Context ctx, AlertType alert, String bgValue, Boolean overrideSilent, int timeFromStartPlaying) {
        Log.d(TAG, "Vibrate called timeFromStartPlaying = " + timeFromStartPlaying);
        Log.d("ALARM", "setting vibrate alarm");
        int profile = getAlertProfile(ctx);
        if(alert.uuid.equals(AlertType.LOW_ALERT_55)) {
            // boost alerts...
            if(profile == ALERT_PROFILE_VIBRATE_ONLY) {
                profile = ALERT_PROFILE_ASCENDING;
            }
        }

        // We use timeFromStartPlaying as a way to force vibrating/ non vibrating...
        if (profile != ALERT_PROFILE_ASCENDING) {
            // We start from the non ascending part...
            timeFromStartPlaying = MAX_ASCENDING;
        }

        String title = bgValue + " " + alert.name;
        String content = "BG LEVEL ALERT: " + bgValue + "  (@" + JoH.hourMinuteString() + ")";
        Intent intent = new Intent(ctx, SnoozeActivity.class);

        boolean localOnly = (Home.get_forced_wear() && Pref.getBooleanDefaultFalse("bg_notifications"));//KS
        Log.d(TAG, "NotificationCompat.Builder localOnly=" + localOnly);
        NotificationCompat.Builder  builder = new NotificationCompat.Builder(ctx)//KS Notification
            .setSmallIcon(R.drawable.ic_launcher)//KS ic_action_communication_invert_colors_on
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(notificationIntent(ctx, intent))
            .setLocalOnly(localOnly)//KS
            .setDeleteIntent(snoozeIntent(ctx));

        if (profile != ALERT_PROFILE_VIBRATE_ONLY && profile != ALERT_PROFILE_SILENT) {
            if (timeFromStartPlaying >= MAX_VIBRATING) {
                // Before this, we only vibrate...
                float volumeFrac = (float)(timeFromStartPlaying - MAX_VIBRATING) / (MAX_ASCENDING - MAX_VIBRATING);
                volumeFrac = Math.min(volumeFrac, 1);
                if(profile == ALERT_PROFILE_MEDIUM) {
                    volumeFrac = (float)0.7;
                }
                Log.d(TAG, "Vibrate volumeFrac = " + volumeFrac);
                boolean isRingTone = true;//KS TODO EditAlertActivity.isPathRingtone(ctx, alert.mp3_file);

                if (notSilencedDueToCall()) {
                    if (isRingTone && !overrideSilent) {
                        builder.setSound(Uri.parse(alert.mp3_file));
                    } else {
                        if (overrideSilent || isLoudPhone(ctx)) {
                            PlayFile(ctx, alert.mp3_file, volumeFrac);
                        }
                    }
                } else {
                    Log.i(TAG,"Silenced Alert Noise due to ongoing call");
                }
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
            //KS ADD:
            // This code snippet will cause the phone to vibrate "SOS" in Morse Code
            // In Morse Code, "s" = "dot-dot-dot", "o" = "dash-dash-dash"
            // There are pauses to separate dots/dashes, letters, and words
            // The following numbers represent millisecond lengths
            int dot = 200;      // Length of a Morse Code "dot" in milliseconds
            int dash = 500;     // Length of a Morse Code "dash" in milliseconds
            int short_gap = 200;    // Length of Gap Between dots/dashes
            int medium_gap = 500;   // Length of Gap Between Letters
            int long_gap = 1000;    // Length of Gap Between Words
            long[] pattern = {
                    0,  // Start immediately
                    dot, short_gap, dot, short_gap, dot,    // s
                    medium_gap,
                    dash, short_gap, dash, short_gap, dash, // o
                    medium_gap,
                    dot, short_gap, dot, short_gap, dot,    // s
                    long_gap
            };
            // Only perform this pattern one time (-1 means "do not repeat")
            //mVibrator.vibrate(pattern, -1);
            builder.setVibrate(pattern);
            //builder.setVibrate(new long[]{1, 0});
        }
        Log.ueh("Alerting",content);
        NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        //mNotifyMgr.cancel(Notifications.exportAlertNotificationId); // this appears to confuse android wear version 2.0.0.141773014.gms even though it shouldn't - can we survive without this?
        mNotifyMgr.notify(Notifications.exportAlertNotificationId, builder.build());

        /* //KS not used on watch
        if (Pref.getBooleanDefaultFalse("broadcast_to_pebble") && (Pref.getBooleanDefaultFalse("pebble_vibe_alerts"))) {
            if (JoH.ratelimit("pebble_vibe_start", 59)) {
                ctx.startService(new Intent(ctx, PebbleWatchSync.class));
            }
        }*/
    }

    private void notificationDismiss(Context ctx) {
        NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Notifications.exportAlertNotificationId);
    }

    // True means play the file false means only vibrate.
    private boolean isLoudPhone(Context ctx) {
        AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                return false;
            case AudioManager.RINGER_MODE_VIBRATE:
                return false;
            case AudioManager.RINGER_MODE_NORMAL:
                return true;
        }
        // unknown mode, not sure let's play just in any case.
        return true;
    }
}
