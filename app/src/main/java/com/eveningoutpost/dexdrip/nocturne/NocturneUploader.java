package com.eveningoutpost.dexdrip.nocturne;

import android.content.Context;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Uploads xDrip+ data (SGV, heart rate, step count) to a Nocturne instance
 * using direct HTTP calls to the v4 API.
 */
public class NocturneUploader {

    private static final String TAG = "NocturneUploader";
    private static final String HR_WATERMARK_KEY = "nocturne-heartrate-synced-time";
    private static final String STEPS_WATERMARK_KEY = "nocturne-steps-synced-time";
    private static final String MOTION_WATERMARK_KEY = "nocturne-motion-synced-time";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String token;
    private final boolean ready;

    public NocturneUploader(final Context context) {
        final String url = Pref.getString("nocturne_instance_url", "").trim();
        final NocturneOAuthService oauthService = new NocturneOAuthService();
        final String accessToken = oauthService.getValidAccessToken();

        if (accessToken == null || url.isEmpty()) {
            if (accessToken == null) {
                UserError.Log.e(TAG, "No valid access token available");
            }
            if (url.isEmpty()) {
                UserError.Log.e(TAG, "No Nocturne instance URL configured");
            }
            httpClient = null;
            baseUrl = null;
            token = null;
            ready = false;
            return;
        }

        httpClient = OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        baseUrl = url.endsWith("/") ? url : url + "/";
        token = accessToken;
        ready = true;
    }

    /**
     * Main upload entry point called from UploaderTask.
     */
    public boolean upload(final List<BgReading> bgReadings,
                          final List<Calibration> calibrations,
                          final List<BloodTest> bloodTests,
                          final List<Treatments> treatmentsAdd,
                          final List<String> treatmentsDel) {
        if (!ready) return false;

        boolean sgvSuccess = true;

        if (Pref.getBooleanDefaultFalse("nocturne_upload_sgv")) {
            sgvSuccess = uploadSgv(bgReadings);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_calibrations")) {
            uploadCalibrations(calibrations);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_bloodtests")) {
            uploadBloodTests(bloodTests);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_treatments")) {
            uploadTreatments(treatmentsAdd);
            deleteTreatments(treatmentsDel);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_heartrate")) {
            uploadHeartRates();
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_stepcount")) {
            uploadStepCounts();
        }

        if (Pref.getBoolean("nocturne_upload_devicestatus", true)) {
            uploadDeviceStatus();
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_motion")) {
            uploadMotionTracking();
        }

        return sgvSuccess;
    }

    private boolean uploadSgv(final List<BgReading> bgReadings) {
        if (bgReadings == null || bgReadings.isEmpty()) {
            return true;
        }

        final JSONArray array = new JSONArray();
        for (final BgReading reading : bgReadings) {
            array.put(mapBgReading(reading));
        }

        try {
            final int code = post("api/v4/glucose/sensor/bulk", array.toString());
            if (code >= 200 && code < 300) {
                UserError.Log.d(TAG, "Uploaded " + bgReadings.size() + " SGV readings");
                return true;
            } else {
                UserError.Log.e(TAG, "SGV bulk upload failed: HTTP " + code);
                return false;
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "SGV bulk upload error: " + e.getMessage());
            return false;
        }
    }

