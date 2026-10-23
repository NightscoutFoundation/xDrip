package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.nocturne.NocturneUploader.DeleteOutcome;
import com.eveningoutpost.dexdrip.nocturne.NocturneUploader.TreatmentRoute;

import org.junit.Test;
import org.nightscoutfoundation.nocturne.model.BolusKind;
import org.nightscoutfoundation.nocturne.model.CreateBasalInjectionRequest;
import org.nightscoutfoundation.nocturne.model.CreateBolusRequest;
import org.nightscoutfoundation.nocturne.model.CreateCarbIntakeRequest;
import org.nightscoutfoundation.nocturne.model.CreateMealRequest;
import org.nightscoutfoundation.nocturne.model.DeviceEventType;
import org.nightscoutfoundation.nocturne.model.GlucoseDirection;
import org.nightscoutfoundation.nocturne.model.UpsertCalibrationRequest;
import org.nightscoutfoundation.nocturne.model.UpsertDeviceEventRequest;
import org.nightscoutfoundation.nocturne.model.UpsertMeterGlucoseRequest;
import org.nightscoutfoundation.nocturne.model.UpsertNoteRequest;
import org.nightscoutfoundation.nocturne.model.UpsertSensorGlucoseRequest;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.List;
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
    public void mapBasalInjection_containsExpectedFields() {
        final CreateBasalInjectionRequest request = NocturneUploader.mapBasalInjection(TEST_TIMESTAMP, 12.0, "Tresiba", "sync-basal");
        assertThat(request.getUnits()).isEqualTo(12.0);
        assertThat(request.getSyncIdentifier()).isEqualTo("sync-basal");
        assertThat(request.getApp()).isEqualTo("xDrip+");
        assertThat(request.getDataSource()).isEqualTo("xdrip");
        assertThat(request.getTimestamp()).isNotNull();
        assertThat(request.getTimestamp().toInstant().toEpochMilli()).isEqualTo(TEST_TIMESTAMP);
        assertThat(request.getUtcOffset()).isEqualTo(
                TimeZone.getDefault().getOffset(TEST_TIMESTAMP) / 60000);
    }

    @Test
    public void mapBasalInjection_insulinName_keptInNotes() {
        // The basal injection API has no insulin type field, so the name lands in the notes
        final CreateBasalInjectionRequest request = NocturneUploader.mapBasalInjection(TEST_TIMESTAMP, 12.0, "Tresiba", "sync-basal");
        assertThat(request.getNotes()).isEqualTo("Tresiba");
    }

    @Test
    public void mapBasalInjection_unknownOrMissingInsulinName_notesAbsent() {
        assertThat(NocturneUploader.mapBasalInjection(TEST_TIMESTAMP, 12.0, null, "sync-b1").getNotes()).isNull();
        assertThat(NocturneUploader.mapBasalInjection(TEST_TIMESTAMP, 12.0, "", "sync-b2").getNotes()).isNull();
        assertThat(NocturneUploader.mapBasalInjection(TEST_TIMESTAMP, 12.0, "unknown", "sync-b3").getNotes()).isNull();
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

    // ---- upload() success aggregation ----

    /**
     * Stubs every stream so tests can drive {@link NocturneUploader#upload}
     * and pin which streams feed the return value.
     */
    private static class StubUploader extends NocturneUploader {
        boolean sgvResult = true;
        boolean calibrationsResult = true;
        boolean bloodTestsResult = true;
        boolean treatmentsResult = true;
        boolean deletesResult = true;
        boolean sgvCalled = false;
        boolean heartRatesCalled = false;

        StubUploader() {
            super(true);
        }

        @Override
        boolean uploadSgv(final List<BgReading> bgReadings) {
            sgvCalled = true;
            return sgvResult;
        }

        @Override
        boolean uploadCalibrations(final List<com.eveningoutpost.dexdrip.models.Calibration> calibrations) {
            return calibrationsResult;
        }

        @Override
        boolean uploadBloodTests(final List<BloodTest> bloodTests) {
            return bloodTestsResult;
        }

        @Override
        boolean uploadTreatments(final List<Treatments> treatments) {
            return treatmentsResult;
        }

        @Override
        boolean deleteTreatments(final List<String> uuids) {
            return deletesResult;
        }

        // Watermark-driven streams: production swallows their failures
        // internally, so a stub that does nothing models a failed run.
        @Override
        void uploadHeartRates() {
            heartRatesCalled = true;
        }

        @Override
        void uploadStepCounts() {
        }

        @Override
        void uploadDeviceStatus() {
        }

        @Override
        void uploadMotionTracking() {
        }
    }

    @Test
    public void upload_sgvUploadsByDefaultAndFailureReturnsFalse() {
        // No prefs set: SGV must be on by default and drive the return value
        final StubUploader uploader = new StubUploader();
        uploader.sgvResult = false;
        assertThat(uploader.upload(null, null, null, null, null)).isFalse();
        assertThat(uploader.sgvCalled).isTrue();
    }

    @Test
    public void upload_failingCalibrationStream_returnsFalse() {
        Pref.setBoolean("nocturne_upload_calibrations", true);
        final StubUploader uploader = new StubUploader();
        uploader.calibrationsResult = false;
        assertThat(uploader.upload(null, null, null, null, null)).isFalse();
    }

    @Test
    public void upload_failingBloodTestStream_returnsFalse() {
        Pref.setBoolean("nocturne_upload_bloodtests", true);
        final StubUploader uploader = new StubUploader();
        uploader.bloodTestsResult = false;
        assertThat(uploader.upload(null, null, null, null, null)).isFalse();
    }

    @Test
    public void upload_failingTreatmentStream_returnsFalse() {
        Pref.setBoolean("nocturne_upload_treatments", true);
        final StubUploader uploader = new StubUploader();
        uploader.treatmentsResult = false;
        assertThat(uploader.upload(null, null, null, null, null)).isFalse();
    }

    @Test
    public void upload_failingTreatmentDeletion_returnsFalse() {
        Pref.setBoolean("nocturne_upload_treatments", true);
        final StubUploader uploader = new StubUploader();
        uploader.deletesResult = false;
        assertThat(uploader.upload(null, null, null, null, null)).isFalse();
    }

    @Test
    public void upload_allQueueStreamsSucceeding_returnsTrue() {
        Pref.setBoolean("nocturne_upload_calibrations", true);
        Pref.setBoolean("nocturne_upload_bloodtests", true);
        Pref.setBoolean("nocturne_upload_treatments", true);
        final StubUploader uploader = new StubUploader();
        assertThat(uploader.upload(null, null, null, null, null)).isTrue();
    }

    @Test
    public void upload_watermarkStreamFailure_doesNotAffectReturnValue() {
        // Heart rate is watermark-driven: it retries via its own watermark and
        // must never block queue completion, even when its upload failed.
        Pref.setBoolean("nocturne_upload_heartrate", true);
        final StubUploader uploader = new StubUploader();
        assertThat(uploader.upload(null, null, null, null, null)).isTrue();
        assertThat(uploader.heartRatesCalled).isTrue();
    }

    @Test
    public void upload_notReady_returnsFalse() {
        assertThat(new NocturneUploader(false).upload(null, null, null, null, null)).isFalse();
    }

    // ---- mapBgReading scaling ----

    @Test
    public void mapBgReading_scalesTrendDeltaAndRawValues() {
        final BgReading reading = new BgReading();
        reading.timestamp = TEST_TIMESTAMP;
        reading.calculated_value = 120.5;
        reading.calculated_value_slope = 0.0005; // per ms
        reading.raw_data = 100;
        reading.age_adjusted_raw_value = 100;
        reading.filtered_data = 98;
        reading.noise = "3";

        final UpsertSensorGlucoseRequest request = NocturneUploader.mapBgReading(reading);

        assertThat(request.getMgdl()).isEqualTo(120.5);
        // calculated_value_slope is per ms; trendRate is per minute
        assertThat(request.getTrendRate()).isEqualTo(0.0005 * 60000);
        // delta is slope per 5 minutes
        assertThat(request.getDelta()).isEqualTo(0.0005 * 5 * 60000);
        // filtered/unfiltered are scaled by 1000 like the Nightscout uploader
        assertThat(request.getFiltered()).isEqualTo(98 * 1000.0);
        assertThat(request.getUnfiltered()).isEqualTo(100 * 1000.0);
        assertThat(request.getNoise()).isEqualTo(3);
        assertThat(request.getTimestamp().toInstant().toEpochMilli()).isEqualTo(TEST_TIMESTAMP);
        assertThat(request.getUtcOffset()).isEqualTo(
                TimeZone.getDefault().getOffset(TEST_TIMESTAMP) / 60000);
    }

    // ---- Sync identifier suffixing ----

    @Test
    public void suffixedSyncId_firstRecordKeepsBareUuid() {
        assertThat(NocturneUploader.suffixedSyncId("uuid-1234", 0)).isEqualTo("uuid-1234");
    }

    @Test
    public void suffixedSyncId_laterRecordsGetNumericSuffix() {
        assertThat(NocturneUploader.suffixedSyncId("uuid-1234", 1)).isEqualTo("uuid-1234-2");
        assertThat(NocturneUploader.suffixedSyncId("uuid-1234", 2)).isEqualTo("uuid-1234-3");
    }

    // ---- Delete outcome merging ----

    @Test
    public void deleteOutcome_errorWinsOverEverything() {
        assertThat(DeleteOutcome.ERROR.merge(DeleteOutcome.DELETED)).isEqualTo(DeleteOutcome.ERROR);
        assertThat(DeleteOutcome.DELETED.merge(DeleteOutcome.ERROR)).isEqualTo(DeleteOutcome.ERROR);
        assertThat(DeleteOutcome.NOT_FOUND.merge(DeleteOutcome.ERROR)).isEqualTo(DeleteOutcome.ERROR);
    }

    @Test
    public void deleteOutcome_deletedWinsOverNotFound() {
        assertThat(DeleteOutcome.DELETED.merge(DeleteOutcome.NOT_FOUND)).isEqualTo(DeleteOutcome.DELETED);
        assertThat(DeleteOutcome.NOT_FOUND.merge(DeleteOutcome.DELETED)).isEqualTo(DeleteOutcome.DELETED);
    }

    @Test
    public void deleteOutcome_notFoundOnlyWhenAllNotFound() {
        assertThat(DeleteOutcome.NOT_FOUND.merge(DeleteOutcome.NOT_FOUND)).isEqualTo(DeleteOutcome.NOT_FOUND);
    }
}
