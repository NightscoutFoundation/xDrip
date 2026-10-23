package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link Telemetry#sendCaptureReport()}, which works out how many readings were captured over
 * the last day.
 * <p>
 * The report itself is no longer sent anywhere, so its log line is the only thing it produces.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class TelemetryCaptureReportTest extends RobolectricTestWithConfig {

    private static final String CAPTURE_LINE = "SEND CAPTURE EVENT PROCESS: ";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().clear().commit();
        BgReading.deleteALL();
        Sensor.deleteAll();
        JoH.clearRatelimit("capture-report");
        Pref.setBoolean("enable_crashlytics", true);
        Pref.setBoolean("enable_telemetry", true);
        ShadowLog.clear();
    }

    @After
    public void tearDown() {
        BgReading.deleteALL();
        Sensor.deleteAll();
        JoH.clearRatelimit("capture-report");
        Pref.removeItem("enable_crashlytics");
        Pref.removeItem("enable_telemetry");
    }

    // ===== Capture report ============================================================================================

    /** A sensor that has run for over a day with every reading captured is reported as 100 % captured. */
    @Test
    public void reportsCaptureOfASensorRunningOverADay() {
        // :: Setup
        Sensor.create(JoH.tsl() - 2 * Constants.DAY_IN_MS);
        seedReadings(288);

        // :: Act
        Telemetry.sendCaptureReport();

        // :: Verify
        final List<String> lines = telemetryLog();
        assertThat(lines).doesNotContain("Got exception sending Capture Report");
        assertThat(lines.stream().filter(l -> l.startsWith(CAPTURE_LINE)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(lines.stream().filter(l -> l.startsWith(CAPTURE_LINE)).findFirst().get())
                .endsWith(" Captured 100");
    }

    /** A day with too few readings captured gives no capture event, but the report still runs to the end. */
    @Test
    public void lowCaptureGivesNoCaptureEvent() {
        // :: Setup
        Sensor.create(JoH.tsl() - 2 * Constants.DAY_IN_MS);
        seedReadings(100);

        // :: Act
        Telemetry.sendCaptureReport();

        // :: Verify
        final List<String> lines = telemetryLog();
        assertThat(lines).containsNoneOf("No sensor active", "Sensor not running for more than 24 hours yet");
        assertThat(lines).contains("SEND EVENT DONE");
        assertThat(lines.stream().anyMatch(l -> l.startsWith(CAPTURE_LINE))).isFalse();
    }

    // =================================================================================================================

    /** Saves {@code count} readings of 150 mg/dL, five minutes apart, ending now. */
    private static void seedReadings(int count) {
        final long now = JoH.tsl();
        for (int i = 0; i < count; i++) {
            final BgReading bg = new BgReading();
            bg.timestamp = now - i * 5 * Constants.MINUTE_IN_MS;
            bg.calculated_value = 150;
            bg.save(); // plain save, so no sync tasks are queued behind the test
        }
    }

    private static List<String> telemetryLog() {
        return ShadowLog.getLogsForTag("Telemetry").stream().map(l -> l.msg).collect(Collectors.toList());
    }
}
