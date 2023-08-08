package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Locale;

/**
 * Created by jamorham on 20/12/2017.
 * <p>
 * Designed to be an easy use-anywhere speech output system, simply call the static "say" method
 * <p>
 * Can be called from any thread, self initializes and statically retains instance and mitigates the binding delays present with Android TTS
 * <p>
 * Uses xDrip preferences to define speech parameters, language etc and will baulk during calls and delay for music.
 * <p>
 * <p>
 * TODO: is there a way we can delay till the end of *any* sound playing - eg notification noise? Does Android expose this in the framework?
 */

public class SpeechUtil {

    public static final String TAG = "SpeechUtil";
    public static final String TWICE_DELIMITER = " ... "; // creates a pause hopefully works on all locales
    private static volatile TextToSpeech tts = null; // maintained instance

    // delay parameter allows you to force a millis delay before playing to avoid clash with notification sounds triggered at the same time
    @SuppressWarnings("WeakerAccess")
    public static void say(final String text) {
        say(text, 0);
    }

    @SuppressWarnings("WeakerAccess")
    public static void say(final String text, long delay) {
        say(text, delay, 0);
    }

    @SuppressWarnings("WeakerAccess")
    public static void say(final String text, long delay, int retry) {

        new Thread(() -> {
            final PowerManager.WakeLock wl = JoH.getWakeLock("SpeechUtil", (int) Constants.SECOND_IN_MS * 60);
            try {
                if (tts == null) {
                    initialize();
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ee) {
                    //
                }

                if (isOngoingCall()) {
                    UserError.Log.e(TAG, "Cannot speak due to ongoing call: " + text);
                    return;
                }

                // if sound is playing, wait up to 40 seconds to deliver the speech
                final long max_wait = JoH.tsl() + Constants.SECOND_IN_MS * 40;
                while (isMusicPlaying() && JoH.tsl() < max_wait) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ee) {
                        //
                    }
                }

                // pull in preferences for speech but set some lower bounds to avoid bad sounding speech
                final float speed = Math.max(0.2f, Pref.getInt("speech_speed", 10) / 10f);
                final float pitch = Math.max(0.4f, Pref.getInt("speech_pitch", 10) / 10f);

                // set the rates
                try {
                    tts.setSpeechRate(speed);
                    tts.setPitch(pitch);
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Deep TTS problem setting speech rates: " + e);
                }
                // handle repeat everything twice feature. We could queue it twice instead of expanding the string but then each block of speech is not a single transaction
                final boolean double_up_text_flag = (!text.contains(TWICE_DELIMITER)) && Pref.getBooleanDefaultFalse("speak_twice");
                final String final_text_to_speak = double_up_text_flag ? (text + TWICE_DELIMITER + text) : text;

                int result;
                try {
                    result = tts.speak(final_text_to_speak, TextToSpeech.QUEUE_ADD, null);
                } catch (NullPointerException e) {
                    result = TextToSpeech.ERROR;
                    UserError.Log.e(TAG, "Got null pointer trying to speak! concurrency issue");
                }
                UserError.Log.d(TAG, "Speak result: " + result);

                // speech randomly fails, usually due to the service not being bound so quick after being initialized, so we wait and retry recursively
                if ((result != TextToSpeech.SUCCESS) && (retry < 5)) {
                    UserError.Log.d(TAG, "Failed to speak: retrying in 2s: " + retry);
                    say(text, delay + 2000, retry + 1);
                    return;
                }
                // only get here if retries exceeded
                if (result != TextToSpeech.SUCCESS) {
                    UserError.Log.wtf(TAG, "Failed to speak after: " + retry + " retries!!!");

                } else {
                    UserError.Log.d(TAG, "Successfully spoke: " + text);
                }

            } finally {
                JoH.releaseWakeLock(wl);
            }
        }).start();

    }

    // get the locale that text to speech should be using
    public static Locale getLocale() {
        try {
            if (tts != null) {
                // if we have an instance return what we are actually using
                return tts.getLanguage();
            } else {
                // if we don't have an instance return what we would like to be using
                return chosenLocale();
            }
        } catch (Exception e) {
            // always try to return something
            return chosenLocale();
        }
    }

    // evaluate locale from system and user settings
    private static Locale chosenLocale() {
        // first get the default language
        Locale speech_locale = Locale.getDefault();
        try {
            final String tts_language = Pref.getStringDefaultBlank("speak_readings_custom_language").trim();
            // did the user specify another language for speech?
            if (tts_language.length() > 1) {
                final String[] lang_components = tts_language.split("_");
                final String country = (lang_components.length > 1) ? lang_components[1] : "";
                speech_locale = new Locale(lang_components[0], country, "");
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception trying to use custom language: " + e);
        }
        return speech_locale;
    }

    // set up an instance to Android TTS with our desired language and settings
    private synchronized static void initialize() {
        if (tts != null) return;
        tts = new TextToSpeech(xdrip.getAppContext(), status -> {

            if (status == TextToSpeech.SUCCESS && tts != null) {
                UserError.Log.d(TAG, "Initializing, successful result code: " + status);

                final Locale speech_locale = chosenLocale();

                UserError.Log.d(TAG, "Chosen locale: " + speech_locale);

                int set_language_result;
                // try setting the language we want
                try {
                    set_language_result = tts.setLanguage(speech_locale);
                } catch (IllegalArgumentException e) {
                    // can end up here with Locales like "OS"
                    UserError.Log.e(TAG, "Got TTS set language error: " + e.toString());
                    set_language_result = TextToSpeech.LANG_MISSING_DATA;
                } catch (Exception e) {
                    // can end up here with deep errors from tts system
                    UserError.Log.e(TAG, "Got TTS set language deep error: " + e.toString());
                    set_language_result = TextToSpeech.LANG_MISSING_DATA;
                }

                // try various fallbacks
                if (set_language_result == TextToSpeech.LANG_MISSING_DATA
                        || set_language_result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    UserError.Log.e(TAG, "Default system language is not supported");
                    try {
                        set_language_result = tts.setLanguage(Locale.ENGLISH);
                    } catch (IllegalArgumentException e) {
                        // can end up here with parcel Locales like "OS"
                        UserError.Log.e(TAG, "Got TTS set default language error: " + e.toString());
                        set_language_result = TextToSpeech.LANG_MISSING_DATA;
                    } catch (Exception e) {
                        // can end up here with deep errors from tts system
                        UserError.Log.e(TAG, "Got TTS set default language deep error: " + e.toString());
                        set_language_result = TextToSpeech.LANG_MISSING_DATA;
                    }
                }
                //try any english as last resort
                if (set_language_result == TextToSpeech.LANG_MISSING_DATA
                        || set_language_result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    UserError.Log.e(TAG, "English is not supported! total failure");
                    tts = null;
                }
            } else {
                UserError.Log.e(TAG, "Initialize status code indicates failure, code: " + status);
                tts = null;
            }
        });
    }

    // shutdown existing instance - most useful when changing language or parameters
    public static synchronized void shutdown() {
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "Got exception shutting down service: " + e);
            }
            tts = null;
        }
    }


    // this is a duplicate of similar code in JoH utility class but kept here as these methods will be specific for the speech util
    @SuppressWarnings("ConstantConditions")
    private static boolean isOngoingCall() {
        final AudioManager manager = (AudioManager) xdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        try {
            return (manager.getMode() == AudioManager.MODE_IN_CALL);
        } catch (NullPointerException e) {
            return false;
        }
    }

    // this isn't as complete as I would like - does the framework expose anything more generic we can use to detect any sound playing?
    @SuppressWarnings("ConstantConditions")
    private static boolean isMusicPlaying() {
        final AudioManager manager = (AudioManager) xdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        try {
            return manager.isMusicActive();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // redirect user to android tts data file installation activity
    public static void installTTSData(Context context) {
        try {
            final Intent intent = new Intent();
            intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UserError.Log.e(TAG, "Could not install TTS data: " + e.toString());
        }
    }

}
