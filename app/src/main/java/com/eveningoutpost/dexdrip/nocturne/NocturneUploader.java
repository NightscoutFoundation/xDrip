package com.eveningoutpost.dexdrip.nocturne;

import android.content.Context;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    public boolean upload(final List<BgReading> bgReadings) {
        if (!ready) {
            UserError.Log.e(TAG, "upload: not ready (missing token or URL)");
            return false;
        }

        boolean sgvSuccess = true;

        if (Pref.getBooleanDefaultFalse("nocturne_upload_sgv")) {
            sgvSuccess = uploadSgv(bgReadings);
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_heartrate")) {
            try {
                uploadHeartRates();
            } catch (Exception e) {
                UserError.Log.e(TAG, "Heart rate upload failed: " + e.getMessage());
            }
        }

        if (Pref.getBooleanDefaultFalse("nocturne_upload_stepcount")) {
            try {
                uploadStepCounts();
            } catch (Exception e) {
                UserError.Log.e(TAG, "Step count upload failed: " + e.getMessage());
            }
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
        } catch (Exception e) {
            UserError.Log.e("NocturneUploader", "Error mapping StepCount: " + e.getMessage());
        }
        return obj;
    }

    private static String toIso8601(final long epochMillis) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(epochMillis));
    }
}
