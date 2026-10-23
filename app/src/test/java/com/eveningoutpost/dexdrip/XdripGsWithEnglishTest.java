package com.eveningoutpost.dexdrip;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

/**
 * Covers {@link xdrip#gsWithEnglish}, the text the Event Log gets from a string resource.
 * <p>
 * The Event Log is read by the user, in their own language, and by whoever helps them, in English.
 * A test cannot count on translations being there (the {@code fast} flavor has English only), so a
 * language other than English is stood in for by {@link TranslationMarkingContext}: text it returns
 * is marked, as text in another language would differ, while the English read underneath it is not.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class XdripGsWithEnglishTest extends RobolectricTestWithConfig {

    @After
    public void tearDown() {
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    /** In another language the user's text comes first and the English follows it in brackets. */
    @Test
    public void textInAnotherLanguageIsFollowedByTheEnglish() {
        // :: Setup
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));
        final String english = "Software version changed from: 1 to 2";

        // :: Act
        final String text = xdrip.gsWithEnglish(R.string.xdrip_software_changed_format, "1", "2");

        // :: Verify
        assertWithMessage("Event Log text").that(text)
                .isEqualTo(TranslationMarkingContext.mark(english) + " (" + english + ")");
    }

    /** Reading the English must not switch the app itself to English. */
    @Test
    public void readingTheEnglishLeavesTheUsersLanguageAlone() {
        // :: Setup
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));

        // :: Act
        xdrip.gsWithEnglish(R.string.xdrip_software_changed_format, "1", "2");

        // :: Verify
        assertWithMessage("the app's language afterwards")
                .that(xdrip.getAppContext().getResources().getConfiguration().getLocales().get(0))
                .isEqualTo(Locale.GERMAN);
    }

    /** In English the text is given once, so English users see the line they always did. */
    @Test
    public void textInEnglishIsGivenOnce() {
        // :: Setup
        xdrip.setContextAlways(RuntimeEnvironment.application);

        // :: Act
        final String text = xdrip.gsWithEnglish(R.string.xdrip_software_changed_format, "1", "2");

        // :: Verify
        assertWithMessage("Event Log text").that(text).isEqualTo("Software version changed from: 1 to 2");
    }

    /** A string without arguments works the same way. */
    @Test
    public void aStringWithoutArgumentsIsFollowedByTheEnglish() {
        // :: Setup
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));
        final String english = RuntimeEnvironment.application.getString(R.string.setting_pref_to_max);

        // :: Act
        final String text = xdrip.gsWithEnglish(R.string.setting_pref_to_max);

        // :: Verify
        assertWithMessage("Event Log text").that(text)
                .isEqualTo(TranslationMarkingContext.mark(english) + " (" + english + ")");
    }

    /** Without arguments the text is taken as it stands, so a percent sign in it is just a percent sign. */
    @Test
    public void aStringWithoutArgumentsIsNotFormatted() {
        // :: Setup
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));

        // :: Act
        final String text = xdrip.gsWithEnglish(R.string.blueReader_turnoffvalue);

        // :: Verify
        assertWithMessage("Event Log text").that(text)
                .isEqualTo(TranslationMarkingContext.mark("Value in %") + " (Value in %)");
    }
}
