package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Created by adrian on 07/09/15.
 */
public class BgToSpeech {

    private static BgToSpeech instance;
    private final Context context;

    private TextToSpeech tts = null;

    private long timestamp = 0;

    public static BgToSpeech getSingleton(Context context){

        if(instance == null ||  instance.context != context) {
            instance = new BgToSpeech(context);
        }
        return instance;
    }

    private BgToSpeech(Context context){
        this.context = context;
    }

    public void speak(final double value, long timestamp){


        if(timestamp < System.currentTimeMillis()-4*60*1000){
            // don't read old values.
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (! prefs.getBoolean("bg_to_speech", false)){
            return;
        }

        if(this.timestamp == timestamp){
            return;
        }

        this.timestamp = timestamp;

        if(tts == null){
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    Log.d("BgToSpeech", "Calling onInit()");
                    if (status == TextToSpeech.SUCCESS) {
                        Log.d("BgToSpeech", "status == TextToSpeech.SUCCESS");
                        //try local language
                        int result = tts.setLanguage(Locale.getDefault());
                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("BgToSpeech", "Default system language is not supported");
                            result = tts.setLanguage(Locale.ENGLISH);
                        }
                        //try any english
                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("BgToSpeech", "English is not supported");
                            tts = null;
                        } else {
                            //first call will be made after initialization
                            int speakresult = tts.speak(calculateText(value, prefs), TextToSpeech.QUEUE_FLUSH, null);
                            if(speakresult == TextToSpeech.SUCCESS){
                                Log.d("BgToSpeech", "successfully spoken after initialization");
                            } else {
                                Log.d("BgToSpeech", "error " + result + ". not trying again.");
                                tts = null;
                            }
                        }

                    } else {
                        Log.d("BgToSpeech", "status != TextToSpeech.SUCCESS; status: " + status);
                        tts= null;
                    }
                }
            });
        } else {

        if (tts == null) {
            return;
        }
        int result = tts.speak(calculateText(value, prefs), TextToSpeech.QUEUE_FLUSH, null);
            if(result == TextToSpeech.SUCCESS){
                Log.d("BgToSpeech", "successfully spoken");
            } else {
                Log.d("BgToSpeech", "error " + result + ". trying again with new tts-object.");
                tts = null;
                this.timestamp = -1;
                speak(value, timestamp);
            }

    }
    }

    private String calculateText(double value, SharedPreferences prefs) {
        boolean doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));

        String text = "";

        DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            text = "high";
        } else if (value >= 40) {
            if(doMgdl) {
                df.setMaximumFractionDigits(0);
                text =  df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                df.setMinimumFractionDigits(1);
                text =  df.format(value* Constants.MGDL_TO_MMOLL);
            }
        } else if (value > 12) {
            text =  "low";
        } else {
            text = "no value";
        }
        Log.d("BgToSpeech", "text: " + text);
        return text;
    }


    public static void installTTSData(Context ctx){
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

}
