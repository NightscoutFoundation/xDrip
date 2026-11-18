package com.eveningoutpost.dexdrip.utils.settings;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Pins how the settings screen behaves for a person: deep links, sub-screens, back, dependencies, search and the
 * ringtone picker. Written against today's screen; the move to androidx must keep every test green unchanged.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class SettingsScreenTest extends RobolectricTestWithConfig {

    private static final String SPEAK_SCREEN = "xdrip_speak_readings_settings";
    private static final String SPEAK_THRESHOLD = "speak_readings_change_threshold";
    private static final String ALERT_SOUND = "other_alerts_sound";
    /** Hidden {@code RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS}. */
    private static final String AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        android.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit().clear().commit();
        xdrip.loadDefaultPreferences(RuntimeEnvironment.application);
    }

    // ===== Navigation ================================================================================================

    /**
     * Opening settings without an action shows the root screen.
     */
    @Test
    public void opensRoot() {
        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Verify
        assertThat(driver.shownScreenKey()).isNull();
    }

    /**
     * The deep link of the speak-readings action opens that sub-screen with its own settings on it.
     */
    @Test
    public void deepLinkOpensSubScreen() {
        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Verify
        assertThat(driver.shownScreenKey()).isEqualTo(SPEAK_SCREEN);
        assertThat(driver.isShown(SPEAK_THRESHOLD)).isTrue();
    }

    /**
     * Back from a deep-linked sub-screen returns to the root.
     */
    @Test
    public void backReturnsToRoot() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        driver.back();

        // :: Verify
        assertThat(driver.shownScreenKey()).isNull();
    }

    /**
     * Every keyed sub-screen of the settings XMLs that is in the live tree opens when tapped. Screens that only start
     * an activity (an {@code <intent>} child, or no children and a click listener in code, like the QR scanner) are
     * not sub-screens a person navigates into, so they are left out here. A disabled sub-screen (its dependency is off)
     * stays shut.
     */
    @Test
    public void everyScreenKeyOpens() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final List<String> opened = new ArrayList<>();

        // :: Act
        final Map<String, String> intentScreens = SettingsXml.intentScreens();
        final List<String> actionScreens = SettingsXml.childlessScreens();
        for (final String key : SettingsXml.keysOf("PreferenceScreen")) {
            if (!driver.exists(key) || intentScreens.containsKey(key) || actionScreens.contains(key)) {
                continue;
            }
            driver.showScreenOf(key);
            final String before = driver.shownScreenKey();
            final boolean enabled = driver.isEnabled(key);
            driver.openScreen(key);
            assertWithMessage("sub-screen " + key + " (enabled " + enabled + ")").that(driver.shownScreenKey())
                    .isEqualTo(enabled ? key : before);
            if (enabled) {
                opened.add(key);
            }
        }

        // :: Verify
        assertThat(opened.size()).isAtLeast(50);
    }

    /**
     * A sub-screen that holds an intent starts that activity when tapped, and stays on the screen it was tapped on.
     */
    @Test
    public void intentScreensStartTheirActivity() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final List<String> started = new ArrayList<>();

        // :: Act
        for (final Map.Entry<String, String> screen : SettingsXml.intentScreens().entrySet()) {
            final String key = screen.getKey();
            if (!driver.exists(key)) {
                continue;
            }
            driver.showScreenOf(key);
            final String before = driver.shownScreenKey();
            driver.click(key);
            final Intent intent = driver.nextStartedActivity();
            assertWithMessage("activity of " + key).that(intent.getComponent().getClassName())
                    .isEqualTo(screen.getValue());
            assertWithMessage("screen after " + key).that(driver.shownScreenKey()).isEqualTo(before);
            started.add(key);
        }

        // :: Verify
        assertThat(started).isNotEmpty();
    }

    // ===== Dependencies ==============================================================================================

    /**
     * The speak threshold follows the speak switch on the same sub-screen: off disables it, on enables it.
     */
    @Test
    public void dependencyFollowsSwitchInSubScreen() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        driver.setChecked("bg_to_speech", false);
        final boolean enabledWhenOff = driver.isEnabled(SPEAK_THRESHOLD);
        driver.setChecked("bg_to_speech", true);
        final boolean enabledWhenOn = driver.isEnabled(SPEAK_THRESHOLD);

        // :: Verify
        assertThat(enabledWhenOff).isFalse();
        assertThat(enabledWhenOn).isTrue();
    }

    // ===== Search ====================================================================================================

    /**
     * A search result for a setting in a sub-screen opens that sub-screen. The speak threshold has no title, so it is
     * found and picked by its summary. Search shows at most ten results and a broad word such as "speak" matches more
     * than ten, so the search uses the summary itself.
     */
    @Test
    public void searchOpensSubScreen() {
        // :: Setup
        final String summary = RuntimeEnvironment.getApplication().getString(R.string.title_When_changes_by);
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.search(summary);

        // :: Act
        driver.clickSearchResult(summary);

        // :: Verify
        assertThat(driver.shownScreenKey()).isEqualTo(SPEAK_SCREEN);
    }

    // ===== Ringtone ==================================================================================================

    /**
     * Tapping the alert sound starts the system ringtone picker with today's extras.
     */
    @Test
    public void ringtonePickerIntent() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.showScreenOf(ALERT_SOUND);

        // :: Act
        driver.click(ALERT_SOUND);

        // :: Verify
        final Intent intent = driver.nextStartedActivityForResult();
        assertThat(intent.getAction()).isEqualTo(RingtoneManager.ACTION_RINGTONE_PICKER);
        assertThat(intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1)).isEqualTo(RingtoneManager.TYPE_ALL);
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)).isTrue();
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)).isTrue();
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI))
                .isEqualTo(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL));
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI))
                .isEqualTo(Uri.parse("default"));
        assertThat(String.valueOf(intent.getCharSequenceExtra(RingtoneManager.EXTRA_RINGTONE_TITLE)))
                .isEqualTo(RuntimeEnvironment.getApplication().getString(R.string.alert_sound));
        assertThat(intent.getIntExtra(AUDIO_ATTRIBUTES_FLAGS, -1)).isEqualTo(0x40); // FLAG_BYPASS_INTERRUPTION_POLICY
    }

    /**
     * A picked sound is stored as its uri string, "no sound" is stored as an empty string, and a cancelled picker
     * (no data) leaves the stored value alone.
     */
    @Test
    public void ringtoneResultIsStoredAsUriString() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.showScreenOf(ALERT_SOUND);
        final Uri picked = Settings.System.DEFAULT_NOTIFICATION_URI;

        // :: Act
        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_OK,
                new Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, picked));
        final String afterPick = storedSound();

        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_OK, new Intent());
        final String afterSilent = storedSound();

        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_CANCELED, null);
        final String afterCancel = storedSound();

        // :: Verify
        assertThat(afterPick).isEqualTo(picked.toString());
        assertThat(afterSilent).isEqualTo("");
        assertThat(afterCancel).isEqualTo("");
    }

    // ===== Helpers ===================================================================================================

    private static String storedSound() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .getString(ALERT_SOUND, "<absent>");
    }
}
