package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.TranslationMarkingContext;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Covers the line {@link VersionTracker} writes to the Event Log when the installed version changes.
 * <p>
 * The Event Log is read by two people: the user, in their own language, and whoever helps them
 * from a screenshot, in English. So the line is the translated text with the English after it, and
 * just the English when the two are the same. {@link TranslationMarkingContext} marks text read in
 * the user's language, which is how a test under the English-only {@code fast} flavor sees both.
 * <p>
 * Only the version-name path is exercised. {@code updateDevice()} runs once per process behind a
 * static flag, and the Robolectric sandbox keeps that flag between tests, so a second test here
 * would find it already set. The two build-timestamp paths are held to the same rule by
 * {@code NoTranslatedStringsInLogsTest}.
 * <p>
 * The run leaves the stored version and build-time entries in {@code PersistentStore} behind, as
 * the app itself would. No other test reads them, and the flag means this one cannot run twice.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class VersionTrackerTest extends RobolectricTestWithConfig {

    private static final String TAG = VersionTracker.class.getSimpleName();

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(new TranslationMarkingContext(RuntimeEnvironment.application));
    }

    @After
    public void tearDown() {
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    /** A new version name is logged as the old name and the new one, translated and in English. */
    @Test
    public void aChangedVersionNameIsLoggedTranslatedAndInEnglish() {
        // :: Setup
        PersistentStore.setString("PHONE_VERSION_SHORT", "old-version");

        // :: Act
        VersionTracker.updateDevice();

        // :: Verify
        final String english = "Software version changed from: old-version to " + BuildConfig.VERSION_NAME;
        assertWithMessage("what VersionTracker logged")
                .that(messages())
                .containsExactly(TranslationMarkingContext.mark(english) + " (" + english + ")");
    }

    private static List<String> messages() {
        return ShadowLog.getLogsForTag(TAG).stream().map(log -> log.msg).collect(Collectors.toList());
    }
}
