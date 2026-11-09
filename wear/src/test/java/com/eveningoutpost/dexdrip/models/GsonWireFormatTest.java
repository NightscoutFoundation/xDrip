package com.eveningoutpost.dexdrip.models;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Pins the JSON that the watch's models write and read with gson, byte for byte.
 * <p>
 * The watch parses what the phone's {@code toS()} sends and sends its own {@code toS()} back, and
 * {@code saved_alerts} is read back after an update. A gson upgrade must not change a character of either.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class GsonWireFormatTest extends RobolectricTestWithConfig {

    /** A reading exactly as the phone's {@code BgReading.toS()} sends it, pinned in the app's own test. */
    private static final String PHONE_BG_READING =
            "{\"timestamp\":1700000000000,\"time_since_sensor_started\":0.0,\"raw_data\":150.0,"
                    + "\"filtered_data\":148.0,\"age_adjusted_raw_value\":149.0,\"calibration_flag\":false,"
                    + "\"calculated_value\":123.4,\"filtered_calculated_value\":0.0,\"calculated_value_slope\":0.5,"
                    + "\"a\":0.0,\"b\":0.0,\"c\":0.0,\"ra\":0.0,\"rb\":0.0,\"rc\":0.0,\"uuid\":\"bg-uuid\","
                    + "\"calibration_uuid\":\"cal-uuid\",\"sensor_uuid\":\"sensor-uuid\",\"ignoreForStats\":false,"
                    + "\"raw_calculated\":0.0,\"hide_slope\":false,\"noise\":\"1\",\"dg_mgdl\":0.0,"
                    + "\"dg_slope\":0.0,\"dg_delta_name\":\"Flat\",\"source_info\":\"test\"}";

    /** An alert exactly as the phone's {@code AlertType.toS()} sends it, including {@code force_speaker}. */
    private static final String PHONE_ALERT =
            "{\"name\":\"High\",\"active\":false,\"volume\":0,\"vibrate\":false,\"light\":false,"
                    + "\"override_silent_mode\":false,\"force_speaker\":false,\"predictive\":false,"
                    + "\"time_until_threshold_crossed\":0.0,\"above\":true,\"threshold\":180.0,\"all_day\":false,"
                    + "\"start_time_minutes\":0,\"end_time_minutes\":0,\"minutes_between\":0,\"default_snooze\":0,"
                    + "\"text\":\"High BG\",\"mp3_file\":\"content://alarm.mp3\",\"uuid\":\"alert-uuid\"}";

    /** An alert as the watch's own {@code AlertType.toS()} writes it. */
    private static final String WATCH_ALERT =
            "{\"name\":\"High\",\"active\":false,\"volume\":0,\"vibrate\":false,\"light\":false,"
                    + "\"override_silent_mode\":false,\"predictive\":false,"
                    + "\"time_until_threshold_crossed\":0.0,\"above\":true,\"threshold\":180.0,\"all_day\":false,"
                    + "\"start_time_minutes\":0,\"end_time_minutes\":0,\"minutes_between\":0,\"default_snooze\":0,"
                    + "\"text\":\"High BG\",\"mp3_file\":\"content://alarm.mp3\",\"uuid\":\"alert-uuid\"}";

    @After
    public void cleanup() {
        new Delete().from(AlertType.class).execute();
        prefs().edit().remove("saved_alerts").commit();
        Ob1G5StateMachine.injectQueueJson("[]");
    }

    // ===== BgReading =================================================================================================

    /** A plain reading serialises to the exact string the phone parses today. */
    @Test
    public void bgReading_toS_writesExposedFields() {
        // :: Setup
        final BgReading reading = reading();

        // :: Act
        final String json = reading.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"time_since_sensor_started\":0.0,\"raw_data\":150.0,"
                        + "\"filtered_data\":148.0,\"age_adjusted_raw_value\":149.0,\"calibration_flag\":false,"
                        + "\"calculated_value\":123.4,\"filtered_calculated_value\":0.0,\"calculated_value_slope\":0.5,"
                        + "\"a\":0.0,\"b\":0.0,\"c\":0.0,\"ra\":0.0,\"rb\":0.0,\"rc\":0.0,\"uuid\":\"bg-uuid\","
                        + "\"calibration_uuid\":\"cal-uuid\",\"sensor_uuid\":\"sensor-uuid\",\"ignoreForStats\":false,"
                        + "\"raw_calculated\":0.0,\"hide_slope\":false,\"noise\":\"1\",\"dg_mgdl\":0.0,"
                        + "\"dg_slope\":0.0}");
    }

    /** A NaN slope is written as the bare literal {@code NaN}, which the phone accepts. */
    @Test
    public void bgReading_toS_withNaNSlope_keepsNaNLiteral() {
        // :: Setup
        final BgReading reading = reading();
        reading.calculated_value_slope = Double.NaN;

        // :: Act
        final String json = reading.toS();

        // :: Verify
        assertThat(json).contains("\"calculated_value_slope\":NaN,");
    }

    /** A reading sent by the phone is read whole: writing it again gives back the exact string the phone sent. */
    @Test
    public void bgReading_fromJSON_readsWhatThePhoneSends() {
        // :: Act
        final BgReading parsed = BgReading.fromJSON(PHONE_BG_READING);

        // :: Verify
        assertThat(parsed).isNotNull();
        assertThat(parsed.timestamp).isEqualTo(1_700_000_000_000L);
        assertThat(parsed.calculated_value).isEqualTo(123.4);
        assertThat(parsed.calculated_value_slope).isEqualTo(0.5);
        assertThat(parsed.uuid).isEqualTo("bg-uuid");
        assertThat(parsed.noise).isEqualTo("1");
        assertThat(parsed.toS()).isEqualTo(PHONE_BG_READING);
    }

    /** A reading sent with a bare {@code NaN} slope is still accepted. */
    @Test
    public void bgReading_fromJSON_readsBareNaN() {
        // :: Setup
        final String json = PHONE_BG_READING.replace("\"calculated_value_slope\":0.5", "\"calculated_value_slope\":NaN");

        // :: Act
        final BgReading parsed = BgReading.fromJSON(json);

        // :: Verify
        assertThat(parsed).isNotNull();
        assertThat(parsed.calculated_value_slope).isNaN();
    }

    // ===== Calibration, Sensor, TransmitterData ======================================================================

    /** A calibration serialises to the exact string the phone writes for the same calibration. */
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
                "{\"timestamp\":1700000000000,\"sensor_age_at_time_of_estimation\":0.0,\"bg\":110.0,\"raw_value\":0.0,"
                        + "\"adjusted_raw_value\":0.0,\"sensor_confidence\":0.0,\"slope_confidence\":0.0,"
                        + "\"raw_timestamp\":0,\"slope\":1.05,\"intercept\":-12.5,\"distance_from_estimate\":0.0,"
                        + "\"estimate_raw_at_time_of_calibration\":0.0,\"estimate_bg_at_time_of_calibration\":0.0,"
                        + "\"uuid\":\"cal-uuid\",\"sensor_uuid\":\"sensor-uuid\",\"possible_bad\":true,"
                        + "\"check_in\":false,\"first_decay\":0.0,\"second_decay\":0.0,\"first_slope\":0.0,"
                        + "\"second_slope\":0.0,\"first_intercept\":0.0,\"second_intercept\":0.0,\"first_scale\":0.0,"
                        + "\"second_scale\":0.0}");
    }

    /** The watch's sensor has no exposed fields, so it serialises to an empty object. */
    @Test
    public void sensor_toS_writesEmptyObject() {
        // :: Setup
        final Sensor sensor = new Sensor();
        sensor.started_at = 1_700_000_000_000L;
        sensor.uuid = "sensor-uuid";

        // :: Act
        final String json = sensor.toS();

        // :: Verify
        assertThat(json).isEqualTo("{}");
    }

    /** Transmitter data serialises to the exact string the watch sends to the phone. */
    @Test
    public void transmitterData_toS_writesExposedFields() {
        // :: Setup
        final TransmitterData data = new TransmitterData();
        data.timestamp = 1_700_000_000_000L;
        data.raw_data = 150000;
        data.filtered_data = 148000;
        data.sensor_battery_level = 215;
        data.uuid = "tx-uuid";

        // :: Act
        final String json = data.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"raw_data\":150000.0,\"filtered_data\":148000.0,"
                        + "\"sensor_battery_level\":215,\"uuid\":\"tx-uuid\"}");
    }

    // ===== UserError, HeartRate, BloodTest ===========================================================================

    /** A log entry keeps gson's HTML escaping and writes its double timestamp in exponent form. */
    @Test
    public void userError_toS_escapesHtmlAndWritesDoubleTimestamp() {
        // :: Setup
        final UserError error = new UserError();
        error.severity = 2;
        error.shortError = "Tag";
        error.message = "a <b> & 'c'";
        error.timestamp = 1.7E12;

        // :: Act
        final String json = error.toS();

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"shortError\":\"Tag\",\"message\":\"a \\u003cb\\u003e \\u0026 \\u0027c\\u0027\",\"severity\":2,"
                        + "\"timestamp\":1.7E12}");
    }

    /** A heart rate serialises to the exact string the watch sends to the phone. */
    @Test
    public void heartRate_toS_writesExposedFields() {
        // :: Setup
        final HeartRate heartRate = new HeartRate();
        heartRate.timestamp = 1_700_000_000_000L;
        heartRate.bpm = 72;
        heartRate.accuracy = 3;

        // :: Act
        final String json = heartRate.toS();

        // :: Verify
        assertThat(json).isEqualTo("{\"timestamp\":1700000000000,\"bpm\":72,\"accuracy\":3}");
    }

    /** A blood test serialises to the exact string the watch sends, and reads back to the same values. */
    @Test
    public void bloodTest_toSAndFromJSON_roundTrip() {
        // :: Setup
        final BloodTest bloodTest = new BloodTest();
        bloodTest.timestamp = 1_700_000_000_000L;
        bloodTest.mgdl = 105.5;
        bloodTest.created_timestamp = 1_700_000_000_500L;
        bloodTest.source = "Manual";
        bloodTest.uuid = "bt-uuid";

        // :: Act
        final String json = bloodTest.toS();
        final BloodTest parsed = BloodTest.fromJSON(json);

        // :: Verify
        assertThat(json).isEqualTo(
                "{\"timestamp\":1700000000000,\"mgdl\":105.5,\"created_timestamp\":1700000000500,\"state\":0,"
                        + "\"source\":\"Manual\",\"uuid\":\"bt-uuid\"}");
        assertThat(parsed.mgdl).isEqualTo(105.5);
        assertThat(parsed.created_timestamp).isEqualTo(1_700_000_000_500L);
        assertThat(parsed.uuid).isEqualTo("bt-uuid");
    }

    // ===== Treatments ================================================================================================

    /**
     * A treatment exactly as the phone's {@code Treatments.toS()} sends it, pinned in the app's own test, is read with
     * every field the watch keeps. The phone-only {@code insulinJSON} is ignored.
     */
    @Test
    public void treatments_fromJSON_readsPhoneString() {
        // :: Setup
        final String phoneTreatment =
                "{\"timestamp\":1700000000000,\"eventType\":\"\\u003cnone\\u003e\",\"enteredBy\":\"xdrip\","
                        + "\"notes\":\"lunch\",\"uuid\":\"treatment-uuid\",\"carbs\":20.0,\"insulin\":2.5,"
                        + "\"insulinJSON\":\"[]\",\"created_at\":\"2023-11-14T22:13:20Z\"}";

        // :: Act
        final Treatments treatment = Treatments.fromJSON(phoneTreatment);

        // :: Verify
        assertThat(treatment).isNotNull();
        assertThat(treatment.timestamp).isEqualTo(1_700_000_000_000L);
        assertThat(treatment.eventType).isEqualTo("<none>");
        assertThat(treatment.enteredBy).isEqualTo("xdrip");
        assertThat(treatment.notes).isEqualTo("lunch");
        assertThat(treatment.uuid).isEqualTo("treatment-uuid");
        assertThat(treatment.carbs).isEqualTo(20.0);
        assertThat(treatment.insulin).isEqualTo(2.5);
        assertThat(treatment.created_at).isEqualTo("2023-11-14T22:13:20Z");
    }

    // ===== AlertType =================================================================================================

    /** An alert serialises to the exact string the watch writes today. */
    @Test
    public void alertType_toS_writesExposedFields() {
        // :: Setup
        final AlertType alert = alert();

        // :: Act
        final String json = alert.toS();

        // :: Verify
        assertThat(json).isEqualTo(WATCH_ALERT);
    }

    /** Saving alerts writes the exact {@code saved_alerts} string the next version reads back. */
    @Test
    public void toSettings_writesSavedAlertsString() {
        // :: Setup
        alert().save();

        // :: Act
        AlertType.toSettings(xdrip.getAppContext());

        // :: Verify
        assertThat(prefs().getString("saved_alerts", null)).isEqualTo("[" + WATCH_ALERT + "]");
    }

    /** A {@code saved_alerts} string in the phone's format restores the alert, ignoring the unknown field. */
    @Test
    public void fromSettings_readsAlertsInPhoneFormat() {
        // :: Setup
        prefs().edit().putString("saved_alerts", "[" + PHONE_ALERT + "]").commit();

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

    // ===== JoH =======================================================================================================

    /** A JSON object becomes a map where every number is a Double and nesting is kept. */
    @Test
    public void jsonStringToMap_readsNumbersAsDoubles() {
        // :: Act
        final HashMap<String, Object> map =
                JoH.JsonStringtoMap("{\"a\":1,\"b\":\"x\",\"c\":[1,2.5],\"d\":{\"e\":true},\"f\":null}");

        // :: Verify
        assertThat(map.get("a")).isEqualTo(1.0d);
        assertThat(map.get("b")).isEqualTo("x");
        assertThat(map.get("c")).isEqualTo(Arrays.asList(1.0d, 2.5d));
        assertThat((Map<?, ?>) map.get("d")).containsExactly("e", true);
        assertThat(map).containsKey("f");
        assertThat(map.get("f")).isNull();
    }

    // ===== Ob1G5 command queue =======================================================================================

    /**
     * A G5 command queue exactly as the phone sends it (and as the watch stores it across an update) is read into the
     * watch's queue with its command name. The phone-only {@code dontRetry} is ignored.
     */
    @Test
    public void ob1Queue_injectQueueJson_readsPhoneString() {
        // :: Setup
        final String phoneQueue = "[{\"msg\":{\"postExecuteGuardTime\":50,\"byteSequence\":[38,1,2]},"
                + "\"text\":\"Start Sensor\",\"timestamp\":1700000000000,\"dontRetry\":false}]";

        // :: Act
        Ob1G5StateMachine.injectQueueJson(phoneQueue);

        // :: Verify
        assertThat(Ob1G5StateMachine.queueSize()).isEqualTo(1);
        assertThat(Ob1G5StateMachine.getFirstQueueItemName()).isEqualTo("Start Sensor");
    }

    // ===== Helpers ===================================================================================================

    private static BgReading reading() {
        final BgReading reading = new BgReading();
        reading.timestamp = 1_700_000_000_000L;
        reading.calculated_value = 123.4;
        reading.calculated_value_slope = 0.5;
        reading.raw_data = 150.0;
        reading.filtered_data = 148.0;
        reading.age_adjusted_raw_value = 149.0;
        reading.noise = "1";
        reading.uuid = "bg-uuid";
        reading.calibration_uuid = "cal-uuid";
        reading.sensor_uuid = "sensor-uuid";
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
