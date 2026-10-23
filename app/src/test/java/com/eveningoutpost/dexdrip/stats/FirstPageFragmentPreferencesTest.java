package com.eveningoutpost.dexdrip.stats;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the target range line on the first statistics page, {@link FirstPageFragment}.
 * <p>
 * The page shows the user's high and low limits in their chosen unit, read from the default preference
 * store. The line is filled in by background threads, so each test waits for it to change from the
 * layout's placeholder.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class FirstPageFragmentPreferencesTest extends RobolectricTestWithConfig {

    private static final String PLACEHOLDER = ": ---";
    private static final long TIMEOUT_MS = 5000;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
    }

    // ===== Stored limits in mmol/l ===================================================================================

    /** Stored mmol/l limits are shown in mmol/l. */
    @Test
    public void mmolLimitsStored_showsThemInMmol() throws InterruptedException {
        // :: Setup
        prefs().edit()
                .putString("units", "mmol")
                .putString("highValue", "10")
                .putString("lowValue", "3.9")
                .commit();

        // :: Act
        final String range = rangeLine();

        // :: Verify
        assertThat(range).isEqualTo("3.9 - 10.0 mmol/l");
    }

    // ===== Defaults ==================================================================================================

    /** With nothing stored, the default limits are shown in mg/dl. */
    @Test
    public void nothingStored_showsTheDefaultsInMgdl() throws InterruptedException {
        // :: Setup
        // nothing stored; setUp cleared the preference store

        // :: Act
        final String range = rangeLine();

        // :: Verify
        assertThat(range).isEqualTo("70 - 170 mg/dl");
    }

    // ===== Helpers ===================================================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** Shows the page and waits until the background threads have replaced the placeholder. */
    private static String rangeLine() throws InterruptedException {
        final AppCompatActivity host = Robolectric.buildActivity(HostActivity.class).setup().get();
        final FirstPageFragment fragment = new FirstPageFragment();
        host.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
        final TextView view = fragment.getView().findViewById(R.id.textView_stats_range_set);

        final long deadline = JoH.tsl() + TIMEOUT_MS;
        while (PLACEHOLDER.contentEquals(view.getText()) && JoH.tsl() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(50);
        }
        return view.getText().toString();
    }

    /** Bare host for the fragment under test; the real host is the statistics activity. */
    public static class HostActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme);
            super.onCreate(savedInstanceState);
        }
    }
}
