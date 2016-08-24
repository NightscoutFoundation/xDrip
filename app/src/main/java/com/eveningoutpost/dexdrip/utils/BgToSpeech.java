package com.eveningoutpost.dexdrip.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Created by adrian on 07/09/15.
 */
public class BgToSpeech {

    private static BgToSpeech instance;
    private final Context context;

    private TextToSpeech tts = null;
    private static final String TAG = "BgToSpeech";

    public static BgToSpeech setupTTS(Context context){

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (! prefs.getBoolean("bg_to_speech", false)){
            return null;
        }

        if(instance == null) {
            instance = new BgToSpeech(context);
            return instance;
        } else {
            tearDownTTS();
            instance = new BgToSpeech(context);
            return instance;
        }
    }

    public static void tearDownTTS(){
        if(instance!=null){
            instance.tearDown();
            instance = null;
        } else {
           // Log.e(TAG, "tearDownTTS() called but instance is null!");
        }
    }

    public static void speak(final double value, long timestamp) {
        if (instance == null) {
            try {
                setupTTS(xdrip.getAppContext());
            } catch (Exception e) {
                Log.e(TAG, "Got exception trying to on demand set up instance: " + e);
            }
        }
        if (instance == null) {
            Log.e(TAG, "speak() called but instance is null!");
        } else {
            instance.speakInternal(value, timestamp);
        }
    }

    private void tearDown() {
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (IllegalArgumentException e) {
                Log.e(TAG,"Got exception shutting down service: " + e);
            }
            tts = null;
        }
    }

    private BgToSpeech(Context context) {
        this.context = context;
        this.tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                Log.d(TAG, "Calling onInit(), tts = " + tts);
                if (status == TextToSpeech.SUCCESS && tts != null) {

                    //try local language
                    Locale loc = Locale.getDefault();
                    Log.d(TAG, "status == TextToSpeech.SUCCESS + loc" + loc);
                    int result;
                    try {
                        result = tts.setLanguage(Locale.getDefault());
                    } catch (IllegalArgumentException e) {
                        // can end up here with Locales like "OS"
                        Log.e(TAG, "Got TTS set language error: " + e.toString());
                        result = TextToSpeech.LANG_MISSING_DATA;
                    }
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Default system language is not supported");
                        result = tts.setLanguage(Locale.ENGLISH);
                    }
                    //try any english
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "English is not supported");
                        tts = null;
                    }
                } else {
                    Log.e(TAG, "status != TextToSpeech.SUCCESS; status: " + status);
                    tts = null;
                }
            }
        });
    }

    private void speakInternal(final double value, long timestamp) {

        // SHIELDING
        if (timestamp < System.currentTimeMillis() - 4 * 60 * 1000) {
            // don't read old values.
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("bg_to_speech", false) || isOngoingCall()) {
            return;
        }

        if (tts == null) {
            Log.wtf(TAG, "TTS is null in speakInternal");
            return;
        }
        // ACTUAL TTS:
        try {
            final int result = tts.speak(calculateText(value, prefs), TextToSpeech.QUEUE_FLUSH, null);
            if (result == TextToSpeech.SUCCESS) {
                Log.d(TAG, "successfully spoken");
            } else {
                Log.d(TAG, "error " + result + ". trying again with new tts-object.");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in TTS: " + e.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException in TTS: " + e.toString());
        }
    }

    private String calculateText(double value, SharedPreferences prefs) {
        boolean doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));

        String text = "";

        DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            text = "high";
        } else if (value >= 40) {
            if (doMgdl) {
                df.setMaximumFractionDigits(0);
                text = df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                df.setMinimumFractionDigits(1);
                text = df.format(value * Constants.MGDL_TO_MMOLL);
                try {
                    if (tts.getLanguage().getLanguage().startsWith("en")) {
                        // in case the text has a comma in current locale but TTS defaults to English
                        text = text.replace(",", ".");
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Null pointer for TTS in calculateText");
                }
            }
        } else if (value > 12) {
            text = "low";
        } else {
            text = "error";
        }
        Log.d(TAG, "text: " + text);
        return text;
    }


    public static void installTTSData(Context ctx) {
        try {
            Intent intent = new Intent();
            intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Could not install TTS data: " + e.toString());
        }
    }

    private boolean isOngoingCall(){
        AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        return (manager.getMode()==AudioManager.MODE_IN_CALL);
    }


}
