package com.eveningoutpost.dexdrip.nocturne;

import android.content.Context;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.InsulinInjection;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;

import org.nightscoutfoundation.nocturne.ApiClient;
import org.nightscoutfoundation.nocturne.ApiException;
import org.nightscoutfoundation.nocturne.api.ActivityApi;
import org.nightscoutfoundation.nocturne.api.BasalInjectionApi;
import org.nightscoutfoundation.nocturne.api.BolusApi;
import org.nightscoutfoundation.nocturne.api.CalibrationApi;
import org.nightscoutfoundation.nocturne.api.DeviceEventApi;
import org.nightscoutfoundation.nocturne.api.HeartRateApi;
import org.nightscoutfoundation.nocturne.api.MeterGlucoseApi;
import org.nightscoutfoundation.nocturne.api.NoteApi;
import org.nightscoutfoundation.nocturne.api.NutritionApi;
import org.nightscoutfoundation.nocturne.api.SensorGlucoseApi;
import org.nightscoutfoundation.nocturne.api.StepCountApi;
import org.nightscoutfoundation.nocturne.api.UploaderSnapshotApi;
import org.nightscoutfoundation.nocturne.model.BolusKind;
import org.nightscoutfoundation.nocturne.model.CreateBasalInjectionRequest;
import org.nightscoutfoundation.nocturne.model.CreateBolusRequest;
import org.nightscoutfoundation.nocturne.model.CreateCarbIntakeRequest;
import org.nightscoutfoundation.nocturne.model.CreateMealRequest;
import org.nightscoutfoundation.nocturne.model.DeviceEventType;
import org.nightscoutfoundation.nocturne.model.GlucoseDirection;
import org.nightscoutfoundation.nocturne.model.UpsertActivityRequest;
import org.nightscoutfoundation.nocturne.model.UpsertCalibrationRequest;
import org.nightscoutfoundation.nocturne.model.UpsertDeviceEventRequest;
import org.nightscoutfoundation.nocturne.model.UpsertHeartRateRequest;
import org.nightscoutfoundation.nocturne.model.UpsertMeterGlucoseRequest;
import org.nightscoutfoundation.nocturne.model.UpsertNoteRequest;
import org.nightscoutfoundation.nocturne.model.UpsertSensorGlucoseRequest;
import org.nightscoutfoundation.nocturne.model.UpsertStepCountRequest;
import org.nightscoutfoundation.nocturne.model.UpsertUploaderSnapshotRequest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Uploads xDrip+ data (SGV, treatments, heart rate, step count, ...) to a
 * Nocturne instance via the official nocturne-java SDK.
 * <p>
 * The SDK models use java.time (API 26+), which is safe without desugaring
 * because the app's minSdkVersion is 26.
 */
public class NocturneUploader {

    private static final String TAG = NocturneUploader.class.getSimpleName();
    private static final String DATA_SOURCE = "xdrip";
    private static final String HR_WATERMARK_KEY = "nocturne-heartrate-synced-time";
    private static final String STEPS_WATERMARK_KEY = "nocturne-steps-synced-time";
    private static final String MOTION_WATERMARK_KEY = "nocturne-motion-synced-time";

    private final boolean ready;
    private final ApiClient apiClient;

    public NocturneUploader(final Context context) {
        final NocturneOAuthService oauthService = new NocturneOAuthService();
        final String baseUrl = oauthService.getBaseUrl();
        final String accessToken = oauthService.getValidAccessToken();

        if (accessToken == null || baseUrl.isEmpty()) {
            if (accessToken == null) {
                UserError.Log.e(TAG, "No valid access token available");
            } else {
                UserError.Log.e(TAG, "No Nocturne instance URL configured");
            }
            apiClient = null;
            ready = false;
            return;
        }

        // SDK paths are absolute (/api/v4/...), so the base path is scheme://host only
        final String basePath = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        apiClient = new ApiClient(OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build())
                .setBasePath(basePath)
                // Cloudflare-fronted instances reject POSTs whose Origin looks cross-site
                .addDefaultHeader("Origin", basePath)
                // nocturne-java 0.2.4 registers no authentication scheme: setAccessToken() is an
                // unconditional throw and getAuthentications() is unmodifiable. The generated APIs
                // pass an empty authNames array, so nothing consults or overwrites this header.
                .addDefaultHeader("Authorization", "Bearer " + accessToken);
        ready = true;
    }

