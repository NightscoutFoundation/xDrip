package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.nocturne.NocturneUploader.TreatmentRoute;

import org.json.JSONObject;
import org.junit.Test;

import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for treatment routing and field mapping in {@link NocturneUploader}.
 */
public class NocturneUploaderTest extends RobolectricTestWithConfig {

    // ---- Task 11: Routing tests ----

    @Test
    public void routeTreatment_sensorStart_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Sensor Start";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_sensorStop_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Sensor Stop";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_sensorChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Sensor Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_siteChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Site Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_insulinChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Insulin Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_pumpBatteryChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Pump Battery Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_podChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Pod Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_reservoirChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Reservoir Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_cannulaChange_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Cannula Change";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_transmitterSensorInsert_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Transmitter Sensor Insert";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_insulinAndCarbs_routesToMeal() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 5.0;
        t.carbs = 30.0;
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.MEAL);
    }

    @Test
    public void routeTreatment_insulinOnly_routesToBolus() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 3.5;
        t.carbs = 0;
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.BOLUS);
    }

    @Test
    public void routeTreatment_carbsOnly_routesToCarbs() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 0;
        t.carbs = 45.0;
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.CARBS);
    }

    @Test
    public void routeTreatment_notesOnly_routesToNote() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 0;
        t.carbs = 0;
        t.notes = "Feeling low";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.NOTE);
    }

    @Test
    public void routeTreatment_unknownEventTypeWithNotes_routesToNote() {
        final Treatments t = new Treatments();
        t.eventType = "SomethingUnknown";
        t.insulin = 0;
        t.carbs = 0;
        t.notes = "Some observation";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.NOTE);
    }

    @Test
    public void routeTreatment_empty_routesToSkip() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 0;
        t.carbs = 0;
        t.notes = null;
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.SKIP);
    }

    @Test
    public void routeTreatment_emptyNotesString_routesToSkip() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 0;
        t.carbs = 0;
        t.notes = "";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.SKIP);
    }

    @Test
    public void routeTreatment_deviceEventWithNotes_routesToDeviceEvent() {
        final Treatments t = new Treatments();
        t.eventType = "Sensor Start";
        t.notes = "New sensor inserted";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.DEVICE_EVENT);
    }

    @Test
    public void routeTreatment_enteredByViaNightscout_routesToSkip() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 5.0;
        t.carbs = 30.0;
        t.enteredBy = "Loop via Nightscout";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.SKIP);
    }

    @Test
    public void routeTreatment_enteredByNightscoutLoader_routesToSkip() {
        final Treatments t = new Treatments();
        t.eventType = "<none>";
        t.insulin = 5.0;
        t.carbs = 30.0;
        t.enteredBy = "Nightscout Loader Plugin";
        assertThat(NocturneUploader.routeTreatment(t)).isEqualTo(TreatmentRoute.SKIP);
    }

    // ---- Task 13: Mapping method tests ----

    private static final long TEST_TIMESTAMP = 1700000000000L;

    @Test
    public void mapBloodTest_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapBloodTest(120.0, TEST_TIMESTAMP, "Contour Next");
        assertThat(obj.getDouble("mgdl")).isEqualTo(120.0);
        assertThat(obj.getString("device")).isEqualTo("Contour Next");
        assertThat(obj.getString("app")).isEqualTo("xDrip+");
        assertThat(obj.getString("dataSource")).isEqualTo("xdrip");
        assertThat(obj.getString("timestamp")).isNotEmpty();
        assertThat(obj.getInt("utcOffset")).isEqualTo(
                TimeZone.getDefault().getOffset(TEST_TIMESTAMP) / 60000);
    }

    @Test
    public void mapCalibration_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapCalibration(TEST_TIMESTAMP, 1.05, 10.0, 1.0);
        assertThat(obj.getDouble("slope")).isEqualTo(1.05);
        assertThat(obj.getDouble("intercept")).isEqualTo(10.0);
        assertThat(obj.getDouble("scale")).isEqualTo(1.0);
        assertThat(obj.getString("app")).isEqualTo("xDrip+");
        assertThat(obj.getString("dataSource")).isEqualTo("xdrip");
        assertThat(obj.has("device")).isTrue();
    }

    @Test
    public void mapBolus_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapBolus(TEST_TIMESTAMP, 5.5, "NovoRapid", "sync-123");
        assertThat(obj.getDouble("insulin")).isEqualTo(5.5);
        assertThat(obj.getString("kind")).isEqualTo("Manual");
        assertThat(obj.getString("insulinType")).isEqualTo("NovoRapid");
        assertThat(obj.getString("syncIdentifier")).isEqualTo("sync-123");
        assertThat(obj.getString("dataSource")).isEqualTo("xdrip");
    }

    @Test
    public void mapBolus_nullInsulinType_fieldAbsent() throws Exception {
        final JSONObject obj = NocturneUploader.mapBolus(TEST_TIMESTAMP, 3.0, null, "sync-456");
        assertThat(obj.has("insulinType")).isFalse();
        assertThat(obj.getDouble("insulin")).isEqualTo(3.0);
    }

    @Test
    public void mapCarbIntake_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapCarbIntake(TEST_TIMESTAMP, 45.0, "sync-789");
        assertThat(obj.getDouble("carbs")).isEqualTo(45.0);
        assertThat(obj.getString("syncIdentifier")).isEqualTo("sync-789");
        assertThat(obj.getString("dataSource")).isEqualTo("xdrip");
    }

    @Test
    public void mapMeal_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapMeal(TEST_TIMESTAMP, 5.0, 60.0, "sync-meal");
        assertThat(obj.getDouble("insulin")).isEqualTo(5.0);
        assertThat(obj.getDouble("carbs")).isEqualTo(60.0);
        assertThat(obj.getString("syncIdentifier")).isEqualTo("sync-meal");
    }

    @Test
    public void mapNote_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapNote(TEST_TIMESTAMP, "Felt dizzy", "Note", "sync-note");
        assertThat(obj.getString("text")).isEqualTo("Felt dizzy");
        assertThat(obj.getString("eventType")).isEqualTo("Note");
        assertThat(obj.getBoolean("isAnnouncement")).isFalse();
        assertThat(obj.getString("syncIdentifier")).isEqualTo("sync-note");
    }

    @Test
    public void mapDeviceEvent_containsExpectedFields() throws Exception {
        final JSONObject obj = NocturneUploader.mapDeviceEvent(TEST_TIMESTAMP, "Sensor Start", "New G7", "sync-dev");
        assertThat(obj.getString("eventType")).isEqualTo("SensorStart");
        assertThat(obj.getString("notes")).isEqualTo("New G7");
        assertThat(obj.getString("syncIdentifier")).isEqualTo("sync-dev");
    }

    @Test
    public void mapDeviceEvent_nullNotes_fieldAbsent() throws Exception {
        final JSONObject obj = NocturneUploader.mapDeviceEvent(TEST_TIMESTAMP, "Site Change", null, "sync-dev2");
        assertThat(obj.has("notes")).isFalse();
        assertThat(obj.getString("eventType")).isEqualTo("SiteChange");
    }
}
