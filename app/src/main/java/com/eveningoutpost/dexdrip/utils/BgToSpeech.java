package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.SpeechUtil;
import com.eveningoutpost.dexdrip.xdrip;

import java.text.DecimalFormat;

import static com.eveningoutpost.dexdrip.UtilityModels.SpeechUtil.TWICE_DELIMITER;

/**
 * Created by adrian on 07/09/15.
 * <p>
 * Updated 27/12/17 by jamorham to use SpeechUtil
 * <p>
 * Designed to speak glucose readings when enabled, call the "speak" method with the value, timestamp and optional trend name
 * <p>
 */
public class BgToSpeech {

    public static final String BG_TO_SPEECH_PREF = "bg_to_speech";

    private static final String TAG = "BgToSpeech";

    // no longer used compatibility signature
    /*
    public static void speak(final double value, long timestamp) {
        speak(value, timestamp, null);
    }
    */

    // speak a bg reading if its timestamp is current, include the delta name if preferences dictate
    public static void speak(final double value, long timestamp, String delta_name) {

        // don't read out old values.
        if (JoH.msSince(timestamp) > 4 * Constants.MINUTE_IN_MS) {
            return;
        }

        // check if speech is enabled and extra check for ongoing call
        if (!Pref.getBooleanDefaultFalse(BG_TO_SPEECH_PREF) || JoH.isOngoingCall()) {
            return;
        }

        realSpeakNow(value, timestamp, delta_name);
    }

    // always speak the value passed
    public static void realSpeakNow(final double value, long timestamp, String delta_name) {
        final String text_to_speak = calculateText(value, Pref.getBooleanDefaultFalse("bg_to_speech_trend") ? delta_name : null);
        UserError.Log.d(TAG, "Attempting to speak BG reading of: " + text_to_speak);

        SpeechUtil.say(text_to_speak);
    }

    private static String mungeDeltaName(String delta_name) {


        switch (delta_name) {
            case "DoubleDown":
                delta_name = xdrip.getAppContext().getString(R.string.DoubleDown);
                break;
            case "SingleDown":
                delta_name = xdrip.getAppContext().getString(R.string.SingleDown);
                break;
            case "FortyFiveDown":
                delta_name = xdrip.getAppContext().getString(R.string.FortyFiveDown);
                break;
            case "Flat":
                delta_name = xdrip.getAppContext().getString(R.string.Flat);
                break;
            case "FortyFiveUp":
                delta_name = xdrip.getAppContext().getString(R.string.FortyFiveUp);
                break;
            case "SingleUp":
                delta_name = xdrip.getAppContext().getString(R.string.SingleUp);
                break;
            case "DoubleUp":
                delta_name = xdrip.getAppContext().getString(R.string.DoubleUp);
                break;
            case "NOT COMPUTABLE":
                delta_name = "";
                break;

            // do we need a default or just pass thru?
        }
        return delta_name;
    }

    private static String calculateText(double value, String delta_name) {

        final boolean doMgdl = (Pref.getString("units", "mgdl").equals("mgdl"));
        final boolean bg_to_speech_repeat_twice = (Pref.getBooleanDefaultFalse("bg_to_speech_repeat_twice"));
        String text = "";

        // TODO does some of this need unifying from best glucose etc?
        final DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            text = xdrip.getAppContext().getString(R.string.high);
        } else if (value >= 40) {
            if (doMgdl) {
                df.setMaximumFractionDigits(0);
                text = df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                df.setMinimumFractionDigits(1);
                text = df.format(value * Constants.MGDL_TO_MMOLL);
                try {
                    // we check the locale but it may not actually be available if the instance isn't created yet
                    if (SpeechUtil.getLocale().getLanguage().startsWith("en")) {
                        // in case the text has a comma in current locale but TTS defaults to English
                        text = text.replace(",", ".");
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Null pointer for TTS in calculateText");
                }
            }
            if (delta_name != null) text += " " + mungeDeltaName(delta_name);
            if (bg_to_speech_repeat_twice) text = text + TWICE_DELIMITER + text;
        } else if (value > 12) {
            text = xdrip.getAppContext().getString(R.string.low);
        } else {
            text = xdrip.getAppContext().getString(R.string.error);
        }
        Log.d(TAG, "calculated text: " + text);
        return text;
    }

    // shutdown instance - used for changing language/settings
    public static void tearDownTTS() {
        SpeechUtil.shutdown();
    }


    // TODO grace period and 20 minute safety when testSpeech is called needs either a rework or rethink as it is ignored by SpeakNow now and the grace parameter is no longer needed
    // hopefully say a test reading
    public static void testSpeech() {
        speakNow(1200000);
    }

    // speak the most recent reading, with 0 grace only if in time
    public static void speakNow(long grace) {
        final BgReading bgReading = BgReading.last();
        if (bgReading != null) {
            final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
            if (dg != null) {
                BgToSpeech.realSpeakNow(dg.mgdl, dg.timestamp + grace, dg.delta_name);
            } else {
                BgToSpeech.realSpeakNow(bgReading.calculated_value, bgReading.timestamp + grace, bgReading.slopeName());
            }
        }
    }

}
