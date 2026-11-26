package com.eveningoutpost.dexdrip.utils.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link RingtonePickerPreference}: it starts the system sound picker with the same extras as the old ringtone
 * setting, and stores the result the same way.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class RingtonePickerPreferenceTest extends RobolectricTestWithConfig {

    private static final String KEY = "test_alert_sound";
    private static final String TITLE = "Alert sound";
    /** Hidden {@code RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS}. */
    private static final String AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";

    // ===== Picker intent =============================================================================================

    /**
     * A sound setting as the settings XML writes it starts the picker with the stored sound, the default sound offered,
     * silence offered, every sound type, its title, and alerts allowed through Do Not Disturb.
     */
    @Test
    public void pickerIntentHasTheOldExtras() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final RingtonePickerPreference sound = addTo(host, Robolectric.buildAttributeSet()
                .addAttribute(android.R.attr.ringtoneType, "all")
                .addAttribute(android.R.attr.showSilent, "true"));
        prefs().edit().putString(KEY, "default").commit();

        // :: Act
        final Intent intent = sound.buildPickerIntent();

        // :: Verify
        assertThat(intent.getAction()).isEqualTo(RingtoneManager.ACTION_RINGTONE_PICKER);
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI))
                .isEqualTo(Uri.parse("default"));
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)).isTrue();
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI))
                .isEqualTo(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL));
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)).isTrue();
        assertThat(intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1)).isEqualTo(RingtoneManager.TYPE_ALL);
        assertThat(String.valueOf(intent.getCharSequenceExtra(RingtoneManager.EXTRA_RINGTONE_TITLE)))
                .isEqualTo(TITLE);
        assertThat(intent.getIntExtra(AUDIO_ATTRIBUTES_FLAGS, -1)).isEqualTo(0x40); // FLAG_BYPASS_INTERRUPTION_POLICY
    }

    /**
     * With no picker attributes and nothing stored, the picker offers ringtones, the default and silence, as the old
     * setting did by default, and is told there is no current sound.
     */
    @Test
    public void pickerIntentWithoutAttributesUsesOldDefaults() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final RingtonePickerPreference sound = addTo(host, Robolectric.buildAttributeSet());

        // :: Act
        final Intent intent = sound.buildPickerIntent();

        // :: Verify
        assertThat(intent.hasExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI)).isTrue();
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI)).isNull();
        assertThat(intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1))
                .isEqualTo(RingtoneManager.TYPE_RINGTONE);
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)).isTrue();
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI))
                .isEqualTo(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)).isTrue();
    }

    /**
     * A setting that does not offer the default sound leaves the default sound out of the intent, as the old one did.
     */
    @Test
    public void noShowDefaultLeavesOutDefaultUri() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final RingtonePickerPreference sound = addTo(host, Robolectric.buildAttributeSet()
                .addAttribute(android.R.attr.showDefault, "false")
                .addAttribute(android.R.attr.showSilent, "false"));

        // :: Act
        final Intent intent = sound.buildPickerIntent();

        // :: Verify
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)).isFalse();
        assertThat(intent.hasExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI)).isFalse();
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)).isFalse();
    }

    // ===== Picker result =============================================================================================

    /**
     * A picked sound is stored as its uri string.
     */
    @Test
    public void pickedSoundIsStoredAsUriString() {
        // :: Setup
        final RingtonePickerPreference sound = addTo(PreferenceTestHost.start(), Robolectric.buildAttributeSet());
        final Uri picked = Settings.System.DEFAULT_NOTIFICATION_URI;

        // :: Act
        sound.onPickerResult(new Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, picked));

        // :: Verify
        assertThat(prefs().getString(KEY, "<absent>")).isEqualTo(picked.toString());
    }

    /**
     * Picking silence, which returns no uri, is stored as an empty string.
     */
    @Test
    public void silenceIsStoredAsEmptyString() {
        // :: Setup
        final RingtonePickerPreference sound = addTo(PreferenceTestHost.start(), Robolectric.buildAttributeSet());
        prefs().edit().putString(KEY, "default").commit();

        // :: Act
        sound.onPickerResult(new Intent());

        // :: Verify
        assertThat(prefs().getString(KEY, "<absent>")).isEqualTo("");
    }

    /**
     * A cancelled picker, which returns no data, leaves the stored sound alone.
     */
    @Test
    public void cancelledPickerKeepsStoredSound() {
        // :: Setup
        final RingtonePickerPreference sound = addTo(PreferenceTestHost.start(), Robolectric.buildAttributeSet());
        prefs().edit().putString(KEY, "default").commit();

        // :: Act
        sound.onPickerResult(null);

        // :: Verify
        assertThat(prefs().getString(KEY, "<absent>")).isEqualTo("default");
    }

    /**
     * A picked sound that the change listener turns down is not stored.
     */
    @Test
    public void rejectedChangeIsNotPersisted() {
        // :: Setup
        final RingtonePickerPreference sound = addTo(PreferenceTestHost.start(), Robolectric.buildAttributeSet());
        prefs().edit().putString(KEY, "default").commit();
        sound.setOnPreferenceChangeListener((preference, newValue) -> false);

        // :: Act
        sound.onPickerResult(new Intent()
                .putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Settings.System.DEFAULT_NOTIFICATION_URI));

        // :: Verify
        assertThat(prefs().getString(KEY, "<absent>")).isEqualTo("default");
    }

    // ===== Default value =============================================================================================

    /**
     * Adding the setting stores its default value when nothing is stored yet, as the old one did.
     */
    @Test
    public void initialValueIsPersisted() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();

        // :: Act
        addTo(host, Robolectric.buildAttributeSet().addAttribute(android.R.attr.defaultValue, "default"));

        // :: Verify
        assertThat(prefs().getString(KEY, "<absent>")).isEqualTo("default");
    }

    /**
     * Adding the setting leaves a stored sound alone.
     */
    @Test
    public void storedValueIsKeptOverDefault() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        prefs().edit().putString(KEY, "").commit();

        // :: Act
        addTo(host, Robolectric.buildAttributeSet().addAttribute(android.R.attr.defaultValue, "default"));

        // :: Verify
        assertThat(prefs().getString(KEY, "<absent>")).isEqualTo("");
    }

    // ===== Helpers ===================================================================================================

    /**
     * Builds the setting from {@code attrs} plus its key and title, and adds it to a page's tree.
     */
    private static RingtonePickerPreference addTo(final PreferenceTestHost host,
                                                  final org.robolectric.android.AttributeSetBuilder attrs) {
        final AttributeSet built = attrs
                .addAttribute(android.R.attr.key, KEY)
                .addAttribute(android.R.attr.title, TITLE)
                .build();
        final RingtonePickerPreference sound = new RingtonePickerPreference(host.fragment.requireContext(), built);
        final PreferenceScreen root = host.screen("root", "Root");
        root.addPreference(sound);
        host.setRoot(root);
        return sound;
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
    }
}
