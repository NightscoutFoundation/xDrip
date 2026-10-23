package com.eveningoutpost.dexdrip.alert;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assume.assumeTrue;

import android.content.res.Configuration;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;

import java.util.Locale;

import lombok.val;

/**
 * JamOrHam
 */
public class SensorExpiryTest extends RobolectricTestWithConfig {

    private static String masterNote(final String expiry) {
        return SensorExpiry.NOTE_PREFIX + xdrip.gs(R.string.sensor_will_expire_in, expiry);
    }

    // the note as a master set to another language would have written it
    private static String masterNoteIn(final String language, final String expiry) {
        val context = xdrip.getAppContext();
        val configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        return SensorExpiry.NOTE_PREFIX + context.createConfigurationContext(configuration)
                .getString(R.string.sensor_will_expire_in, expiry);
    }

    // sets the language of the app itself
    private static Locale setAppLanguage(final Locale locale) {
        val resources = xdrip.getAppContext().getResources();
        val configuration = resources.getConfiguration();
        val previous = configuration.getLocales().get(0);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return previous;
    }

    @Test
    public void testRecognizesNoteFromMaster() {
        assertWithMessage("note as written by the master")
                .that(SensorExpiry.isExpiryNote(masterNote("12.0 hours"))).isTrue();

        assertWithMessage("note appended to an earlier one")
                .that(SensorExpiry.isExpiryNote("Reminder: Take insulin → " + masterNote("2 hours"))).isTrue();
    }

    /**
     * Master and follower both on the same language, and not English.
     */
    @Test
    public void testRecognizesNoteWhenBothAreSetToTheSameLanguage() {
        val previous = setAppLanguage(Locale.SIMPLIFIED_CHINESE);
        try {
            val note = masterNote("12.0");
            // the fast flavour is built with resConfigs "en", so there is nothing to match there
            assumeTrue("build includes translations", !note.contains("will expire"));

            assertWithMessage("chinese note on a chinese follower: " + note)
                    .that(SensorExpiry.isExpiryNote(note)).isTrue();
        } finally {
            setAppLanguage(previous);
        }
    }

    @Test
    public void testRecognizesNoteFromMasterInAnotherLanguage() {
        assumeTrue("build includes translations",
                !masterNoteIn("fr", "12,0").equals(masterNote("12,0")));

        for (val language : new String[]{"fr", "it", "pl", "sv", "ko", "bg", "de"}) {
            val note = masterNoteIn(language, "12,0");
            assertWithMessage("note from a master set to " + language + ": " + note)
                    .that(SensorExpiry.isExpiryNote(note)).isTrue();
        }
    }

    @Test
    public void testIgnoresOtherNotes() {
        assertWithMessage("unrelated note")
                .that(SensorExpiry.isExpiryNote("Sensor started")).isFalse();

        assertWithMessage("empty note")
                .that(SensorExpiry.isExpiryNote("")).isFalse();

        assertWithMessage("null note")
                .that(SensorExpiry.isExpiryNote(null)).isFalse();
    }

    @Test
    public void testNotifiesOncePerNote() {
        PersistentStore.removeItem("PREF_SENSOR_EXPIRE_FOLLOWER_NOTIFIED");
        val noteTime = JoH.tsl();

        assertWithMessage("note seen for the first time")
                .that(SensorExpiry.firstSightOf(noteTime)).isTrue();

        assertWithMessage("same note resent by the master")
                .that(SensorExpiry.firstSightOf(noteTime)).isFalse();

        assertWithMessage("earlier note arriving late")
                .that(SensorExpiry.firstSightOf(noteTime - Constants.MINUTE_IN_MS)).isFalse();

        assertWithMessage("note for the next threshold")
                .that(SensorExpiry.firstSightOf(noteTime + Constants.MINUTE_IN_MS)).isTrue();
    }

    @Test
    public void testShowsSameMessageAsTheMaster() {
        val expireMsg = xdrip.gs(R.string.sensor_will_expire_in, "6 hours");

        assertWithMessage("warning prefix stripped")
                .that(SensorExpiry.messageFromNote(masterNote("6 hours"))).isEqualTo(expireMsg);

        assertWithMessage("note without the prefix left alone")
                .that(SensorExpiry.messageFromNote(expireMsg)).isEqualTo(expireMsg);
    }

}
