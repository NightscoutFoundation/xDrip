package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.nocturne.NocturneUploader.TreatmentRoute;

import org.junit.Test;
import org.nightscoutfoundation.nocturne.model.BolusKind;
import org.nightscoutfoundation.nocturne.model.CreateBolusRequest;
import org.nightscoutfoundation.nocturne.model.CreateCarbIntakeRequest;
import org.nightscoutfoundation.nocturne.model.CreateMealRequest;
import org.nightscoutfoundation.nocturne.model.DeviceEventType;
import org.nightscoutfoundation.nocturne.model.GlucoseDirection;
import org.nightscoutfoundation.nocturne.model.UpsertCalibrationRequest;
import org.nightscoutfoundation.nocturne.model.UpsertDeviceEventRequest;
import org.nightscoutfoundation.nocturne.model.UpsertMeterGlucoseRequest;
import org.nightscoutfoundation.nocturne.model.UpsertNoteRequest;

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
    public void mapBloodTest_containsExpectedFields() {
        final UpsertMeterGlucoseRequest request = NocturneUploader.mapBloodTest(120.0, TEST_TIMESTAMP, "Contour Next");
        assertThat(request.getMgdl()).isEqualTo(120.0);
        assertThat(request.getDevice()).isEqualTo("Contour Next");
        assertThat(request.getApp()).isEqualTo("xDrip+");
        assertThat(request.getDataSource()).isEqualTo("xdrip");
        assertThat(request.getTimestamp()).isNotNull();
        assertThat(request.getTimestamp().toInstant().toEpochMilli()).isEqualTo(TEST_TIMESTAMP);
        assertThat(request.getUtcOffset()).isEqualTo(
                TimeZone.getDefault().getOffset(TEST_TIMESTAMP) / 60000);
    }

    @Test
    public void mapCalibration_containsExpectedFields() {
        final UpsertCalibrationRequest request = NocturneUploader.mapCalibration(TEST_TIMESTAMP, 1.05, 10.0, 1.0);
        assertThat(request.getSlope()).isEqualTo(1.05);
        assertThat(request.getIntercept()).isEqualTo(10.0);
        assertThat(request.getScale()).isEqualTo(1.0);
        assertThat(request.getApp()).isEqualTo("xDrip+");
        assertThat(request.getDataSource()).isEqualTo("xdrip");
        assertThat(request.getDevice()).isNotNull();
    }

    @Test
    public void mapBolus_containsExpectedFields() {
        final CreateBolusRequest request = NocturneUploader.mapBolus(TEST_TIMESTAMP, 5.5, "NovoRapid", "sync-123");
        assertThat(request.getInsulin()).isEqualTo(5.5);
        assertThat(request.getKind()).isEqualTo(BolusKind.MANUAL);
        assertThat(request.getInsulinType()).isEqualTo("NovoRapid");
        assertThat(request.getSyncIdentifier()).isEqualTo("sync-123");
        assertThat(request.getDataSource()).isEqualTo("xdrip");
    }

    @Test
    public void mapBolus_nullInsulinType_fieldAbsent() {
        final CreateBolusRequest request = NocturneUploader.mapBolus(TEST_TIMESTAMP, 3.0, null, "sync-456");
        assertThat(request.getInsulinType()).isNull();
        assertThat(request.getInsulin()).isEqualTo(3.0);
    }

    @Test
    public void mapCarbIntake_containsExpectedFields() {
        final CreateCarbIntakeRequest request = NocturneUploader.mapCarbIntake(TEST_TIMESTAMP, 45.0, "sync-789");
        assertThat(request.getCarbs()).isEqualTo(45.0);
        assertThat(request.getSyncIdentifier()).isEqualTo("sync-789");
        assertThat(request.getDataSource()).isEqualTo("xdrip");
    }

    @Test
    public void mapMeal_containsExpectedFields() {
        final CreateMealRequest request = NocturneUploader.mapMeal(TEST_TIMESTAMP, 5.0, 60.0, "sync-meal");
        assertThat(request.getInsulin()).isEqualTo(5.0);
        assertThat(request.getCarbs()).isEqualTo(60.0);
        assertThat(request.getSyncIdentifier()).isEqualTo("sync-meal");
    }

    @Test
    public void mapNote_containsExpectedFields() {
        final UpsertNoteRequest request = NocturneUploader.mapNote(TEST_TIMESTAMP, "Felt dizzy", "Note", "sync-note");
        assertThat(request.getText()).isEqualTo("Felt dizzy");
        assertThat(request.getEventType()).isEqualTo("Note");
        assertThat(request.getIsAnnouncement()).isFalse();
        assertThat(request.getSyncIdentifier()).isEqualTo("sync-note");
    }

    @Test
    public void mapDeviceEvent_containsExpectedFields() {
        final UpsertDeviceEventRequest request = NocturneUploader.mapDeviceEvent(TEST_TIMESTAMP, "Sensor Start", "New G7", "sync-dev");
        assertThat(request.getEventType()).isEqualTo(DeviceEventType.SENSOR_START);
        assertThat(request.getNotes()).isEqualTo("New G7");
        assertThat(request.getSyncIdentifier()).isEqualTo("sync-dev");
    }

    @Test
    public void mapDeviceEvent_nullNotes_fieldAbsent() {
        final UpsertDeviceEventRequest request = NocturneUploader.mapDeviceEvent(TEST_TIMESTAMP, "Site Change", null, "sync-dev2");
        assertThat(request.getNotes()).isNull();
        assertThat(request.getEventType()).isEqualTo(DeviceEventType.SITE_CHANGE);
    }

    // ---- Conversion helper tests ----

    @Test
    public void toOffsetDateTime_preservesInstant() {
        assertThat(NocturneUploader.toOffsetDateTime(TEST_TIMESTAMP).toInstant().toEpochMilli())
                .isEqualTo(TEST_TIMESTAMP);
    }

    @Test
    public void directionFromSlopeName_mapsKnownValues() {
        assertThat(NocturneUploader.directionFromSlopeName("DoubleUp")).isEqualTo(GlucoseDirection.DOUBLE_UP);
        assertThat(NocturneUploader.directionFromSlopeName("Flat")).isEqualTo(GlucoseDirection.FLAT);
        assertThat(NocturneUploader.directionFromSlopeName("FortyFiveDown")).isEqualTo(GlucoseDirection.FORTY_FIVE_DOWN);
    }

    @Test
    public void directionFromSlopeName_mapsSpacedLegacyNames() {
        assertThat(NocturneUploader.directionFromSlopeName("NOT COMPUTABLE")).isEqualTo(GlucoseDirection.NOT_COMPUTABLE);
        assertThat(NocturneUploader.directionFromSlopeName("NOT_COMPUTABLE")).isEqualTo(GlucoseDirection.NOT_COMPUTABLE);
        assertThat(NocturneUploader.directionFromSlopeName("OUT OF RANGE")).isEqualTo(GlucoseDirection.RATE_OUT_OF_RANGE);
    }

    @Test
    public void directionFromSlopeName_unknownOrEmpty_returnsNull() {
        assertThat(NocturneUploader.directionFromSlopeName("SomethingElse")).isNull();
        assertThat(NocturneUploader.directionFromSlopeName("")).isNull();
        assertThat(NocturneUploader.directionFromSlopeName(null)).isNull();
    }
}
