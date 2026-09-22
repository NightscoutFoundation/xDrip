package com.eveningoutpost.dexdrip;

import android.widget.CheckBox;
import android.widget.EditText;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link MissedReadingActivity}.
 * <p>
 * The activity is a settings form: onCreate loads the stored missed-reading alert configuration
 * into its views and onDestroy writes whatever the user left there back out. The round trip is the
 * contract, and it only holds if both ends address the same preference store.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class MissedReadingActivityTest extends RobolectricTestWithConfig {

    private static final String ENABLED_KEY = "bg_missed_alerts";
    private static final String ALL_DAY_KEY = "missed_readings_all_day";
    private static final String RERAISE_KEY = "bg_missed_alerts_enable_alerts_reraise";
    private static final String OVERRIDE_SILENT_KEY = "bg_missed_alerts_override_silent";
    private static final String MINUTES_KEY = "bg_missed_minutes";
    private static final String START_KEY = "missed_readings_start";
    private static final String END_KEY = "missed_readings_end";

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
    }

    // ===== Stored settings drive the form ========================================================

    /** Each of the four stored flags ticks its own checkbox when it is set. */
    @Test
    public void setFlagsTickTheirCheckboxes() {
        // :: Setup
        storeBoolean(ENABLED_KEY, true);
        storeBoolean(ALL_DAY_KEY, true);
        storeBoolean(RERAISE_KEY, true);
        storeBoolean(OVERRIDE_SILENT_KEY, true);

        // :: Act
        MissedReadingActivity activity = openActivity().get();

        // :: Verify
        assertThat(checkBox(activity, R.id.missed_reading_enable_alert).isChecked()).isTrue();
        assertThat(checkBox(activity, R.id.missed_reading_all_day).isChecked()).isTrue();
        assertThat(checkBox(activity, R.id.missed_reading_enable_alerts_reraise).isChecked()).isTrue();
        assertThat(checkBox(activity, R.id.bg_missed_alerts_override_silent).isChecked()).isTrue();
    }

    /**
     * The same four flags leave their checkboxes clear when they are stored false. Three of the four
     * default to false, but {@code missed_readings_all_day} defaults to <em>true</em>, so this case
     * pins the read on its own — it goes red the moment the activity stops consulting the store.
     */
    @Test
    public void clearedFlagsLeaveTheirCheckboxesUnticked() {
        // :: Setup
        storeBoolean(ENABLED_KEY, false);
        storeBoolean(ALL_DAY_KEY, false);
        storeBoolean(RERAISE_KEY, false);
        storeBoolean(OVERRIDE_SILENT_KEY, false);

        // :: Act
        MissedReadingActivity activity = openActivity().get();

        // :: Verify
        assertThat(checkBox(activity, R.id.missed_reading_enable_alert).isChecked()).isFalse();
        assertThat(checkBox(activity, R.id.missed_reading_all_day).isChecked()).isFalse();
        assertThat(checkBox(activity, R.id.missed_reading_enable_alerts_reraise).isChecked()).isFalse();
        assertThat(checkBox(activity, R.id.bg_missed_alerts_override_silent).isChecked()).isFalse();
    }

    /** The stored missed-reading threshold is what the form shows, rather than the "30" default. */
    @Test
    public void minutesFieldFollowsStoredValue() {
        // :: Setup
        storeString(MINUTES_KEY, "17");

        // :: Act
        MissedReadingActivity activity = openActivity().get();

        // :: Verify
        EditText minutes = activity.findViewById(R.id.missed_reading_bg_minutes);
        assertThat(minutes).isNotNull();
        assertThat(minutes.getText().toString()).isEqualTo("17");
    }

    // ===== Closing the form writes it back =======================================================

    /** Ticking the alert checkboxes and closing the screen stores them. */
    @Test
    public void closingStoresCheckboxState() {
        // :: Setup
        storeBoolean(ENABLED_KEY, false);
        storeBoolean(OVERRIDE_SILENT_KEY, false);
        ActivityController<MissedReadingActivity> controller = openActivity();
        checkBox(controller.get(), R.id.missed_reading_enable_alert).setChecked(true);
        checkBox(controller.get(), R.id.bg_missed_alerts_override_silent).setChecked(true);

        // :: Act
        controller.destroy();

        // :: Verify
        assertThat(storedBoolean(ENABLED_KEY)).isTrue();
        assertThat(storedBoolean(OVERRIDE_SILENT_KEY)).isTrue();
    }

    /** Unticking them and closing the screen clears them again. */
    @Test
    public void closingClearsUntickedCheckboxState() {
        // :: Setup
        storeBoolean(ENABLED_KEY, true);
        storeBoolean(OVERRIDE_SILENT_KEY, true);
        ActivityController<MissedReadingActivity> controller = openActivity();
        checkBox(controller.get(), R.id.missed_reading_enable_alert).setChecked(false);
        checkBox(controller.get(), R.id.bg_missed_alerts_override_silent).setChecked(false);

        // :: Act
        controller.destroy();

        // :: Verify
        assertThat(storedBoolean(ENABLED_KEY)).isFalse();
        assertThat(storedBoolean(OVERRIDE_SILENT_KEY)).isFalse();
    }

    /**
     * An edited threshold reaches the store as the string the user typed. Asserted against the
     * store rather than against a reopened form: reopening would read and write through production
     * on both ends and would stay green even if the two APIs addressed different files.
     */
    @Test
    public void closingStoresEditedMinutes() {
        // :: Setup
        ActivityController<MissedReadingActivity> controller = openActivity();
        EditText minutes = controller.get().findViewById(R.id.missed_reading_bg_minutes);
        assertThat(minutes).isNotNull();
        minutes.setText("42");

        // :: Act
        controller.destroy();

        // :: Verify
        assertThat(storedString(MINUTES_KEY)).isEqualTo("42");
    }

    /**
     * The alert window is stored as minutes-since-midnight, split into hour and minute for the
     * pickers on load and recombined on save. An untouched form must give the value back unchanged.
     */
    @Test
    public void alertWindowSurvivesAnUntouchedRoundTrip() {
        // :: Setup
        storeInt(START_KEY, 8 * 60 + 15);  // 08:15
        storeInt(END_KEY, 22 * 60 + 45);   // 22:45
        ActivityController<MissedReadingActivity> controller = openActivity();

        // :: Act
        controller.destroy();

        // :: Verify
        assertThat(storedInt(START_KEY)).isEqualTo(8 * 60 + 15);
        assertThat(storedInt(END_KEY)).isEqualTo(22 * 60 + 45);
    }

    // ===== Helpers ===============================================================================

    private ActivityController<MissedReadingActivity> openActivity() {
        return Robolectric.buildActivity(MissedReadingActivity.class).create();
    }

    private CheckBox checkBox(MissedReadingActivity activity, int id) {
        CheckBox checkBox = activity.findViewById(id);
        assertThat(checkBox).isNotNull();
        return checkBox;
    }

    private void storeBoolean(String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean(key, value).commit();
    }

    private void storeString(String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString(key, value).commit();
    }

    private void storeInt(String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putInt(key, value).commit();
    }

    private boolean storedBoolean(String key) {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getBoolean(key, false);
    }

    private String storedString(String key) {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getString(key, "");
    }

    private int storedInt(String key) {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getInt(key, -1);
    }
}