    private void uploadHeartRates() {
        final long watermark = PersistentStore.getLong(HR_WATERMARK_KEY);
        final List<HeartRate> readings = HeartRate.latestForGraph(1000, watermark, JoH.tsl());

        if (readings == null || readings.isEmpty()) {
            return;
        }

        final JSONArray array = new JSONArray();
        for (final HeartRate hr : readings) {
            array.put(mapHeartRate(hr));
        }

        try {
            final int code = post("api/v4/HeartRate", array.toString());
            if (code >= 200 && code < 300) {
                final long latestTimestamp = readings.get(readings.size() - 1).timestamp;
                PersistentStore.setLong(HR_WATERMARK_KEY, latestTimestamp + 1);
                UserError.Log.d(TAG, "Uploaded " + readings.size() + " heart rate readings");
            } else {
                UserError.Log.e(TAG, "Heart rate upload failed: HTTP " + code);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Heart rate upload error: " + e.getMessage());
        }
    }

    private void uploadStepCounts() {
        final long watermark = PersistentStore.getLong(STEPS_WATERMARK_KEY);
        final List<StepCounter> readings = StepCounter.latestForGraph(1000, watermark, JoH.tsl());

        if (readings == null || readings.isEmpty()) {
            return;
        }

        final JSONArray array = new JSONArray();
        for (final StepCounter step : readings) {
            array.put(mapStepCount(step));
        }

        try {
            final int code = post("api/v4/StepCount", array.toString());
            if (code >= 200 && code < 300) {
                final long latestTimestamp = readings.get(readings.size() - 1).timestamp;
                PersistentStore.setLong(STEPS_WATERMARK_KEY, latestTimestamp + 1);
                UserError.Log.d(TAG, "Uploaded " + readings.size() + " step count readings");
            } else {
                UserError.Log.e(TAG, "Step count upload failed: HTTP " + code);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Step count upload error: " + e.getMessage());
        }
    }

    /**
     * POST JSON to a Nocturne API endpoint.
     *
     * @return HTTP status code
     */
    private int post(final String path, final String jsonBody) throws Exception {
        final Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + token)
                .header("Origin", baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .post(RequestBody.create(JSON, jsonBody))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.code();
        }
    }

    /**
     * DELETE to a Nocturne API endpoint.
     *
     * @return HTTP status code
     */
    private int delete(final String path) throws Exception {
        final Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + token)
                .header("Origin", baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .delete()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.code();
        }
    }

    // --- Upload methods for additional data streams ---

    private boolean uploadCalibrations(final List<Calibration> calibrations) {
        if (calibrations == null || calibrations.isEmpty()) return true;
        try {
            final JSONArray array = new JSONArray();
            for (final Calibration cal : calibrations) {
                if (cal.slope == 0) continue; // skip invalid calibrations
                array.put(mapCalibration(cal.timestamp, cal.slope, cal.intercept, cal.first_scale));
            }
            if (array.length() == 0) return true;
            final int code = post("api/v4/glucose/calibrations", array.toString());
            if (code < 200 || code >= 300) {
                UserError.Log.e(TAG, "Failed to upload calibrations, HTTP " + code);
                return false;
            }
            return true;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error uploading calibrations: " + e.getMessage());
            return false;
        }
    }

    private boolean uploadBloodTests(final List<BloodTest> bloodTests) {
        if (bloodTests == null || bloodTests.isEmpty()) return true;
        try {
            final JSONArray array = new JSONArray();
            for (final BloodTest bt : bloodTests) {
                array.put(mapBloodTest(bt.mgdl, bt.timestamp, bt.source));
            }
            final int code = post("api/v4/glucose/meter", array.toString());
            if (code < 200 || code >= 300) {
                UserError.Log.e(TAG, "Failed to upload blood tests, HTTP " + code);
                return false;
            }
            return true;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error uploading blood tests: " + e.getMessage());
            return false;
        }
    }

