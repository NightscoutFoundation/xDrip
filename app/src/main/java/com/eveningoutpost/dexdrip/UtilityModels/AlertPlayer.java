package com.eveningoutpost.dexdrip.UtilityModels;

import java.util.Date;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;

public class AlertPlayer {

    static AlertPlayer singletone;
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    private MediaPlayer mediaPlayer;
    int volumeBeforeAlert;
    Context context;
    
    
    public static AlertPlayer getPlayer() {
        if(singletone == null) {
            Log.e(TAG,"Creating a new AlertPlayer");
            singletone = new AlertPlayer();
        } else {
            Log.e("tag","Using existing AlertPlayer");
        }
        return singletone;
    }

    public synchronized  void startAlert(Context ctx, AlertType newAlert )  {
      Log.e(TAG, "start called, Threadid " + Thread.currentThread().getId());
      stopAlert(true);
      ActiveBgAlert.Create(newAlert.uuid, false, new Date().getTime() + newAlert.minutes_between * 60000 );
      
      PlayFile(ctx, newAlert.mp3_file);

    }

    public synchronized void stopAlert(boolean ClearData) {
        Log.e(TAG, "stop called ThreadID " + Thread.currentThread().getId());
        if (ClearData) {
            ActiveBgAlert.ClearData();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    public synchronized  void Snooze(Context ctx, int repeatTime) {
        Log.e(TAG, "Snooze called repeatTime = "+ repeatTime);
        stopAlert(false);
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            Log.e(TAG, "Error, snooze was called but no alert is active. how can that be ??? !!! ");
        }
        activeBgAlert.snooze(repeatTime);
    }
    
 // Check the state and alrarm if needed
    public void ClockTick(Context ctx)
    {
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            // Nothing to do ...
            return;
        }
        if(activeBgAlert.ready_to_alarm()) {
            stopAlert(false);
            AlertType alert = AlertType.get_alert(activeBgAlert.alert_uuid);
            if (alert == null) {
                Log.w(TAG, "The alert was already deleted... will not play");
                ActiveBgAlert.ClearData();
                return;
            }
            Log.e(TAG,"Playing the alert again"); 
            PlayFile(ctx, alert.mp3_file);
        }
        
    }

    private void PlayFile(Context ctx, String FileName) {
        if(mediaPlayer != null) {
            Log.e(TAG, "ERROR, going to leak a mediaplayer !!!");
        }
        if(FileName != null) {
            mediaPlayer = MediaPlayer.create(ctx, Uri.parse(FileName), null);
        }
        if(mediaPlayer == null) {
            Log.w(TAG, "Creating mediaplayer with file " + FileName + " failed. using default alarm");
            mediaPlayer = MediaPlayer.create(ctx, R.raw.default_alert);
        }
        if(mediaPlayer != null) {
            
            AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volumeBeforeAlert = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
            context = ctx;

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    int currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    if(maxVolume == currentVolume) {
                        // If the user has changed the volume, don't change it again.
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeAlert, 0);
                    }
                }
            });
            
            mediaPlayer.start();
        } else {
            // TODO, what should we do here???
            Log.wtf(TAG,"Starting an alert failed, what should we do !!!");
        }
    }

}
