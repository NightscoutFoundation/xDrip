package com.eveningoutpost.dexdrip.utils.framework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.music.MusicSpec;
import com.eveningoutpost.dexdrip.UtilityModels.music.MusicState;

public class MusicPlaybackReceiver extends BroadcastReceiver {
    protected String TAG = this.getClass().getSimpleName();
    private static MusicSpec lastMusicSpec = new MusicSpec();
    private static MusicState lastStateSpec = new MusicState();

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        Bundle bundle = intent.getExtras();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            LOG.info(String.format("%s %s (%s)", key,
                    value != null ? value.toString() : "null", value != null ? value.getClass().getName() : "no class"));
        }
        */
        MusicSpec musicSpec = new MusicSpec(lastMusicSpec);
        MusicState stateSpec = new MusicState(lastStateSpec);

        Bundle incomingBundle = intent.getExtras();

        if (incomingBundle == null) {
            UserError.Log.d(TAG, "Not processing incoming null bundle.");
            return;
        }

        for (String key : incomingBundle.keySet()) {
            Object incoming = incomingBundle.get(key);
            if (incoming instanceof String && "artist".equals(key)) {
                musicSpec.artist = (String) incoming;
            } else if (incoming instanceof String && "album".equals(key)) {
                musicSpec.album = (String) incoming;
            } else if (incoming instanceof String && "track".equals(key)) {
                musicSpec.track = (String) incoming;
            } else if (incoming instanceof String && "title".equals(key) && musicSpec.track == null) {
                musicSpec.track = (String) incoming;
            } else if (incoming instanceof Integer && "duration".equals(key)) {
                musicSpec.duration = (Integer) incoming / 1000;
            } else if (incoming instanceof Long && "duration".equals(key)) {
                musicSpec.duration = ((Long) incoming).intValue() / 1000;
            } else if (incoming instanceof Integer && "position".equals(key)) {
                stateSpec.position = (Integer) incoming / 1000;
            } else if (incoming instanceof Long && "position".equals(key)) {
                stateSpec.position = ((Long) incoming).intValue() / 1000;
            } else if (incoming instanceof Boolean && "playing".equals(key)) {
                stateSpec.state = (byte) (((Boolean) incoming) ? MusicState.STATE_PLAYING : MusicState.STATE_PAUSED);
                stateSpec.playRate = (byte) (((Boolean) incoming) ? 100 : 0);
            } else if (incoming instanceof String && "duration".equals(key)) {
                musicSpec.duration = Integer.parseInt((String) incoming) / 1000;
            } else if (incoming instanceof String && "trackno".equals(key)) {
                musicSpec.trackNr = Integer.parseInt((String) incoming);
            } else if (incoming instanceof String && "totaltrack".equals(key)) {
                musicSpec.trackCount = Integer.parseInt((String) incoming);
            } else if (incoming instanceof Integer && "pos".equals(key)) {
                stateSpec.position = (Integer) incoming;
            } else if (incoming instanceof Integer && "repeat".equals(key)) {
                if ((Integer) incoming > 0) {
                    stateSpec.repeat = 1;
                } else {
                    stateSpec.repeat = 0;
                }
            } else if (incoming instanceof Integer && "shuffle".equals(key)) {
                if ((Integer) incoming > 0) {
                    stateSpec.shuffle = 1;
                } else {
                    stateSpec.shuffle = 0;
                }
            }
        }

        if (!lastMusicSpec.equals(musicSpec)) {
            lastMusicSpec = musicSpec;
            UserError.Log.d(TAG, "Update Music Info: " + musicSpec.artist + " / " + musicSpec.album + " / " + musicSpec.track);
            //GBApplication.deviceService().onSetMusicInfo(musicSpec);
        } else {
            UserError.Log.d(TAG, "Got metadata changed intent, but nothing changed, ignoring.");
        }

        if (!lastStateSpec.equals(stateSpec)) {
            lastStateSpec = stateSpec;
            UserError.Log.d(TAG, "Update Music State: state=" + stateSpec.state + ", position= " + stateSpec.position);
            //GBApplication.deviceService().onSetMusicState(stateSpec);
        } else {
            UserError.Log.d(TAG, "Got state changed intent, but not enough has changed, ignoring.");
        }
    }
}