    private boolean uploadTreatments(final List<Treatments> treatments) {
        if (treatments == null || treatments.isEmpty()) return true;
        try {
            final JSONArray meals = new JSONArray();
            final JSONArray boluses = new JSONArray();
            final JSONArray carbs = new JSONArray();
            final JSONArray notes = new JSONArray();
            final JSONArray deviceEvents = new JSONArray();

            for (final Treatments t : treatments) {
                final TreatmentRoute route = routeTreatment(t);
                switch (route) {
                    case DEVICE_EVENT:
                        deviceEvents.put(mapDeviceEvent(t.timestamp, t.eventType, t.notes, t.uuid));
                        break;
                    case MEAL:
                        meals.put(mapMeal(t.timestamp, t.insulin, t.carbs, t.uuid));
                        break;
                    case BOLUS:
                        final List<InsulinInjection> injections = t.getInsulinInjections();
                        if (injections != null && !injections.isEmpty()) {
                            for (final InsulinInjection inj : injections) {
                                if (inj.getUnits() > 0) {
                                    boluses.put(mapBolus(t.timestamp, inj.getUnits(), inj.getInsulin(), t.uuid));
                                }
                            }
                        } else {
                            boluses.put(mapBolus(t.timestamp, t.insulin, null, t.uuid));
                        }
                        break;
                    case CARBS:
                        carbs.put(mapCarbIntake(t.timestamp, t.carbs, t.uuid));
                        break;
                    case NOTE:
                        notes.put(mapNote(t.timestamp, t.notes, t.eventType, t.uuid));
                        break;
                    case SKIP:
                    default:
                        break;
                }
            }

            boolean success = true;
            if (meals.length() > 0) {
                final int code = post("api/v4/nutrition/meals", meals.toString());
                if (code < 200 || code >= 300) {
                    UserError.Log.e(TAG, "Failed to upload meals, HTTP " + code);
                    success = false;
                }
            }
            if (boluses.length() > 0) {
                final int code = post("api/v4/insulin/boluses", boluses.toString());
                if (code < 200 || code >= 300) {
                    UserError.Log.e(TAG, "Failed to upload boluses, HTTP " + code);
                    success = false;
                }
            }
            if (carbs.length() > 0) {
                final int code = post("api/v4/nutrition/carbs", carbs.toString());
                if (code < 200 || code >= 300) {
                    UserError.Log.e(TAG, "Failed to upload carbs, HTTP " + code);
                    success = false;
                }
            }
            if (notes.length() > 0) {
                final int code = post("api/v4/observations/notes", notes.toString());
                if (code < 200 || code >= 300) {
                    UserError.Log.e(TAG, "Failed to upload notes, HTTP " + code);
                    success = false;
                }
            }
            if (deviceEvents.length() > 0) {
                final int code = post("api/v4/observations/device-events", deviceEvents.toString());
                if (code < 200 || code >= 300) {
                    UserError.Log.e(TAG, "Failed to upload device events, HTTP " + code);
                    success = false;
                }
            }
            return success;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error uploading treatments: " + e.getMessage());
            return false;
        }
    }

