package com.eveningoutpost.dexdrip.cgm.sharefollow;

import static com.google.common.truth.Truth.assertWithMessage;

import android.util.Log;

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
 * Covers what {@link ShareFollowDownload} reports when the Share login it was given is incomplete.
 * <p>
 * The error log is for the support group and is English; the status line is what the user reads on
 * the status page and stays translated. {@link TranslationMarkingContext} marks every resource read,
 * which is how a test under the English-only {@code fast} flavor tells the two apart.
 * <p>
 * The status text is also logged at debug level, by {@code msg()}, as {@code "Setting message: "}
 * plus the translated text. That is text passed in from another method, which the source check
 * does not trace, and it is left as it is.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class ShareFollowDownloadTest extends RobolectricTestWithConfig {

    private static final String TAG = "ShareFollowDL";

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

    /** Without a username the download gives up, and the error log says why in English. */
    @Test
    public void anIncompleteLoginIsLoggedInEnglish() {
        // :: Setup
        final ShareFollowDownload download = new ShareFollowDownload("https://share.example", "", "secret");

        // :: Act
        final boolean started = download.doEverything(10);

        // :: Verify
        assertWithMessage("whether anything was started").that(started).isFalse();
        assertWithMessage("what ShareFollowDownload logged as an error")
                .that(errors())
                .containsExactly("Share Login data isn't valid - check username / password");
    }

    /** The user still reads why in their own language. */
    @Test
    public void theStatusLineStaysTranslated() {
        // :: Setup
        final ShareFollowDownload download = new ShareFollowDownload("https://share.example", "", "secret");

        // :: Act
        download.doEverything(10);

        // :: Verify
        assertWithMessage("status line")
                .that(download.getStatus())
                .endsWith(": " + TranslationMarkingContext.mark(
                        RuntimeEnvironment.application.getString(R.string.share_login_data_isnt_valid)));
    }

    private static List<String> errors() {
        return ShadowLog.getLogsForTag(TAG).stream()
                .filter(log -> log.type == Log.ERROR)
                .map(log -> log.msg)
                .collect(Collectors.toList());
    }
}
