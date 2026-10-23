package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.OUT_OF_RANGE_GLUCOSE_ENTRY_ID;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.eveningoutpost.dexdrip.R;
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
 * Covers what {@link Preferences#applyPrefSettingRange} tells the support group and the user when it
 * pulls an out-of-range glucose setting back into range.
 * <p>
 * The Event Log line is read by the user and by whoever helps them, so it is the translated text
 * with the English after it; the notification is for the user alone, so it stays translated.
 * {@link TranslationMarkingContext} marks the strings read through it, which is how a test under
 * the English-only {@code fast} flavor tells the two apart.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class PreferencesSettingRangeTest extends RobolectricTestWithConfig {

    private static final String TAG = "jamorham PREFS";
    private static final String KEY = "translated_log_test_threshold";
    private static final double MIN = 40d;
    private static final double MAX = 400d;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));
        Pref.setString("units", "mgdl");
    }

    @After
    public void tearDown() {
        Pref.removeItem(KEY);
        Pref.removeItem("units");
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    /** A value above the maximum is set to the maximum, and the Event Log says so translated and in English. */
    @Test
    public void aValueAboveTheMaximumIsLoggedTranslatedAndInEnglish() {
        // :: Setup
        Pref.setString(KEY, "500");

        // :: Act
        Preferences.applyPrefSettingRange(KEY, "170", MIN, MAX);

        // :: Verify
        assertWithMessage("setting after the correction").that(Pref.getString(KEY, "")).isEqualTo("400.0");
        final String english = KEY
                + " was set to a value greater than maximum allowed. It has been changed to maximum.";
        assertWithMessage("what Preferences logged")
                .that(messages())
                .contains(TranslationMarkingContext.mark(english) + " (" + english + ")");
    }

    /** A value below the minimum is set to the minimum, and the Event Log says so translated and in English. */
    @Test
    public void aValueBelowTheMinimumIsLoggedTranslatedAndInEnglish() {
        // :: Setup
        Pref.setString(KEY, "10");

        // :: Act
        Preferences.applyPrefSettingRange(KEY, "170", MIN, MAX);

        // :: Verify
        assertWithMessage("setting after the correction").that(Pref.getString(KEY, "")).isEqualTo("40.0");
        final String english = KEY
                + " was set to a value less than minimum allowed. It has been changed to minimum.";
        assertWithMessage("what Preferences logged")
                .that(messages())
                .contains(TranslationMarkingContext.mark(english) + " (" + english + ")");
    }

    /** The user is still told in their own language: the notification text comes from a resource. */
    @Test
    public void theNotificationStaysTranslated() {
        // :: Setup
        Pref.setString(KEY, "500");

        // :: Act
        Preferences.applyPrefSettingRange(KEY, "170", MIN, MAX);

        // :: Verify
        final NotificationManager manager = (NotificationManager)
                RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification notification = shadowOf(manager).getNotification(OUT_OF_RANGE_GLUCOSE_ENTRY_ID);
        assertWithMessage("notification for the corrected setting").that(notification).isNotNull();
        assertWithMessage("notification text")
                .that(String.valueOf(notification.extras.getCharSequence(Notification.EXTRA_TEXT)))
                .isEqualTo(TranslationMarkingContext.mark(RuntimeEnvironment.application.getString(R.string.setting_pref_to_max)));
    }

    private static List<String> messages() {
        return ShadowLog.getLogsForTag(TAG).stream().map(log -> log.msg).collect(Collectors.toList());
    }
}