    private void deleteTreatments(final List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return;

        final String[] deletePaths = {
                "api/v4/insulin/boluses/by-sync-id",
                "api/v4/nutrition/carbs/by-sync-id",
                "api/v4/observations/notes/by-sync-id",
                "api/v4/observations/device-events/by-sync-id"
        };

        for (final String uuid : uuids) {
            boolean deleted = false;
            for (final String path : deletePaths) {
                try {
                    final int code = delete(path + "?dataSource=xdrip&syncIdentifier=" + java.net.URLEncoder.encode(uuid, "UTF-8"));
                    if (code == 204) {
                        deleted = true;
                        break;
                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Error deleting treatment " + uuid + " from " + path + ": " + e.getMessage());
                }
            }
            if (!deleted) {
                UserError.Log.d(TAG, "Treatment " + uuid + " not found in any Nocturne resource for deletion");
            }
        }
    }

    private void uploadDeviceStatus() {
        try {
            final JSONObject uploader = new JSONObject();
            uploader.put("battery", PowerStateReceiver.getBatteryLevel());

            final JSONObject status = new JSONObject();
            status.put("device", "xDrip+");
            status.put("mills", JoH.tsl());
            status.put("uploader", uploader);

            final JSONArray array = new JSONArray();
            array.put(status);

            final int code = post("api/v1/devicestatus", array.toString());
            if (code < 200 || code >= 300) {
                UserError.Log.e(TAG, "Failed to upload device status, HTTP " + code);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error uploading device status: " + e.getMessage());
        }
    }

    private void uploadMotionTracking() {
        try {
            final long watermark = Math.max(PersistentStore.getLong(MOTION_WATERMARK_KEY),
                    JoH.tsl() - Constants.DAY_IN_MS * 7);
            final ArrayList<ActivityRecognizedService.motionData> readings =
                    ActivityRecognizedService.getForGraph(watermark, JoH.tsl());

            if (readings == null || readings.isEmpty()) return;

            final JSONArray array = new JSONArray();
            long highestTimestamp = 0;

            for (final ActivityRecognizedService.motionData reading : readings) {
                final JSONObject obj = new JSONObject();
                obj.put("mills", reading.timestamp);
                obj.put("utcOffset", utcOffsetMinutes(reading.timestamp));
                obj.put("type", "motion-class");
                obj.put("description", reading.toPrettyType());
                obj.put("enteredBy", "xDrip+");
                array.put(obj);
                highestTimestamp = Math.max(highestTimestamp, reading.timestamp);
            }

            final int code = post("api/v4/activity", array.toString());
            if (code >= 200 && code < 300) {
                PersistentStore.setLong(MOTION_WATERMARK_KEY, highestTimestamp + 1);
                UserError.Log.d(TAG, "Uploaded " + readings.size() + " motion tracking readings");
            } else {
                UserError.Log.e(TAG, "Motion tracking upload failed: HTTP " + code);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error uploading motion tracking: " + e.getMessage());
        }
    }

    // --- Mapping methods ---

    private static JSONObject mapBgReading(final BgReading reading) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("timestamp", toIso8601(reading.timestamp));
            obj.put("utcOffset", TimeZone.getDefault().getOffset(reading.timestamp) / 60000);
            obj.put("mgdl", reading.calculated_value);
            obj.put("device", "xDrip-" + DexCollectionType.getDexCollectionType());
            obj.put("app", "xDrip+");
            obj.put("dataSource", Pref.getString("dex_collection_method", "unknown"));
            obj.put("syncIdentifier", reading.uuid);

            final String direction = reading.slopeName();
            if (direction != null && !direction.isEmpty()) {
                obj.put("direction", direction);
            }

            // calculated_value_slope is per ms; convert to per minute
            obj.put("trendRate", reading.calculated_value_slope * 60000);
            // delta = slope per 5 minutes
            obj.put("delta", reading.calculated_value_slope * 5 * 60000);
            obj.put("noise", reading.noiseValue());
            obj.put("filtered", reading.ageAdjustedFiltered() * 1000);
            obj.put("unfiltered", reading.usedRaw() * 1000);
        } catch (Exception e) {
            UserError.Log.e("NocturneUploader", "Error mapping BgReading: " + e.getMessage());
        }
        return obj;
    }

    private static JSONObject mapHeartRate(final HeartRate hr) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("timestamp", toIso8601(hr.timestamp));
            obj.put("bpm", hr.bpm);
            obj.put("accuracy", hr.accuracy);
            obj.put("device", "xDrip+");
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
        } catch (Exception e) {
            UserError.Log.e("NocturneUploader", "Error mapping HeartRate: " + e.getMessage());
        }
        return obj;
    }

    private static JSONObject mapStepCount(final StepCounter step) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("timestamp", toIso8601(step.timestamp));
            obj.put("metric", step.metric);
            obj.put("source", step.source);
            obj.put("device", "xDrip+");
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
        } catch (Exception e) {
            UserError.Log.e("NocturneUploader", "Error mapping StepCount: " + e.getMessage());
        }
        return obj;
    }

    static String toIso8601(final long epochMillis) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(epochMillis));
    }

    static int utcOffsetMinutes(final long epochMillis) {
        return TimeZone.getDefault().getOffset(epochMillis) / 60000;
    }

    // --- Treatment routing ---

    enum TreatmentRoute { DEVICE_EVENT, MEAL, BOLUS, CARBS, NOTE, SKIP }

    static final Set<String> DEVICE_EVENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Sensor Start", "Sensor Change", "Sensor Stop",
            "Site Change", "Insulin Change", "Pump Battery Change",
            "Pod Change", "Reservoir Change", "Cannula Change",
            "Transmitter Sensor Insert"
    )));

    static final Map<String, String> DEVICE_EVENT_TYPE_MAP;
    static {
        final Map<String, String> m = new HashMap<>();
        m.put("Sensor Start", "SensorStart");
        m.put("Sensor Change", "SensorChange");
        m.put("Sensor Stop", "SensorStop");
        m.put("Site Change", "SiteChange");
        m.put("Insulin Change", "InsulinChange");
        m.put("Pump Battery Change", "PumpBatteryChange");
        m.put("Pod Change", "PodChange");
        m.put("Reservoir Change", "ReservoirChange");
        m.put("Cannula Change", "CannulaChange");
        m.put("Transmitter Sensor Insert", "TransmitterSensorInsert");
        DEVICE_EVENT_TYPE_MAP = Collections.unmodifiableMap(m);
    }

    static TreatmentRoute routeTreatment(final Treatments t) {
        // Loop prevention
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

    // --- Mapping methods for treatments ---

    static JSONObject mapBloodTest(final double mgdl, final long timestamp, final String source) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("mgdl", mgdl);
            obj.put("device", source);
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping BloodTest: " + e.getMessage());
        }
        return obj;
    }

    static JSONObject mapCalibration(final long timestamp, final double slope, final double intercept, final double scale) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("slope", slope);
            obj.put("intercept", intercept);
            obj.put("scale", scale);
            obj.put("device", "xDrip-" + DexCollectionType.getDexCollectionType().toString());
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping Calibration: " + e.getMessage());
        }
        return obj;
    }

    static JSONObject mapBolus(final long timestamp, final double insulin, final String insulinType, final String syncIdentifier) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("insulin", insulin);
            obj.put("kind", "Manual");
            if (insulinType != null) {
                obj.put("insulinType", insulinType);
            }
            obj.put("syncIdentifier", syncIdentifier);
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping Bolus: " + e.getMessage());
        }
        return obj;
    }

    static JSONObject mapCarbIntake(final long timestamp, final double carbs, final String syncIdentifier) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("carbs", carbs);
            obj.put("syncIdentifier", syncIdentifier);
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping CarbIntake: " + e.getMessage());
        }
        return obj;
    }

    static JSONObject mapMeal(final long timestamp, final double insulin, final double carbs, final String syncIdentifier) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("insulin", insulin);
            obj.put("carbs", carbs);
            obj.put("syncIdentifier", syncIdentifier);
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping Meal: " + e.getMessage());
        }
        return obj;
    }

    static JSONObject mapNote(final long timestamp, final String text, final String eventType, final String syncIdentifier) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("text", text);
            obj.put("eventType", eventType);
            obj.put("isAnnouncement", false);
            obj.put("syncIdentifier", syncIdentifier);
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping Note: " + e.getMessage());
        }
        return obj;
    }

    static JSONObject mapDeviceEvent(final long timestamp, final String xdripEventType, final String notes, final String syncIdentifier) {
        final JSONObject obj = new JSONObject();
        try {
            final String mapped = DEVICE_EVENT_TYPE_MAP.get(xdripEventType);
            obj.put("eventType", mapped != null ? mapped : xdripEventType);
            if (notes != null && !notes.isEmpty()) {
                obj.put("notes", notes);
            }
            obj.put("syncIdentifier", syncIdentifier);
            obj.put("app", "xDrip+");
            obj.put("dataSource", "xdrip");
            obj.put("timestamp", toIso8601(timestamp));
            obj.put("utcOffset", utcOffsetMinutes(timestamp));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error mapping DeviceEvent: " + e.getMessage());
        }
        return obj;
    }
}
