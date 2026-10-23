package com.eveningoutpost.dexdrip;

import android.widget.Button;
import android.widget.CheckBox;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link LicenseAgreementActivity}.
 * <p>
 * The activity's contract is a round trip: the stored I_understand preference decides whether the
 * agreement checkbox is ticked on open, and pressing Save writes the checkbox state back. Both
 * ends are asserted through the view hierarchy and the preference store, so the tests survive the
 * android.preference -> androidx PreferenceManager swap only if that contract is unchanged.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class LicenseAgreementActivityTest extends RobolectricTestWithConfig {

    private static final String PREF_KEY = "I_understand";

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    // --- Preference drives the checkbox ---

    /**
     * The checkbox follows the stored preference in both directions. Asserting both within one
     * test is deliberate: a "false leaves it unticked" assertion on its own would also pass if the
     * preference read were deleted entirely, since the underlying field defaults to false. Only the
     * contrast proves the value is actually read.
     */
    @Test
    public void checkboxFollowsStoredPreference() {
        // :: Setup
        storeUnderstood(true);

        // :: Act
        CheckBox checkBoxAfterTrue = checkBox(openActivity());

        // :: Verify
        assertThat(checkBoxAfterTrue.isChecked()).isTrue();

        // :: Setup
        storeUnderstood(false);

        // :: Act
        CheckBox checkBoxAfterFalse = checkBox(openActivity());

        // :: Verify
        assertThat(checkBoxAfterFalse.isChecked()).isFalse();
    }

    /** With no preference stored at all, the agreement starts unaccepted. */
    @Test
    public void checkboxIsUntickedWhenPreferenceUnset() {
        // :: Act
        LicenseAgreementActivity activity = openActivity();

        // :: Verify
        assertThat(checkBox(activity).isChecked()).isFalse();
    }

    // --- Save writes the checkbox back ---

    /** Ticking the checkbox and pressing Save stores true. */
    @Test
    public void saveStoresTrueWhenCheckboxTicked() {
        // :: Setup
        LicenseAgreementActivity activity = openActivity();
        checkBox(activity).setChecked(true);

        // :: Act
        saveButton(activity).performClick();

        // :: Verify
        assertThat(storedUnderstood()).isTrue();
    }

    /** Unticking the checkbox and pressing Save stores false. */
    @Test
    public void saveStoresFalseWhenCheckboxUnticked() {
        // :: Setup
        storeUnderstood(true);
        LicenseAgreementActivity activity = openActivity();
        checkBox(activity).setChecked(false);

        // :: Act
        saveButton(activity).performClick();

        // :: Verify
        assertThat(storedUnderstood()).isFalse();
    }

    // --- Helpers ---

    private LicenseAgreementActivity openActivity() {
        return Robolectric.buildActivity(LicenseAgreementActivity.class).create().get();
    }

    /**
     * onCreate wraps its whole view setup in a catch for UnsupportedOperationException and simply
     * calls finish(), which would leave the views absent and surface here as a confusing NPE.
     * Fail with a clear message instead, so the cause is not mistaken for a broken assertion.
     */
    private CheckBox checkBox(LicenseAgreementActivity activity) {
        CheckBox checkBox = activity.findViewById(R.id.agreeCheckBox);
        assertThat(checkBox).isNotNull(); // activity aborted its own setup; see onCreate's catch
        return checkBox;
    }

    private Button saveButton(LicenseAgreementActivity activity) {
        Button button = activity.findViewById(R.id.saveButton);
        assertThat(button).isNotNull(); // activity aborted its own setup; see onCreate's catch
        return button;
    }

    private void storeUnderstood(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean(PREF_KEY, value).commit();
    }

    private boolean storedUnderstood() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getBoolean(PREF_KEY, false);
    }
}