    /**
     * Test seam: skips preference and token wiring so tests can drive
     * {@link #upload} with the stream methods overridden.
     */
    NocturneUploader(final boolean ready) {
        this.ready = ready;
        this.apiClient = null;
    }

    /**
     * Main upload entry point called from UploaderTask.
     */
    public boolean upload(final List<BgReading> bgReadings,
                          final List<com.eveningoutpost.dexdrip.models.Calibration> calibrations,
                          final List<BloodTest> bloodTests,
                          final List<Treatments> treatmentsAdd,
                          final List<String> treatmentsDel) {
        if (!ready) return false;

        // The return value tells UploaderTask whether to mark the queue entries
        // complete, so it must reflect every queue-driven stream (SGV,
        // calibrations, blood tests, treatments) — otherwise a failed stream's
        // records would be dropped without retry. The watermark-driven streams
        // below (heart rate, steps, device status, motion) are not queue
        // entries; their failures retry via their own watermarks and must not
        // block queue completion.
        boolean queueSuccess = true;

        // SGV defaults on: glucose is the point of the uploader, so enabling
        // Nocturne in settings should start it flowing without a second toggle.
        // Everything else stays opt-in.
        if (Pref.getBoolean("nocturne_upload_sgv", true)) {
            queueSuccess &= uploadSgv(bgReadings);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_calibrations")) {
            queueSuccess &= uploadCalibrations(calibrations);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_bloodtests")) {
            queueSuccess &= uploadBloodTests(bloodTests);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_treatments")) {
            queueSuccess &= uploadTreatments(treatmentsAdd);
            queueSuccess &= deleteTreatments(treatmentsDel);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_heartrate")) {
            uploadHeartRates();
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_stepcount")) {
            uploadStepCounts();
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_devicestatus")) {
            uploadDeviceStatus();
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_motion")) {
            uploadMotionTracking();
        }

        return queueSuccess;
    }

    // --- Upload methods ---

    boolean uploadSgv(final List<BgReading> bgReadings) {
        if (bgReadings == null || bgReadings.isEmpty()) {
            return true;
        }
        final List<UpsertSensorGlucoseRequest> requests = new ArrayList<>(bgReadings.size());
        for (final BgReading reading : bgReadings) {
            requests.add(mapBgReading(reading));
        }
        try {
            new SensorGlucoseApi(apiClient).sensorGlucoseCreateSensorGlucoseBulk(requests);
            UserError.Log.d(TAG, "Uploaded " + requests.size() + " SGV readings");
            return true;
        } catch (Exception e) {
            logFailure("SGV bulk upload", e);
            return false;
        }
    }

    boolean uploadCalibrations(final List<com.eveningoutpost.dexdrip.models.Calibration> calibrations) {
        if (calibrations == null || calibrations.isEmpty()) return true;
        boolean allOk = true;
        final CalibrationApi api = new CalibrationApi(apiClient);
        for (final com.eveningoutpost.dexdrip.models.Calibration cal : calibrations) {
            if (cal.slope == 0) continue; // skip invalid calibrations
            try {
                api.calibrationCreate(mapCalibration(cal.timestamp, cal.slope, cal.intercept, cal.first_scale));
            } catch (Exception e) {
                logFailure("Calibration upload", e);
                allOk = false;
            }
        }
        return allOk;
    }

    boolean uploadBloodTests(final List<BloodTest> bloodTests) {
        if (bloodTests == null || bloodTests.isEmpty()) return true;
        boolean allOk = true;
        final MeterGlucoseApi api = new MeterGlucoseApi(apiClient);
        for (final BloodTest bt : bloodTests) {
            try {
                api.meterGlucoseCreate(mapBloodTest(bt.mgdl, bt.timestamp, bt.source));
            } catch (Exception e) {
                logFailure("Blood test upload", e);
                allOk = false;
            }
        }
        return allOk;
    }

