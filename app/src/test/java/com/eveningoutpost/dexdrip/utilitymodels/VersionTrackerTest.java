package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.R;
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
 * That line is a user event: it is written for the user to read, so it is formatted from a
 * translatable resource and shows in the phone's language. {@link TranslationMarkingContext} is
 * what lets a test under the English-only {@code fast} flavor see that it came from one.
 * <p>
 * Only the version-name path is exercised. {@code updateDevice()} runs once per process behind a
 * static flag, and the Robolectric sandbox keeps that flag between tests, so a second test here
 * would find it already set.
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

    /** A new version name is logged, in the user's language, as the old name and the new one. */
    @Test
    public void aChangedVersionNameIsLoggedTranslated() {
        // :: Setup
        PersistentStore.setString("PHONE_VERSION_SHORT", "old-version");

        // :: Act
        VersionTracker.updateDevice();

        // :: Verify
        assertWithMessage("what VersionTracker logged")
                .that(messages())
                .containsExactly(TranslationMarkingContext.mark(RuntimeEnvironment.application.getString(
                        R.string.xdrip_software_changed_format, "old-version", BuildConfig.VERSION_NAME)));
    }

    private static List<String> messages() {
        return ShadowLog.getLogsForTag(TAG).stream().map(log -> log.msg).collect(Collectors.toList());
    }
}
