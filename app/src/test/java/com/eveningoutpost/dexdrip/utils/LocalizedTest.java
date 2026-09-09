package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assume.assumeTrue;

import android.content.res.Configuration;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;

import java.util.Locale;

import lombok.val;

// JamOrHam
public class LocalizedTest extends RobolectricTestWithConfig {

    // the text as a device set to another language would have written it
    private static String writtenIn(final String language, final int resId, final String... args) {
        val context = xdrip.getAppContext();
        val configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        val localized = context.createConfigurationContext(configuration);
        // the varargs version formats the string, so it cannot give us the raw wording
        return args.length > 0 ? localized.getString(resId, (Object[]) args) : localized.getString(resId);
    }

    // sets the language of the app itself, the same way xdrip.checkForcedEnglish() does
    private static Locale setAppLanguage(final Locale locale) {
        val resources = xdrip.getAppContext().getResources();
        val configuration = resources.getConfiguration();
        val previous = configuration.getLocales().get(0);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return previous;
    }

    private static boolean isTranslated(final int resId) {
        return !writtenIn("fr", resId).equals(writtenIn("en", resId));
    }

    @Test
    public void testMatchesPlainString() {
        assertWithMessage("the string itself")
                .that(Localized.matches(R.string.sensor_expiring, "Sensor expiring")).isTrue();

        assertWithMessage("case and surrounding space ignored")
                .that(Localized.matches(R.string.sensor_expiring, "  SENSOR EXPIRING ")).isTrue();

        assertWithMessage("a different string")
                .that(Localized.matches(R.string.sensor_expiring, "Sensor started")).isFalse();

        assertWithMessage("nothing")
                .that(Localized.matches(R.string.sensor_expiring, null)).isFalse();
    }

    @Test
    public void testMatchesFormatStringWhateverWasSubstitutedIn() {
        assertWithMessage("as written")
                .that(Localized.matches(R.string.sensor_will_expire_in, "Sensor will expire in 12.0 hours")).isTrue();

        assertWithMessage("a different value")
                .that(Localized.matches(R.string.sensor_will_expire_in, "Sensor will expire in next Tuesday")).isTrue();

        assertWithMessage("no value substituted in")
                .that(Localized.matches(R.string.sensor_will_expire_in, "Sensor will expire in")).isFalse();

        assertWithMessage("the wording of another string")
                .that(Localized.matches(R.string.sensor_will_expire_in, "Sensor expiring")).isFalse();
    }

    @Test
    public void testDistinguishesWholeTextFromTextItAppearsIn() {
        val concatenated = "Warning: " + writtenIn("en", R.string.sensor_will_expire_in, "2 hours") + " → Feeling low";

        assertWithMessage("string concatenated into other text is not the whole of it")
                .that(Localized.matches(R.string.sensor_will_expire_in, concatenated)).isFalse();

        assertWithMessage("but it does appear in it")
                .that(Localized.appearsIn(R.string.sensor_will_expire_in, concatenated)).isTrue();

        assertWithMessage("unrelated text")
                .that(Localized.appearsIn(R.string.sensor_will_expire_in, "Reminder: Take insulin")).isFalse();
    }

    @Test
    public void testMatchesTextWrittenByADeviceInAnotherLanguage() {
        assumeTrue("build includes translations", isTranslated(R.string.sensor_will_expire_in));

        for (val language : new String[]{"fr", "it", "pl", "sv", "ko", "bg"}) {
            val text = writtenIn(language, R.string.sensor_will_expire_in, "12,0");
            assertWithMessage("written by a device set to " + language + ": " + text)
                    .that(Localized.matches(R.string.sensor_will_expire_in, text)).isTrue();
        }
    }

    @Test
    public void testMatchesTextWrittenInOurOwnLanguage() {
        val previous = setAppLanguage(Locale.SIMPLIFIED_CHINESE);
        try {
            assumeTrue("build includes translations", isTranslated(R.string.sensor_will_expire_in));
            val text = xdrip.gs(R.string.sensor_will_expire_in, "12.0");

            assertWithMessage("written by a chinese device, read by a chinese one: " + text)
                    .that(Localized.matches(R.string.sensor_will_expire_in, text)).isTrue();
        } finally {
            setAppLanguage(previous);
        }
    }

}
