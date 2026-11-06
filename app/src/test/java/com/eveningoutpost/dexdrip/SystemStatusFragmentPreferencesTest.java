package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the transmitter row on {@link SystemStatusFragment}.
 * <p>
 * A Dexcom Share receiver reports its own transmitter state, so with Share stored as the data source
 * the row points the user there instead of showing local transmitter data. The fragment reads the
 * data source from the default preference store when it is created.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class SystemStatusFragmentPreferencesTest extends RobolectricTestWithConfig {

    private static final String COLLECTION_METHOD_KEY = "dex_collection_method";
    private static final String SEE_SHARE_RECEIVER = "See Share Receiver";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
    }

    // ===== Share points to the receiver ==============================================================================

    /** With Dexcom Share stored as the data source, the row points to the Share receiver. */
    @Test
    public void dexcomShare_pointsToTheReceiver() {
        // :: Setup
        prefs().edit().putString(COLLECTION_METHOD_KEY, "DexcomShare").commit();

        // :: Act
        final TextView row = transmitterRow();

        // :: Verify
        assertThat(row.getText().toString()).isEqualTo(SEE_SHARE_RECEIVER);
    }

    // ===== Other sources show local transmitter state ================================================================

    /** A bluetooth collector shows its own transmitter state, not the Share pointer. */
    @Test
    public void bluetoothCollector_showsLocalState() {
        // :: Setup
        prefs().edit().putString(COLLECTION_METHOD_KEY, "BluetoothWixel").commit();

        // :: Act
        final TextView row = transmitterRow();

        // :: Verify
        assertThat(row.getText().toString()).isNotEqualTo(SEE_SHARE_RECEIVER);
    }

    // ===== Helpers ===================================================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private TextView transmitterRow() {
        final FragmentActivity host = Robolectric.buildActivity(HostActivity.class).setup().get();
        final SystemStatusFragment fragment = new SystemStatusFragment();
        host.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
        return fragment.getView().findViewById(R.id.transmitter_status);
    }

    /** Bare host for the fragment under test; the real host is the paged status activity. */
    public static class HostActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme);
            super.onCreate(savedInstanceState);
        }
    }
}
