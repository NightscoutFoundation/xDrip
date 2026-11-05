package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.TranslationMarkingContext;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Covers what {@link BgToSpeech} logs when it works out what to say.
 * <p>
 * The words spoken are the user's, in the user's language: above 400 mg/dL that is the translated
 * "HIGH". The log lines are for the support group, so they name the reading and the trend it was
 * given instead of the words, which keeps them English. {@link TranslationMarkingContext} marks
 * the strings read through it, which is how a test under the English-only {@code fast} flavor would
 * see a translated word slip into a log line.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class BgToSpeechTest extends RobolectricTestWithConfig {

    private static final String TAG = "BgToSpeech";

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));
        Pref.setBoolean("bg_to_speech_trend", true);
    }

    @After
    public void tearDown() {
        Pref.setBoolean("bg_to_speech_trend", false);
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    /** A reading spoken as a translated word is logged as the reading and its trend. */
    @Test
    public void aReadingSpokenAsAWordIsLoggedAsTheReading() {
        // :: Act
        BgToSpeech.realSpeakNow(450d, 0, "Flat");

        // :: Verify
        assertWithMessage("what BgToSpeech logged")
                .that(messages())
                .containsExactly("calculated text for value 450.0 delta Flat",
                        "Attempting to speak BG reading of: 450.0")
                .inOrder();
    }

    private static List<String> messages() {
        return ShadowLog.getLogsForTag(TAG).stream().map(log -> log.msg).collect(Collectors.toList());
    }
}