    boolean uploadTreatments(final List<Treatments> treatments) {
        if (treatments == null || treatments.isEmpty()) return true;
        boolean allOk = true;

        final BolusApi bolusApi = new BolusApi(apiClient);
        final BasalInjectionApi basalInjectionApi = new BasalInjectionApi(apiClient);
        final NutritionApi nutritionApi = new NutritionApi(apiClient);
        final NoteApi noteApi = new NoteApi(apiClient);
        final DeviceEventApi deviceEventApi = new DeviceEventApi(apiClient);

        for (final Treatments t : treatments) {
            try {
                switch (routeTreatment(t)) {
                    case DEVICE_EVENT:
                        deviceEventApi.deviceEventCreate(mapDeviceEvent(t.timestamp, t.eventType, t.notes, t.uuid));
                        break;
                    case MEAL:
                        // Split out any long-acting component so basal doses aren't
                        // mis-filed as prandial meal insulin (e.g. a bedtime snack
                        // logged together with a Tresiba dose).
                        final List<InsulinInjection> mealInjections = t.getInsulinInjections();
                        double basalUnits = 0;
                        int mealBasalCount = 0;
                        if (mealInjections != null) {
                            for (final InsulinInjection inj : mealInjections) {
                                if (inj.getUnits() > 0 && isBasal(inj)) {
                                    basalInjectionApi.basalInjectionCreate(
                                            mapBasalInjection(t.timestamp, inj.getUnits(), inj.getInsulin(), suffixedSyncId(t.uuid, mealBasalCount++)));
                                    basalUnits += inj.getUnits();
                                }
                            }
                        }
                        final double mealInsulin = t.insulin - basalUnits;
                        if (mealInsulin > 0) {
                            nutritionApi.nutritionCreateMeal(mapMeal(t.timestamp, mealInsulin, t.carbs, t.uuid));
                        } else {
                            // All the insulin was basal — the carbs stand alone.
                            nutritionApi.nutritionCreateCarbIntake(mapCarbIntake(t.timestamp, t.carbs, t.uuid));
                        }
                        break;
                    case BOLUS:
                        final List<InsulinInjection> injections = t.getInsulinInjections();
                        if (injections != null && !injections.isEmpty()) {
                            int bolusCount = 0;
                            int basalCount = 0;
                            for (final InsulinInjection inj : injections) {
                                if (inj.getUnits() > 0) {
                                    // Long-acting doses belong to a separate Nocturne resource
                                    if (isBasal(inj)) {
                                        basalInjectionApi.basalInjectionCreate(
                                                mapBasalInjection(t.timestamp, inj.getUnits(), inj.getInsulin(), suffixedSyncId(t.uuid, basalCount++)));
                                    } else {
                                        bolusApi.bolusCreate(mapBolus(t.timestamp, inj.getUnits(), inj.getInsulin(), suffixedSyncId(t.uuid, bolusCount++)));
                                    }
                                }
                            }
                        } else {
                            // No per-injection detail, so nothing identifies the insulin
                            // profile: the treatment total can only be sent as a bolus.
                            bolusApi.bolusCreate(mapBolus(t.timestamp, t.insulin, null, t.uuid));
                        }
                        break;
                    case CARBS:
                        nutritionApi.nutritionCreateCarbIntake(mapCarbIntake(t.timestamp, t.carbs, t.uuid));
                        break;
                    case NOTE:
                        noteApi.noteCreate(mapNote(t.timestamp, t.notes, t.eventType, t.uuid));
                        break;
                    case SKIP:
                    default:
                        break;
                }
            } catch (Exception e) {
                logFailure("Treatment upload (" + t.uuid + ")", e);
                allOk = false;
            }
        }
        return allOk;
    }

