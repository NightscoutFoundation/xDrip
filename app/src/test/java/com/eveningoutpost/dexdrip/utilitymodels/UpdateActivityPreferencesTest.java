package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the update settings {@link UpdateActivity} reads and writes: the automatic update switch,
 * the internal downloader box and the update channel.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class UpdateActivityPreferencesTest extends RobolectricTestWithConfig {

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
    }

    // ===== Settings screen ===========================================================================================

    /** The automatic update switch shows the stored setting. */
    @Test
    public void autoUpdateSwitchShowsTheStoredSetting() {
        // :: Setup
        store().edit().putBoolean("auto_update_download", false).commit();

        // :: Act
        final UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class).create().get();

        // :: Verify
        assertThat(((Switch) activity.findViewById(R.id.autoupdate)).isChecked()).isFalse();
    }

    /** Flipping the automatic update switch stores the new setting. */
    @Test
    public void flippingTheAutoUpdateSwitchStoresIt() {
        // :: Setup
        final UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class).create().get();

        // :: Act
        ((Switch) activity.findViewById(R.id.autoupdate)).setChecked(false);

        // :: Verify
        assertThat(store().getBoolean("auto_update_download", true)).isFalse();
    }

    /** Ticking off the internal downloader stores the new setting. */
    @Test
    public void untickingTheInternalDownloaderStoresIt() {
        // :: Setup
        final UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class).create().get();

        // :: Act
        ((CheckBox) activity.findViewById(R.id.internaldownloadercheckBox)).setChecked(false);

        // :: Verify
        assertThat(store().getBoolean("use_internal_downloader", true)).isFalse();
    }

    /** The screen names the stored update channel. */
    @Test
    public void screenNamesTheStoredChannel() {
        // :: Setup
        store().edit().putString("update_channel", "alpha").commit();

        // :: Act
        final UpdateActivity activity = Robolectric.buildActivity(UpdateActivity.class).create().get();

        // :: Verify
        assertThat(((TextView) activity.findViewById(R.id.update_channel)).getText().toString())
                .isEqualTo("Update Channel: Alpha");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
