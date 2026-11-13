package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.CheckBox;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the answer {@link NFCReaderX#enableBluetoothAskUser(Activity)} stores in the default
 * preference store when the user ticks "don't ask again".
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class NFCReaderXBluetoothStreamingPreferenceTest extends RobolectricTestWithConfig {

    private static final String KEY = "libre2_enable_bluetooth_streaming";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
    }

    // ===== Don't ask again ===========================================================================================

    /** "Yes" with "don't ask again" ticked stores that streaming is always enabled. */
    @Test
    public void yesWithDontAskAgainStoresAlways() {
        // :: Setup
        final AlertDialog dialog = askWithDontAskAgainTicked();

        // :: Act
        dialog.findViewById(R.id.enable_streaming_yes).performClick();

        // :: Verify
        assertThat(store().getString(KEY, null)).isEqualTo("enable_streaming_always");
    }

    /** "No" with "don't ask again" ticked stores that streaming is never enabled. */
    @Test
    public void noWithDontAskAgainStoresNever() {
        // :: Setup
        final AlertDialog dialog = askWithDontAskAgainTicked();

        // :: Act
        dialog.findViewById(R.id.enable_streaming_no).performClick();

        // :: Verify
        assertThat(store().getString(KEY, null)).isEqualTo("enable_streaming_never");
    }

    // =================================================================================================================

    private static AlertDialog askWithDontAskAgainTicked() {
        final Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NFCReaderX.enableBluetoothAskUser(activity);
        final AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        ((CheckBox) dialog.findViewById(R.id.enable_streaming_dont_ask_again)).setChecked(true);
        return dialog;
    }

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
