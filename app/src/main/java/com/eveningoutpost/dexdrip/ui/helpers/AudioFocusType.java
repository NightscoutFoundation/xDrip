package com.eveningoutpost.dexdrip.ui.helpers;

import android.media.AudioManager;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import lombok.val;

/**
 * JamOrHam - convert preference to audio manager type
 */

public class AudioFocusType {

    public static int getAlarmAudioFocusType() {
        val pref = Pref.getString("alert_audio_focus", "AUDIOFOCUS_NONE");

        switch (pref) {
            case "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK":
                return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
            case "AUDIOFOCUS_GAIN_TRANSIENT":
                return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
            case "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE":
                return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
            default:
                return 0;
        }
    }

}
