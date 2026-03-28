package com.eveningoutpost.dexdrip.cgm.nsfollow;

import android.net.ConnectivityManager;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.NightscoutTreatments;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.AuthResponse;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.NightscoutV3Response;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService.BaseCallback;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.cgm.nsfollow.NightscoutFollowService.msg;

/**
 * Data transport interface to Nightscout API v3 for follower service.
 * Auth: exchanges the access token for a JWT via /api/v2/authorization/request/{token},
 * then sends it as Bearer header on v3 calls. JWT is cached until near expiry.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3 {

    private static final String TAG = "NightscoutFollowV3";

    private static final boolean D = true;

    private static final Gson GSON = new Gson();

    private static volatile Nightscout service;

    private static volatile String cachedJwt;
    private static volatile long jwtExpiry;

    public interface Nightscout {

        @Headers({
                "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME,
        })
        @GET("/api/v3/entries?sort$desc=date&type=sgv&fields=date,dateString,sysTime,sgv,unfiltered,filtered,noise,identifier")
        Call<NightscoutV3Response<List<Entry>>> getEntries(
                @Header("Authorization") String auth,
                @Query(value = "date$gt", encoded = true) long sinceMs,
                @Query("limit") int limit);

        @GET("/api/v3/treatments?sort$desc=date&fields=identifier,eventType,uuid,enteredBy,glucoseType,created_at,glucose,units,carbs,insulin,insulinInjections,notes")
        Call<NightscoutV3Response<List<JsonObject>>> getTreatments(
                @Header("Authorization") String auth,
                @Query(value = "date$gt", encoded = true) long sinceMs,
                @Query("limit") int limit);

        @GET("/api/v3/devicestatus?sort$desc=date&limit=1&fields=uploaderBattery,isCharging,pump,date")
        Call<NightscoutV3Response<List<DeviceStatus>>> getDeviceStatus(
                @Header("Authorization") String auth);
    }

    public interface NightscoutAuth {

        @GET("/api/v2/authorization/request/{accessToken}")
        Call<AuthResponse> requestJwt(@Path("accessToken") String accessToken);
    }

    private static Nightscout getService() {
        if (service == null) {
            try {
                service = RetrofitService.getRetrofitInstance(getUrl(), TAG, D).create(Nightscout.class);
            } catch (NullPointerException e) {
                UserError.Log.e(TAG, "Null pointer trying to getService()");
            }
        }
        return service;
    }

    public static void work(final boolean live) {
        msg(xdrip.gs(R.string.nsfollow_connecting));

        final String urlString = getUrl();

        // Entries callback — uses v3 populator to extract result list
        final List<Entry>[] entriesHolder = new List[1];
        final BaseCallback<NightscoutV3Response<List<Entry>>> entriesCallback =
                new NightscoutCallback<>(
                        "NS entries download",
                        new Session(),
                        response -> entriesHolder[0] = response.result,
                        () -> {
                            EntryProcessor.processEntries(entriesHolder[0], live);
                            NightscoutFollowService.updateBgReceiveDelay();
                            NightscoutFollowService.scheduleWakeUp();
                            msg("");
                        });
        entriesCallback.setOnFailure(() -> msg(httpErrorMsg(entriesCallback.getStatus())));

        // Treatments callback — normalise v3 field names then pass JSON array string downstream
        final String[] treatmentsJson = {null};
        final BaseCallback<NightscoutV3Response<List<JsonObject>>> treatmentsCallback =
                new NightscoutCallback<>(
                        "NS treatments download",
                        new Session(),
                        response -> {
                            final List<JsonObject> items =
                                    response.result != null ? response.result : Collections.emptyList();
                            normalizeV3Treatments(items);
                            treatmentsJson[0] = GSON.toJson(items);
                        },
                        () -> {
                            try {
                                NightscoutTreatments.processTreatmentResponse(treatmentsJson[0]);
                                NightscoutFollowService.updateTreatmentDownloaded();
                            } catch (Exception e) {
                                msg(xdrip.gs(R.string.nsfollow_treatments_exception) + e);
                            }
                        });
        treatmentsCallback.setOnFailure(() -> msg(httpErrorMsg(treatmentsCallback.getStatus())));

        if (!emptyString(urlString)) {
            final String auth = buildBearerAuth();
            final Nightscout svc = getService();
            if (svc == null) {
                msg(xdrip.gs(R.string.nsfollow_v3_cannot_connect));
                return;
            }
            try {
                final BgReading last = BgReading.last(true);  // true = follower mode (no sensor constraint)
                final long sinceMs = (last != null) ? last.timestamp : 0;
                final int safetyLimit = (int) (Constants.DAY_IN_MS / DEXCOM_PERIOD);
                UserError.Log.d(TAG, "Fetching entries since: " + sinceMs + " (limit " + safetyLimit + ")");
                svc.getEntries(auth, sinceMs, safetyLimit).enqueue(entriesCallback);
            } catch (Exception e) {
                UserError.Log.e(TAG, "Exception in entries work() " + e);
                msg(xdrip.gs(R.string.nsfollow_entries_error) + e);
            }
            if (NightscoutFollow.treatmentDownloadEnabled()) {
                if (JoH.ratelimit("nsfollow-v3-treatment-download", 60)) {
                    try {
                        final Treatments lastTreatment = Treatments.lastNotFromXdrip();
                        final long treatmentSinceMs = (lastTreatment != null) ? lastTreatment.timestamp : 0;
                        svc.getTreatments(auth, treatmentSinceMs, 100).enqueue(treatmentsCallback);
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Exception in treatments work() " + e);
                        msg(xdrip.gs(R.string.nsfollow_treatments_error) + e);
                    }
                }
            }
            if (JoH.ratelimit("nsfollow-v3-devicestatus", 5 * 60)) {
                try {
                    svc.getDeviceStatus(auth).enqueue(
                            new NightscoutCallback<NightscoutV3Response<List<DeviceStatus>>>(
                                    "NS devicestatus download",
                                    new Session(),
                                    response -> {
                                        if (response.result != null && !response.result.isEmpty()) {
                                            applyDeviceStatus(response.result.get(0));
                                        }
                                    },
                                    () -> UserError.Log.d(TAG, "Device status updated")));
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception in devicestatus work() " + e);
                }
            }
        } else {
            msg(xdrip.gs(R.string.nsfollow_no_url));
        }
    }

    private static String getUrl() {
        return Pref.getString("nsfollow_url", "");
    }

    /** Extracts the access token from the userinfo of {@code nsfollow_url} (e.g. {@code https://token@host}). */
    static String getToken() {
        try {
            final String userInfo = new URI(getUrl()).getUserInfo();
            return emptyString(userInfo) ? null : userInfo;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Exchanges the access token for a short-lived JWT, caching the result until near expiry.
     * Returns null if no access token is configured or if the exchange fails.
     */
    static synchronized String getJwt() {
        final String accessToken = getToken();
        if (accessToken == null) return null;

        // Return cached JWT if still valid (with 1-minute buffer before expiry)
        if (cachedJwt != null && JoH.tsl() < jwtExpiry - Constants.MINUTE_IN_MS) {
            return cachedJwt;
        }

        try {
            final Response<AuthResponse> resp = RetrofitService
                    .getRetrofitInstance(getUrl(), TAG, D)
                    .create(NightscoutAuth.class)
                    .requestJwt(accessToken)
                    .execute();
            final AuthResponse body = resp.body();
            if (resp.isSuccessful() && body != null && body.token != null) {
                cachedJwt = body.token;
                jwtExpiry = body.exp > 0
                        ? body.exp * 1000L
                        : JoH.tsl() + 7 * Constants.HOUR_IN_MS;
                UserError.Log.d(TAG, "JWT obtained for subject: " + body.sub
                        + ", valid until: " + JoH.dateTimeText(jwtExpiry));
                return cachedJwt;
            }
            UserError.Log.e(TAG, "JWT exchange failed: HTTP " + resp.code());
        } catch (Exception e) {
            UserError.Log.e(TAG, "JWT exchange exception: " + e);
        }
        return null;
    }

    @VisibleForTesting
    static synchronized void setJwtForTest(final String jwt, final long expiryMs) {
        cachedJwt = jwt;
        jwtExpiry = expiryMs;
    }

    @VisibleForTesting
    static void applyDeviceStatus(final DeviceStatus ds) {
        if (ds.uploaderBattery != null) {
            PumpStatus.setBattery(ds.uploaderBattery);
        }
        if (ds.pump != null && ds.pump.reservoir != null) {
            PumpStatus.setReservoir(ds.pump.reservoir);
        }
    }

    private static String buildBearerAuth() {
        final String jwt = getJwt();
        return jwt != null ? "Bearer " + jwt : null;
    }

    /** Returns a human-readable JWT status for MegaStatus, or null if no token is configured. */
    static String jwtStatusText() {
        if (getToken() == null) return null;
        if (cachedJwt == null) return xdrip.gs(R.string.nsfollow_v3_jwt_not_fetched);
        if (JoH.tsl() >= jwtExpiry) return xdrip.gs(R.string.nsfollow_v3_jwt_expired);
        return String.format(xdrip.gs(R.string.nsfollow_v3_jwt_active), JoH.niceTimeTill(jwtExpiry));
    }

    /** Maps raw callback status strings containing HTTP codes to user-friendly messages. */
    static String httpErrorMsg(final String status) {
        if (status == null) return xdrip.gs(R.string.nsfollow_v3_error_unknown);
        if (status.contains("401")) return xdrip.gs(R.string.nsfollow_v3_error_auth);
        if (status.contains("403")) return xdrip.gs(R.string.nsfollow_v3_error_access_denied);
        if (status.contains("404")) return xdrip.gs(R.string.nsfollow_v3_error_not_found);
        if (status.contains("Failed:")) return xdrip.gs(R.string.nsfollow_v3_error_connection);
        return status;
    }

    /** Maps v3 {@code identifier} to {@code _id} on each treatment, as required by NightscoutTreatments. */
    static void normalizeV3Treatments(final List<JsonObject> items) {
        for (final JsonObject item : items) {
            if (!item.has("_id") && item.has("identifier")) {
                item.addProperty("_id", item.get("identifier").getAsString());
            }
        }
    }

    static boolean isNetworkAvailable(final ConnectivityManager cm) {
        return cm != null && cm.getActiveNetwork() != null;
    }

    public static synchronized void resetInstance() {
        RetrofitService.remove(getUrl(), TAG, D);
        service = null;
        cachedJwt = null;
        jwtExpiry = 0;
        PumpStatus.setBattery(-1);
        PumpStatus.setReservoir(-1);
        UserError.Log.d(TAG, "Instance reset");
    }
}
