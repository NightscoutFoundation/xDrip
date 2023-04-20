package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.SpeechUtil;
import com.eveningoutpost.dexdrip.utilitymodels.VehicleMode;
import com.eveningoutpost.dexdrip.xdrip;

import java.text.DecimalFormat;

import static com.eveningoutpost.dexdrip.utilitymodels.SpeechUtil.TWICE_DELIMITER;

/**
 * Created by adrian on 07/09/15.
 * <p>
 * Updated 27/12/17 by jamorham to use SpeechUtil
 * <p>
 * Designed to speak glucose readings when enabled, call the "speak" method with the value, timestamp and optional trend name
 * <p>
 */
public class BgToSpeech implements NamedSliderProcessor {

    public static final String BG_TO_SPEECH_PREF = "bg_to_speech";
    private static final double MAX_THRESHOLD_MINUTES = (8 * 60) + 1;
    private static final double MAX_THRESHOLD_MGDL = 100;

    private static final String TAG = "BgToSpeech";

    private static int getMinutesSliderValue(int position) {
        return (int) LogSlider.calc(0, 300, 4, MAX_THRESHOLD_MINUTES, position);
    }

    private static int getThresholdSliderValue(int position) {
        return (int) LogSlider.calc(0, 300, 4, MAX_THRESHOLD_MGDL, position);
    }

    // speak a bg reading if its timestamp is current, include the delta name if preferences dictate
    public static void speak(final double value, long timestamp, String delta_name) {

        // don't read out old values.
        if (JoH.msSince(timestamp) > 4 * Constants.MINUTE_IN_MS) {
            return;
        }

        // TODO As we check for this in new data observer should we only check for ongoing call here?
        // check if speech is enabled and extra check for ongoing call
        if (!(Pref.getBooleanDefaultFalse(BG_TO_SPEECH_PREF) || VehicleMode.shouldSpeak()) || JoH.isOngoingCall()) {
            return;
        }

        // check constraints
        final long change_time = getMinutesSliderValue(Pref.getInt("speak_readings_change_time", 0)) * Constants.MINUTE_IN_MS;

        boolean conditions_met = false;

        if (lastSpokenSince() < change_time) {
            UserError.Log.d(TAG, "Not speaking due to change time threshold: " + JoH.niceTimeScalar(change_time) + " vs " + JoH.niceTimeScalar(lastSpokenSince()));

        } else {
            UserError.Log.d(TAG, "Speaking due to change time threshold: " + JoH.niceTimeScalar(change_time) + " vs " + JoH.niceTimeScalar(lastSpokenSince()));
            conditions_met = true;
        }

        if (!conditions_met) {
            if (!thresholdExceeded(value)) {
                UserError.Log.d(TAG, "Not speaking due to change delta threshold: " + value);
                return;
            }
        }


        updateLastSpokenSince();
        realSpeakNow(value, timestamp, delta_name);

    }

    private static final String LAST_SPOKEN_TIME = "last-spoken-reading-time";

    private static long lastSpokenSince() {
        return JoH.msSince(PersistentStore.getLong(LAST_SPOKEN_TIME));
    }

    private static void updateLastSpokenSince() {
        PersistentStore.setLong(LAST_SPOKEN_TIME, JoH.tsl());
    }

    private static final String LAST_SPOKEN_VALUE = "last-spoken-value";

    private static boolean thresholdExceeded(double value) {
        final long change_delta = getThresholdSliderValue(Pref.getInt("speak_readings_change_threshold", 0));
        final double abs_delta = Math.abs(value - PersistentStore.getDouble(LAST_SPOKEN_VALUE));
        if (abs_delta > change_delta) {
            UserError.Log.uel(TAG, "Threshold EXCEEDED: Current change delta: " + abs_delta + " vs " + change_delta + " @ " + value);
            PersistentStore.setDouble(LAST_SPOKEN_VALUE, value);
            return true;
        }
        UserError.Log.d(TAG, "Threshold not exceeded: Current change delta: " + abs_delta + " vs " + change_delta + " @ " + value);
        return false;
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

    @Override
    public int interpolate(String name, int position) {
        switch (name) {
            case "time":
                return getMinutesSliderValue(position);
            case "threshold":
                return getThresholdSliderValue(position);
        }
        throw new RuntimeException("name not matched in interpolate");
    }
}
