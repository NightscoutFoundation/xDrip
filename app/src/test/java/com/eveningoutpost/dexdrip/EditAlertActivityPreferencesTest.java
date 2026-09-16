package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.widget.EditText;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.AlertType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the units preference {@link EditAlertActivity} reads when it opens.
 * <p>
 * The stored threshold is always mg/dL. The edit form converts it for display, so the number the
 * user is shown — and edits — depends entirely on the units preference read in onCreate.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class EditAlertActivityPreferencesTest extends RobolectricTestWithConfig {

    private static final String ALERT_UUID = "batch5-edit-alert";

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        AlertType.remove_all();
        AlertType.add_alert(ALERT_UUID, "batch5 low", false, 100, true, 1, null,
                0, 0, true, true, 20, true, true);
    }

    /**
     * The whole :app: suite shares one JVM and one ActiveAndroid database, so the seeded alert would
     * otherwise outlive this class and be visible to anything that reads the table afterwards.
     */
    @After
    public void tearDown() {
        AlertType.remove_all();
    }

    // ===== Units preference drives the shown threshold ===========================================

    /** A 100 mg/dL alert opens showing "100" when the units preference says mgdl. */
    @Test
    public void thresholdIsShownInMgdlWhenUnitsAreMgdl() {
        // :: Setup
        storeUnits("mgdl");

        // :: Act
        String shown = openFormAndReadThreshold();

        // :: Verify
        assertThat(shown).isEqualTo("100");
    }

    /**
     * The same alert opens showing its mmol equivalent when the units preference says mmol. This is
     * the half that proves the preference is read at all: mgdl is also the default, so the mgdl test
     * above would still pass if the read were deleted.
     */
    @Test
    public void thresholdIsShownInMmolWhenUnitsAreMmol() {
        // :: Setup
        storeUnits("mmol");

        // :: Act
        String shown = openFormAndReadThreshold();

        // :: Verify
        assertThat(shown).doesNotContain("100");
        // 100 mg/dL is 5.6 mmol/L; the decimal separator is locale-dependent
        assertThat(shown).containsMatch("5[.,]6");
    }

    /** With nothing stored the form falls back to mg/dL, the same default the rest of the app uses. */
    @Test
    public void thresholdFallsBackToMgdlWhenNothingIsStored() {
        // :: Act
        String shown = openFormAndReadThreshold();

        // :: Verify
        assertThat(shown).isEqualTo("100");
    }

    // ===== Helpers ===============================================================================

    private String openFormAndReadThreshold() {
        Intent intent = new Intent(xdrip.getAppContext(), EditAlertActivity.class)
                .putExtra("uuid", ALERT_UUID);
        EditAlertActivity activity = Robolectric.buildActivity(EditAlertActivity.class, intent).create().get();
        EditText threshold = activity.findViewById(R.id.edit_alert_threshold);
        assertThat(threshold).isNotNull();
        return threshold.getText().toString();
    }

    private void storeUnits(String units) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("units", units).commit();
    }
}
