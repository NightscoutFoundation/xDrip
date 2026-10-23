package com.eveningoutpost.dexdrip;

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
 * Behavioural tests for the connection status line on {@link SystemStatusFragment}.
 * <p>
 * A wifi uploader is reached over the network, so the bluetooth connection state the fragment
 * reports for every other collector says nothing about it. WifiWixel therefore has its own arm,
 * and these tests pin the contrast: without it the row would claim "Not connected" to a user whose
 * collector was never on bluetooth in the first place.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class SystemStatusFragmentTest extends RobolectricTestWithConfig {

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    // --- connection status ---

    /** A wifi uploader has no bluetooth connection to report, so the row says no data. */
    @Test
    public void wifiWixelReportsNoData() {
        // :: Setup
        storeCollectionMethod("WifiWixel");

        // :: Act
        TextView status = connectionStatus();

        // :: Verify
        assertThat(status.getText().toString())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.no_data));
    }

    /** A bluetooth collector still reports its bluetooth connection state. */
    @Test
    public void bluetoothCollectorsReportTheirConnectionState() {
        // :: Setup
        storeCollectionMethod("BluetoothWixel");

        // :: Act
        TextView status = connectionStatus();

        // :: Verify
        assertThat(status.getText().toString())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.not_connected));
    }

    // --- Helpers ---

    private void storeCollectionMethod(String method) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("dex_collection_method", method).commit();
    }

    private TextView connectionStatus() {
        final FragmentActivity host = Robolectric.buildActivity(HostActivity.class).setup().get();
        final SystemStatusFragment fragment = new SystemStatusFragment();
        host.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
        return fragment.getView().findViewById(R.id.connection_status);
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