    boolean deleteTreatments(final List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return true;
        boolean allOk = true;

        final BolusApi bolusApi = new BolusApi(apiClient);
        final NutritionApi nutritionApi = new NutritionApi(apiClient);
        final NoteApi noteApi = new NoteApi(apiClient);
        final DeviceEventApi deviceEventApi = new DeviceEventApi(apiClient);

        // A treatment's uuid may exist in any of these resources; try each and
        // treat 404 as "not stored there". Meals have no delete endpoint of
        // their own because the server stores a meal as a correlated bolus +
        // carb intake pair sharing the meal's sync identifier, so the bolus
        // and carb intake deletes below cover meal deletion too.
        // Basal injections cannot be deleted here: the SDK/server has no
        // basalInjectionDeleteBySyncIdentifier endpoint yet (pending upstream),
        // so deleting a basal-only treatment reports "not found" and completes.
        // When that endpoint lands, note that multi-record treatments upload
        // with suffixed sync identifiers (uuid, uuid-2, ... — see
        // suffixedSyncId), so deletion must match by prefix or correlation id,
        // not just the bare uuid tried below.
        for (final String uuid : uuids) {
            DeleteOutcome outcome = DeleteOutcome.NOT_FOUND;
            outcome = outcome.merge(deleteBySyncId(() -> bolusApi.bolusDeleteBySyncIdentifier(DATA_SOURCE, uuid), "bolus", uuid));
            outcome = outcome.merge(deleteBySyncId(() -> nutritionApi.nutritionDeleteCarbIntakeBySyncIdentifier(DATA_SOURCE, uuid), "carbs", uuid));
            outcome = outcome.merge(deleteBySyncId(() -> noteApi.noteDeleteBySyncIdentifier(DATA_SOURCE, uuid), "note", uuid));
            outcome = outcome.merge(deleteBySyncId(() -> deviceEventApi.deviceEventDeleteBySyncIdentifier(DATA_SOURCE, uuid), "device event", uuid));
            if (outcome == DeleteOutcome.NOT_FOUND) {
                // Legitimately absent (e.g. never uploaded) — done, don't retry.
                UserError.Log.d(TAG, "Treatment " + uuid + " not found in any Nocturne resource for deletion");
            } else if (outcome == DeleteOutcome.ERROR) {
                allOk = false;
            }
        }
        return allOk;
    }

    private interface DeleteCall {
        void run() throws ApiException;
    }

    enum DeleteOutcome {
        DELETED, NOT_FOUND, ERROR;

        // ERROR wins over anything so the queue entry is retried; otherwise
        // one successful delete is enough to consider the uuid handled.
        DeleteOutcome merge(final DeleteOutcome other) {
            if (this == ERROR || other == ERROR) return ERROR;
            if (this == DELETED || other == DELETED) return DELETED;
            return NOT_FOUND;
        }
    }

