package com.eveningoutpost.dexdrip.models;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Pins the JSON that xDrip's models write and read with gson, byte for byte.
 * <p>
 * {@code toS()} output goes to watches and followers that may run an older xDrip, and {@code saved_alerts} is
 * read back after an app upgrade. A gson upgrade must not change a character of either.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class GsonWireFormatTest extends RobolectricTestWithConfig {

    // ===== BgReading =================================================================================================

    /** A plain reading serialises to the exact string a follower parses today. */
    @Test
    public void bgReading_toS_writesExposedFields() {
        // :: Setup
        final BgReading reading = reading(123.4);

        // :: Act
        final String json = reading.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"time_since_sensor_started\":0.0,\"raw_d"
                        + "ata\":150.0,\"filtered_data\":148.0,\"age_adjusted_raw_value\":149.0,"
                        + "\"calibration_flag\":false,\"calculated_value\":123.4,\"filtered_calcu"
                        + "lated_value\":0.0,\"calculated_value_slope\":0.5,\"a\":0.0,\"b\":0.0,"
                        + "\"c\":0.0,\"ra\":0.0,\"rb\":0.0,\"rc\":0.0,\"uuid\":\"bg-uuid\",\"cali"
                        + "bration_uuid\":\"cal-uuid\",\"sensor_uuid\":\"sensor-uuid\",\"ignoreFo"
                        + "rStats\":false,\"raw_calculated\":0.0,\"hide_slope\":false,\"noise\":"
                        + "\"1\",\"dg_mgdl\":0.0,\"dg_slope\":0.0,\"dg_delta_name\":\"Flat\",\"so"
                        + "urce_info\":\"test\"}");
    }

    /** A NaN slope is written as the bare literal {@code NaN}, which older readers accept. */
    @Test
    public void bgReading_toS_withNaNSlope_keepsNaNLiteral() {
        // :: Setup
        final BgReading reading = reading(123.4);
        reading.calculated_value_slope = Double.NaN;

        // :: Act
        final String json = reading.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"time_since_sensor_started\":0.0,\"raw_d"
                        + "ata\":150.0,\"filtered_data\":148.0,\"age_adjusted_raw_value\":149.0,"
                        + "\"calibration_flag\":false,\"calculated_value\":123.4,\"filtered_calcu"
                        + "lated_value\":0.0,\"calculated_value_slope\":NaN,\"a\":0.0,\"b\":0.0,"
                        + "\"c\":0.0,\"ra\":0.0,\"rb\":0.0,\"rc\":0.0,\"uuid\":\"bg-uuid\",\"cali"
                        + "bration_uuid\":\"cal-uuid\",\"sensor_uuid\":\"sensor-uuid\",\"ignoreFo"
                        + "rStats\":false,\"raw_calculated\":0.0,\"hide_slope\":false,\"noise\":"
                        + "\"1\",\"dg_mgdl\":0.0,\"dg_slope\":0.0,\"dg_delta_name\":\"Flat\",\"so"
                        + "urce_info\":\"test\"}");
    }

    /** What {@code toS()} writes, {@code fromJSON} reads back to the same reading. */
    @Test
    public void bgReading_fromJSON_readsBackWhatToSWrote() {
        // :: Setup
        final String json = reading(123.4).toS();

        // :: Act
        final BgReading parsed = BgReading.fromJSON(json);

        // :: Verify
        assertThat(parsed.timestamp).isEqualTo(1_700_000_000_000L);
        assertThat(parsed.calculated_value).isEqualTo(123.4);
        assertThat(parsed.toS()).isEqualTo(json);
    }

    /** A reading sent with a bare {@code NaN} slope is still accepted by the reader. */
    @Test
    public void bgReading_fromJSON_readsBareNaN() {
        // :: Setup
        final BgReading sent = reading(123.4);
        sent.calculated_value_slope = Double.NaN;
        final String json = sent.toS();

        // :: Act
        final BgReading parsed = BgReading.fromJSON(json);

        // :: Verify
        assertThat(parsed).isNotNull();
        assertThat(parsed.calculated_value_slope).isNaN();
    }

    // ===== Calibration, Sensor, Treatments ===========================================================================

    /** A calibration serialises to the exact string sent to followers. */
    @Test
    public void calibration_toS_writesExposedFields() {
        // :: Setup
        final Calibration calibration = new Calibration();
        calibration.timestamp = 1_700_000_000_000L;
        calibration.bg = 110;
        calibration.slope = 1.05;
        calibration.intercept = -12.5;
        calibration.uuid = "cal-uuid";
        calibration.sensor_uuid = "sensor-uuid";
        calibration.possible_bad = true;

        // :: Act
        final String json = calibration.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"sensor_age_at_time_of_estimation\":0.0,"
                        + "\"bg\":110.0,\"raw_value\":0.0,\"adjusted_raw_value\":0.0,\"sensor_con"
                        + "fidence\":0.0,\"slope_confidence\":0.0,\"raw_timestamp\":0,\"slope\":1"
                        + ".05,\"intercept\":-12.5,\"distance_from_estimate\":0.0,\"estimate_raw_"
                        + "at_time_of_calibration\":0.0,\"estimate_bg_at_time_of_calibration\":0."
                        + "0,\"uuid\":\"cal-uuid\",\"sensor_uuid\":\"sensor-uuid\",\"possible_bad"
                        + "\":true,\"check_in\":false,\"first_decay\":0.0,\"second_decay\":0.0,\""
                        + "first_slope\":0.0,\"second_slope\":0.0,\"first_intercept\":0.0,\"secon"
                        + "d_intercept\":0.0,\"first_scale\":0.0,\"second_scale\":0.0}");
    }

    /** A sensor serialises to the exact string sent to followers. */
    @Test
    public void sensor_toS_writesExposedFields() {
        // :: Setup
        final Sensor sensor = new Sensor();
        sensor.started_at = 1_700_000_000_000L;
        sensor.uuid = "sensor-uuid";
        sensor.sensor_location = "arm";

        // :: Act
        final String json = sensor.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"started_at\":1700000000000,\"stopped_at\":0,\"latest_battery_level"
                        + "\":0,\"uuid\":\"sensor-uuid\",\"sensor_location\":\"arm\"}");
    }

    /** A treatment serialises to the exact string used for treatment sync, including the escaped {@code <none>}. */
    @Test
    public void treatments_toS_writesExposedFields() {
        // :: Setup
        final Treatments treatment = new Treatments();
        treatment.timestamp = 1_700_000_000_000L;
        treatment.carbs = 20;
        treatment.insulin = 2.5;
        treatment.uuid = "treatment-uuid";
        treatment.enteredBy = "xdrip";
        treatment.notes = "lunch";
        treatment.insulinJSON = "[]";
        treatment.created_at = "2023-11-14T22:13:20Z";

        // :: Act
        final String json = treatment.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"eventType\":\"\\u003cnone\\u003e\",\"en"
                        + "teredBy\":\"xdrip\",\"notes\":\"lunch\",\"uuid\":\"treatment-uuid\",\""
                        + "carbs\":20.0,\"insulin\":2.5,\"insulinJSON\":\"[]\",\"created_at\":\"2"
                        + "023-11-14T22:13:20Z\"}");
    }

    // ===== AlertType =================================================================================================

    /** An alert serialises to the exact string written into exported settings. */
    @Test
    public void alertType_toS_writesExposedFields() {
        // :: Setup
        final AlertType alert = alert();

        // :: Act
        final String json = alert.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"name\":\"High\",\"active\":false,\"volume\":0,\"vibrate\":false,\"l"
                        + "ight\":false,\"override_silent_mode\":false,\"force_speaker\":false,\""
                        + "predictive\":false,\"time_until_threshold_crossed\":0.0,\"above\":true"
                        + ",\"threshold\":180.0,\"all_day\":false,\"start_time_minutes\":0,\"end_"
                        + "time_minutes\":0,\"minutes_between\":0,\"default_snooze\":0,\"text\":"
                        + "\"High BG\",\"mp3_file\":\"content://alarm.mp3\",\"uuid\":\"alert-uuid"
                        + "\"}");
    }

    /** Saving alerts writes the exact {@code saved_alerts} string the next app version reads back. */
    @Test
    public void toSettings_writesSavedAlertsString() {
        // :: Setup
        alert().save();

        // :: Act
        AlertType.toSettings(xdrip.getAppContext());

        // :: Verify
        assertThat(prefs().getString("saved_alerts", null)).isEqualTo(
                "[{\"name\":\"High\",\"active\":false,\"volume\":0,\"vibrate\":false,\""
                        + "light\":false,\"override_silent_mode\":false,\"force_speaker\":false,"
                        + "\"predictive\":false,\"time_until_threshold_crossed\":0.0,\"above\":tr"
                        + "ue,\"threshold\":180.0,\"all_day\":false,\"start_time_minutes\":0,\"en"
                        + "d_time_minutes\":0,\"minutes_between\":0,\"default_snooze\":0,\"text\""
                        + ":\"High BG\",\"mp3_file\":\"content://alarm.mp3\",\"uuid\":\"alert-uui"
                        + "d\"}]");
    }

    /** A {@code saved_alerts} string written by the old version restores the same alert. */
    @Test
    public void fromSettings_readsAlertsWrittenByOldVersion() {
        // :: Setup
        prefs().edit().putString("saved_alerts",
                "[{\"name\":\"High\",\"active\":false,\"volume\":0,\"vibrate\":false,\""
                        + "light\":false,\"override_silent_mode\":false,\"force_speaker\":false,"
                        + "\"predictive\":false,\"time_until_threshold_crossed\":0.0,\"above\":tr"
                        + "ue,\"threshold\":180.0,\"all_day\":false,\"start_time_minutes\":0,\"en"
                        + "d_time_minutes\":0,\"minutes_between\":0,\"default_snooze\":0,\"text\""
                        + ":\"High BG\",\"mp3_file\":\"content://alarm.mp3\",\"uuid\":\"alert-uui"
                        + "d\"}]").commit();

        // :: Act
        AlertType.fromSettings(xdrip.getAppContext());

        // :: Verify
        final AlertType restored = AlertType.get_alert("alert-uuid");
        assertThat(restored).isNotNull();
        assertThat(restored.name).isEqualTo("High");
        assertThat(restored.threshold).isEqualTo(180.0);
        assertThat(restored.above).isTrue();
        assertThat(restored.mp3_file).isEqualTo("content://alarm.mp3");
    }

    // ===== Helpers ===================================================================================================

    private static BgReading reading(final double mgdl) {
        final BgReading reading = new BgReading();
        reading.timestamp = 1_700_000_000_000L;
        reading.calculated_value = mgdl;
        reading.calculated_value_slope = 0.5;
        reading.raw_data = 150.0;
        reading.filtered_data = 148.0;
        reading.age_adjusted_raw_value = 149.0;
        reading.noise = "1";
        reading.uuid = "bg-uuid";
        reading.calibration_uuid = "cal-uuid";
        reading.sensor_uuid = "sensor-uuid";
        reading.dg_delta_name = "Flat";
        reading.source_info = "test";
        return reading;
    }

    private static AlertType alert() {
        final AlertType alert = new AlertType();
        alert.name = "High";
        alert.above = true;
        alert.threshold = 180;
        alert.uuid = "alert-uuid";
        alert.text = "High BG";
        alert.mp3_file = "content://alarm.mp3";
        return alert;
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
