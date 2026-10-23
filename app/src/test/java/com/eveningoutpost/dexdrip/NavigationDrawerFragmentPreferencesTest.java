package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests which preference store {@link NavigationDrawerFragment} writes to when it is created.
 * <p>
 * The fragment marks the drawer as learned in the default preference store as soon as it is created.
 * The drawer behaves the same whichever store receives the flag, so this pins where the write lands,
 * not a change the user can see.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class NavigationDrawerFragmentPreferencesTest extends RobolectricTestWithConfig {

    private static final String LEARNED_KEY = "navigation_drawer_learned";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
    }

    // ===== Drawer learned flag =======================================================================================

    /** Creating the fragment stores the drawer as learned in the default preference store. */
    @Test
    public void onCreate_marksTheDrawerLearned() {
        // :: Setup
        prefs().edit().putBoolean(LEARNED_KEY, false).commit();
        final Activity host = Robolectric.buildActivity(HostActivity.class).setup().get();

        // :: Act
        host.getFragmentManager().beginTransaction()
                .add(android.R.id.content, new NavigationDrawerFragment()).commit();
        host.getFragmentManager().executePendingTransactions();

        // :: Verify
        assertThat(prefs().getBoolean(LEARNED_KEY, false)).isTrue();
    }

    // ===== Helpers ===================================================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** Bare host; the fragment insists its activity takes drawer callbacks. */
    public static class HostActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
        @Override
        public void onNavigationDrawerItemSelected(int position) {
        }
    }
}