    private DeleteOutcome deleteBySyncId(final DeleteCall call, final String what, final String uuid) {
        try {
            call.run();
            return DeleteOutcome.DELETED;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return DeleteOutcome.NOT_FOUND;
            }
            UserError.Log.e(TAG, "Error deleting " + what + " " + uuid + ": HTTP " + e.getCode() + " body=" + e.getResponseBody());
            return DeleteOutcome.ERROR;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error deleting " + what + " " + uuid + ": " + e);
            return DeleteOutcome.ERROR;
        }
    }

    void uploadHeartRates() {
        final long watermark = PersistentStore.getLong(HR_WATERMARK_KEY);
        final List<HeartRate> readings = HeartRate.latestForGraph(1000, watermark, JoH.tsl());
        if (readings == null || readings.isEmpty()) return;

        final List<UpsertHeartRateRequest> requests = new ArrayList<>(readings.size());
        for (final HeartRate hr : readings) {
            requests.add(mapHeartRate(hr));
        }
        try {
            new HeartRateApi(apiClient).heartRateCreateHeartRates(requests);
            PersistentStore.setLong(HR_WATERMARK_KEY, readings.get(readings.size() - 1).timestamp + 1);
            UserError.Log.d(TAG, "Uploaded " + requests.size() + " heart rate readings");
        } catch (Exception e) {
            logFailure("Heart rate upload", e);
        }
    }

    void uploadStepCounts() {
        final long watermark = PersistentStore.getLong(STEPS_WATERMARK_KEY);
        final List<StepCounter> readings = StepCounter.latestForGraph(1000, watermark, JoH.tsl());
        if (readings == null || readings.isEmpty()) return;

        final List<UpsertStepCountRequest> requests = new ArrayList<>(readings.size());
        for (final StepCounter step : readings) {
            requests.add(mapStepCount(step));
        }
        try {
            new StepCountApi(apiClient).stepCountCreateStepCounts(requests);
            PersistentStore.setLong(STEPS_WATERMARK_KEY, readings.get(readings.size() - 1).timestamp + 1);
            UserError.Log.d(TAG, "Uploaded " + requests.size() + " step count readings");
        } catch (Exception e) {
            logFailure("Step count upload", e);
        }
    }

    void uploadDeviceStatus() {
        try {
            final long now = JoH.tsl();
            final UpsertUploaderSnapshotRequest snapshot = new UpsertUploaderSnapshotRequest()
                    .timestamp(toOffsetDateTime(now))
                    .utcOffset(utcOffsetMinutes(now))
                    .device("xDrip+")
                    .app("xDrip+")
                    .dataSource(DATA_SOURCE)
                    .battery(PowerStateReceiver.getBatteryLevel())
                    .isCharging(PowerStateReceiver.is_power_connected())
                    .type("phone");
            new UploaderSnapshotApi(apiClient)
                    .uploaderSnapshotCreateUploaderSnapshots(Collections.singletonList(snapshot));
        } catch (Exception e) {
            logFailure("Device status upload", e);
        }
    }

    void uploadMotionTracking() {
        try {
            final long watermark = Math.max(PersistentStore.getLong(MOTION_WATERMARK_KEY),
                    JoH.tsl() - Constants.DAY_IN_MS * 7);
            final ArrayList<ActivityRecognizedService.motionData> readings =
                    ActivityRecognizedService.getForGraph(watermark, JoH.tsl());
            if (readings == null || readings.isEmpty()) return;

            final List<UpsertActivityRequest> requests = new ArrayList<>(readings.size());
            long highestTimestamp = 0;
            for (final ActivityRecognizedService.motionData reading : readings) {
                requests.add(new UpsertActivityRequest()
                        .mills(reading.timestamp)
                        .utcOffset(utcOffsetMinutes(reading.timestamp))
                        .type("motion-class")
                        .description(reading.toPrettyType())
                        .enteredBy("xDrip+"));
                highestTimestamp = Math.max(highestTimestamp, reading.timestamp);
            }
            new ActivityApi(apiClient).activityCreateActivities(requests);
            PersistentStore.setLong(MOTION_WATERMARK_KEY, highestTimestamp + 1);
            UserError.Log.d(TAG, "Uploaded " + requests.size() + " motion tracking readings");
        } catch (Exception e) {
            logFailure("Motion tracking upload", e);
        }
    }

    private static void logFailure(final String what, final Exception e) {
        if (e instanceof ApiException) {
            final ApiException ae = (ApiException) e;
            UserError.Log.e(TAG, what + " failed: HTTP " + ae.getCode() + " body=" + ae.getResponseBody());
        } else {
            UserError.Log.e(TAG, what + " failed: " + e);
        }
    }

    // --- Mapping methods (static for testability) ---

    static UpsertSensorGlucoseRequest mapBgReading(final BgReading reading) {
        return new UpsertSensorGlucoseRequest()
                .timestamp(toOffsetDateTime(reading.timestamp))
                .utcOffset(utcOffsetMinutes(reading.timestamp))
                .mgdl(reading.calculated_value)
                .device("xDrip-" + DexCollectionType.getDexCollectionType())
                .app("xDrip+")
                .dataSource(Pref.getString("dex_collection_method", "unknown"))
                .direction(directionFromSlopeName(reading.slopeName()))
                // calculated_value_slope is per ms; convert to per minute
                .trendRate(reading.calculated_value_slope * 60000)
                // delta = slope per 5 minutes
                .delta(reading.calculated_value_slope * 5 * 60000)
                .noise(reading.noiseValue())
                .filtered(reading.ageAdjustedFiltered() * 1000)
                .unfiltered(reading.usedRaw() * 1000);
    }

    static UpsertCalibrationRequest mapCalibration(final long timestamp, final double slope, final double intercept, final double scale) {
        return new UpsertCalibrationRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .slope(slope)
                .intercept(intercept)
                .scale(scale)
                .device("xDrip-" + DexCollectionType.getDexCollectionType())
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    static UpsertMeterGlucoseRequest mapBloodTest(final double mgdl, final long timestamp, final String source) {
        return new UpsertMeterGlucoseRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .mgdl(mgdl)
                .device(source)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    static CreateBolusRequest mapBolus(final long timestamp, final double insulin, final String insulinType, final String syncIdentifier) {
        final CreateBolusRequest request = new CreateBolusRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .insulin(insulin)
                .kind(BolusKind.MANUAL)
                .syncIdentifier(syncIdentifier)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
        if (insulinType != null) {
            request.insulinType(insulinType);
        }
        return request;
    }

    static CreateBasalInjectionRequest mapBasalInjection(final long timestamp, final double units, final String insulinName, final String syncIdentifier) {
        final CreateBasalInjectionRequest request = new CreateBasalInjectionRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .units(units)
                .syncIdentifier(syncIdentifier)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
        // The API has no insulin type field, so keep the name (e.g. "Tresiba") in the notes
        if (insulinName != null && !insulinName.isEmpty() && !insulinName.equals("unknown")) {
            request.notes(insulinName);
        }
        return request;
    }

    static CreateCarbIntakeRequest mapCarbIntake(final long timestamp, final double carbs, final String syncIdentifier) {
        return new CreateCarbIntakeRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .carbs(carbs)
                .syncIdentifier(syncIdentifier)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    static CreateMealRequest mapMeal(final long timestamp, final double insulin, final double carbs, final String syncIdentifier) {
        return new CreateMealRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .insulin(insulin)
                .carbs(carbs)
                .syncIdentifier(syncIdentifier)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    static UpsertNoteRequest mapNote(final long timestamp, final String text, final String eventType, final String syncIdentifier) {
        return new UpsertNoteRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .text(text)
                .eventType(eventType)
                .isAnnouncement(false)
                .syncIdentifier(syncIdentifier)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    static UpsertDeviceEventRequest mapDeviceEvent(final long timestamp, final String xdripEventType, final String notes, final String syncIdentifier) {
        final UpsertDeviceEventRequest request = new UpsertDeviceEventRequest()
                .timestamp(toOffsetDateTime(timestamp))
                .utcOffset(utcOffsetMinutes(timestamp))
                .eventType(DEVICE_EVENT_TYPE_MAP.get(xdripEventType))
                .syncIdentifier(syncIdentifier)
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
        if (notes != null && !notes.isEmpty()) {
            request.notes(notes);
        }
        return request;
    }

    static UpsertHeartRateRequest mapHeartRate(final HeartRate hr) {
        return new UpsertHeartRateRequest()
                .timestamp(toOffsetDateTime(hr.timestamp))
                .utcOffset(utcOffsetMinutes(hr.timestamp))
                .bpm(hr.bpm)
                .accuracy(hr.accuracy)
                .device("xDrip+")
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    static UpsertStepCountRequest mapStepCount(final StepCounter step) {
        return new UpsertStepCountRequest()
                .timestamp(toOffsetDateTime(step.timestamp))
                .utcOffset(utcOffsetMinutes(step.timestamp))
                .metric(step.metric)
                .source(step.source)
                .device("xDrip+")
                .app("xDrip+")
                .dataSource(DATA_SOURCE);
    }

    // --- Conversion helpers ---

    static OffsetDateTime toOffsetDateTime(final long epochMillis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    static int utcOffsetMinutes(final long epochMillis) {
        return TimeZone.getDefault().getOffset(epochMillis) / 60000;
    }

    /**
     * Maps xDrip trend names ("DoubleUp", "NOT COMPUTABLE", "OUT OF RANGE", ...)
     * to the SDK's GlucoseDirection enum; returns null when the name has no
     * counterpart so the field is simply omitted.
     */
    static GlucoseDirection directionFromSlopeName(final String slopeName) {
        if (slopeName == null || slopeName.isEmpty()) return null;
        final String normalized = slopeName.replace(" ", "").replace("_", "");
        if (normalized.equalsIgnoreCase("OUTOFRANGE")) return GlucoseDirection.RATE_OUT_OF_RANGE;
        for (final GlucoseDirection direction : GlucoseDirection.values()) {
            if (direction.getValue().equalsIgnoreCase(normalized)) {
                return direction;
            }
        }
        return null;
    }

    // --- Treatment routing ---

    enum TreatmentRoute { DEVICE_EVENT, MEAL, BOLUS, CARBS, NOTE, SKIP }

    static final Set<String> DEVICE_EVENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Sensor Start", "Sensor Change", "Sensor Stop",
            "Site Change", "Insulin Change", "Pump Battery Change",
            "Pod Change", "Reservoir Change", "Cannula Change",
            "Transmitter Sensor Insert"
    )));

    static final Map<String, DeviceEventType> DEVICE_EVENT_TYPE_MAP;
    static {
        final Map<String, DeviceEventType> m = new HashMap<>();
        m.put("Sensor Start", DeviceEventType.SENSOR_START);
        m.put("Sensor Change", DeviceEventType.SENSOR_CHANGE);
        m.put("Sensor Stop", DeviceEventType.SENSOR_STOP);
        m.put("Site Change", DeviceEventType.SITE_CHANGE);
        m.put("Insulin Change", DeviceEventType.INSULIN_CHANGE);
        m.put("Pump Battery Change", DeviceEventType.PUMP_BATTERY_CHANGE);
        m.put("Pod Change", DeviceEventType.POD_CHANGE);
        m.put("Reservoir Change", DeviceEventType.RESERVOIR_CHANGE);
        m.put("Cannula Change", DeviceEventType.CANNULA_CHANGE);
        m.put("Transmitter Sensor Insert", DeviceEventType.TRANSMITTER_SENSOR_INSERT);
        DEVICE_EVENT_TYPE_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * Whether an injection is long-acting, per xDrip's insulin profile heuristic.
     * An unrecognised insulin name has no profile, in which case we keep the
     * historic behaviour and treat the dose as a bolus.
     */
    private static boolean isBasal(final InsulinInjection injection) {
        return injection.getProfile() != null && injection.isBasal();
    }

    /**
     * Sync identifiers must be unique within a resource, so when one treatment
     * produces several records of the same kind (e.g. two rapid-acting
     * injections) the second and later ones get a numeric suffix. The first
     * keeps the bare uuid so by-sync-identifier deletion still finds it;
     * suffixed extras are a documented deletion gap alongside basal.
     */
    static String suffixedSyncId(final String uuid, final int index) {
        return index == 0 ? uuid : uuid + "-" + (index + 1);
    }

    static TreatmentRoute routeTreatment(final Treatments t) {
        // Loop prevention: skip treatments that originated from Nightscout to
        // avoid round-trip cycles (xDrip -> Nocturne -> Nightscout -> xDrip)
        if (t.enteredBy != null
                && (t.enteredBy.contains("via Nightscout") || t.enteredBy.contains("Nightscout Loader"))) {
            return TreatmentRoute.SKIP;
        }

        if (t.eventType != null && DEVICE_EVENT_TYPES.contains(t.eventType)) {
            return TreatmentRoute.DEVICE_EVENT;
        }

        if (t.insulin > 0 && t.carbs > 0) {
            return TreatmentRoute.MEAL;
        }
        if (t.insulin > 0) {
            return TreatmentRoute.BOLUS;
        }
        if (t.carbs > 0) {
            return TreatmentRoute.CARBS;
        }
        if (t.notes != null && !t.notes.isEmpty()) {
            return TreatmentRoute.NOTE;
        }

        return TreatmentRoute.SKIP;
    }
}
