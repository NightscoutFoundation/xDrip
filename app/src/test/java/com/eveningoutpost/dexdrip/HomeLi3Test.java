package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowDialog;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for what the home screen shows and asks with Li3 as the data source, with GluPro,
 * the collector it follows, as the control.
 * <p>
 * Home caches follower mode in a static; every test resets it, so a follower left by another test never takes the
 * follower display path here. Settings go through {@link Pref}, whose cached store outlives each test's application.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class HomeLi3Test extends RobolectricTestWithConfig {

    private static final double READING_MGDL = 123;
    private static final String METER_MGDL = "105";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        Home.resetFollowerCacheForTests();
    }

    @After
    public void tearDown() {
        new Delete().from(BloodTest.class).execute();
        BgReading.deleteALL();
        Sensor.deleteAll();
        Pref.removeItem("warning_agreed_to");
        Pref.removeItem("I_understand");
        Pref.removeItem("dex_collection_method");
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        Home.resetFollowerCacheForTests();
        Home.get_follower();
    }

    // ===== Current value =============================================================================================

    /** A recent Li3 reading is shown as the current value. */
    @Test
    public void li3_showsCurrentValue() {
        // :: Setup
        runningWithRecentReading(DexCollectionType.Li3);

        // :: Act
        Home home = Robolectric.buildActivity(Home.class).setup().get();

        // :: Verify
        assertThat(currentValue(home)).startsWith("123");
    }

    /** GluPro still shows its current value the same way. */
    @Test
    public void gluPro_showsCurrentValue() {
        // :: Setup
        runningWithRecentReading(DexCollectionType.GluPro);

        // :: Act
        Home home = Robolectric.buildActivity(Home.class).setup().get();

        // :: Verify
        assertThat(currentValue(home)).startsWith("123");
    }

    // ===== Blood glucose meter =======================================================================================

    /** A reading from a Bluetooth meter does not ask to calibrate a Li3. */
    @Test
    public void li3_meterReading_noCalibrationPrompt() {
        // :: Setup
        runningWithRecentReading(DexCollectionType.Li3);

        // :: Act
        Robolectric.buildActivity(Home.class, meterIntent()).setup();

        // :: Verify
        assertThat(shownDialogTitles()).doesNotContain(meterPromptTitle());
    }

    /** GluPro still asks whether to use a meter reading for calibration. */
    @Test
    public void gluPro_meterReading_asksToCalibrate() {
        // :: Setup
        runningWithRecentReading(DexCollectionType.GluPro);

        // :: Act
        Robolectric.buildActivity(Home.class, meterIntent()).setup();

        // :: Verify
        assertThat(shownDialogTitles()).contains(meterPromptTitle());
    }

    // ===== Entered blood test ========================================================================================

    /** An entered blood test is stored with a Li3, without asking to calibrate. */
    @Test
    public void li3_enteredBloodTest_storedWithoutCalibrationPrompt() {
        // :: Setup
        runningWithRecentReading(DexCollectionType.Li3);

        // :: Act
        enterBloodTest();

        // :: Verify
        assertThat(BloodTest.last().mgdl).isEqualTo(105d);
        assertThat(shownDialogTitles()).doesNotContain(xdrip.gs(R.string.use_bg_for_calibration));
    }

    /** GluPro still asks whether to use an entered blood test for calibration. */
    @Test
    public void gluPro_enteredBloodTest_asksToCalibrate() {
        // :: Setup
        runningWithRecentReading(DexCollectionType.GluPro);

        // :: Act
        enterBloodTest();

        // :: Verify
        assertThat(BloodTest.last().mgdl).isEqualTo(105d);
        assertThat(shownDialogTitles()).contains(xdrip.gs(R.string.use_bg_for_calibration));
    }

    // ===== Helpers ===================================================================================================

    /** Writes through {@link Pref}, whose static store is the one Home and the collector type read. */
    private static void runningWithRecentReading(DexCollectionType collector) {
        Pref.setBoolean("warning_agreed_to", true);
        Pref.setBoolean("I_understand", true);
        DexCollectionType.setDexCollectionType(collector);
        Sensor.create(JoH.tsl() - Constants.HOUR_IN_MS);
        BgReading.bgReadingInsertFromGluPro(READING_MGDL, JoH.tsl() - Constants.MINUTE_IN_MS, "test");
    }

    private static Intent meterIntent() {
        return new Intent(RuntimeEnvironment.application, Home.class)
                .putExtra(Home.BLUETOOTH_METER_CALIBRATION, METER_MGDL)
                .putExtra(Home.BLUETOOTH_METER_CALIBRATION + "2", "0");
    }

    private static String meterPromptTitle() {
        return String.format(xdrip.gs(R.string.use_for_calibration), METER_MGDL);
    }

    /** Enters "blood 105" the way a spoken or typed treatment arrives, then taps approve. */
    private static void enterBloodTest() {
        Intent intent = new Intent(RuntimeEnvironment.application, Home.class)
                .putExtra(WatchUpdaterService.WEARABLE_VOICE_PAYLOAD, "blood " + METER_MGDL);
        Home home = Robolectric.buildActivity(Home.class, intent).setup().get();
        home.findViewById(R.id.approveTreatment).performClick();
    }

    private static String currentValue(Home home) {
        return ((TextView) home.findViewById(R.id.currentBgValueRealTime)).getText().toString();
    }

    private static List<String> shownDialogTitles() {
        List<String> titles = new ArrayList<>();
        for (Dialog dialog : ShadowDialog.getShownDialogs()) {
            if (dialog instanceof AlertDialog && dialog.isShowing()) {
                titles.add(String.valueOf(shadowOf((AlertDialog) dialog).getTitle()));
            }
        }
        return titles;
    }
}
