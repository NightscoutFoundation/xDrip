package com.eveningoutpost.dexdrip.cgm.nsfollow;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.NightscoutTreatments;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.NightscoutUrl;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.cgm.nsfollow.NightscoutFollowService.msg;

/**
 * jamorham
 *
 * Data transport interface to Nightscout for follower service
 *
 */
public class NightscoutFollow {

    private static final String TAG = "NightscoutFollow";

    private static final boolean D = true;

    private static Nightscout service;


    public interface Nightscout {
        @Headers({
                "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME,
        })

        @GET("/api/v1/entries.json")
        Call<List<Entry>> getEntries(@Header("api-secret") String secret, @Query("count") int count, @Query("rr") String rr);

        @GET("/api/v1/entries.json")
        Call<List<Entry>> getEntriesSince(@Header("api-secret") String secret, @Query("count") int count, @Query(value = "find[date][$gt]", encoded = true) long sinceMs, @Query("rr") String rr);

        @GET("/api/v1/treatments")
        Call<ResponseBody> getTreatments(@Header("api-secret") String secret);

        @GET("/api/v1/devicestatus.json?count=1")
        Call<List<DeviceStatus>> getDeviceStatus(@Header("api-secret") String secret);
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
        msg("Connecting to Nightscout");

        final String urlString = getUrl();

        final Session session = new Session();
        session.url = new NightscoutUrl(urlString);

        // set up processing callback for entries
        session.entriesCallback = new NightscoutCallback<List<Entry>>("NS entries download", session, () -> {
            // process data
            EntryProcessor.processEntries(session.entries, live);
            NightscoutFollowService.updateBgReceiveDelay();
            NightscoutFollowService.scheduleWakeUp();
            msg("");
        })
                .setOnFailure(() -> msg(session.entriesCallback.getStatus()));

        // set up processing callback for treatments
        session.treatmentsCallback = new NightscoutCallback<ResponseBody>("NS treatments download", session, () -> {
            // process data
            try {
                NightscoutTreatments.processTreatmentResponse(session.treatments.string());
                NightscoutFollowService.updateTreatmentDownloaded();
            } catch (Exception e) {
                msg("Treatments: " + e);
            }
        })
                .setOnFailure(() -> msg(session.treatmentsCallback.getStatus()));

        if (!emptyString(urlString)) {
            try {
                final BgReading last = BgReading.last(true);
                final long lastTs = (last != null) ? last.timestamp : 0;
                if (lastTs > 0) {
                    final long cutoff = Math.max(lastTs, JoH.tsl() - Constants.DAY_IN_MS);
                    final int safetyLimit = 2 * 24 * 60; // 2× headroom: fits 24h at any upload rate
                    UserError.Log.d(TAG, "Fetching entries since: " + cutoff + " (limit " + safetyLimit + ")");
                    getService().getEntriesSince(session.url.getHashedSecret(), safetyLimit, cutoff, JoH.tsl() + "").enqueue(session.entriesCallback);
                } else {
                    UserError.Log.d(TAG, "No prior reading - fetching last 10 entries");
                    getService().getEntries(session.url.getHashedSecret(), 10, JoH.tsl() + "").enqueue(session.entriesCallback);
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Exception in entries work() " + e);
                msg("Nightscout follow entries error: " + e);
            }
            if (treatmentDownloadEnabled()) {
                if (JoH.ratelimit("nsfollow-treatment-download", 60)) {
                    try {
                        getService().getTreatments(session.url.getHashedSecret()).enqueue(session.treatmentsCallback);
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Exception in treatments work() " + e);
                        msg("Nightscout follow treatments error: " + e);
                    }
                }
            }
            if (NsServerCapabilities.supportsDeviceStatus(urlString)
                    && JoH.ratelimit("nsfollow-devicestatus", 5 * 60)) {
                try {
                    getService().getDeviceStatus(session.url.getHashedSecret())
                            .enqueue(new DeviceStatusCallback(session, urlString));
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception in devicestatus work() " + e);
                }
            }
        } else {
            msg("Please define Nightscout follow URL");
        }
    }

    private static String getUrl() {
        return Pref.getString("nsfollow_url", "");
    }

    static boolean treatmentDownloadEnabled() {
        return Pref.getBooleanDefaultFalse("nsfollow_download_treatments");
    }

    public static void resetInstance() {
        RetrofitService.remove(getUrl(), TAG, D);
        service = null;
        PumpStatus.setBattery(-1);
        PumpStatus.setReservoir(-1);
        NightscoutFollowService.clearUploaderStatus();
        UserError.Log.d(TAG, "Instance reset");
        CollectionServiceStarter.restartCollectionServiceBackground();
    }

    @VisibleForTesting
    static void applyDeviceStatus(final DeviceStatus ds) {
        final Integer uploaderBat = resolveUploaderBattery(ds);
        final Integer pumpBatteryPercent = ds.pump != null && ds.pump.battery != null
                ? ds.pump.battery.percent : null;
        if (pumpBatteryPercent != null) {
            PumpStatus.setBattery(pumpBatteryPercent);
        } else if (uploaderBat != null) {
            PumpStatus.setBattery(uploaderBat);
        }

        if (ds.pump != null && ds.pump.reservoir != null) {
            PumpStatus.setReservoir(ds.pump.reservoir);
        }

        NightscoutFollowService.updateUploaderStatus(uploaderBat, ds.isCharging);
    }

    private static Integer resolveUploaderBattery(final DeviceStatus ds) {
        if (ds.uploaderBattery != null) return ds.uploaderBattery;
        if (ds.uploader != null) return ds.uploader.battery;
        return null;
    }

    /**
     * Devicestatus callback that disables further devicestatus polling for this server when the
     * endpoint is rejected with an HTTP 4xx (e.g. Juggluco returns 400 Bad Request).
     */
    private static final class DeviceStatusCallback extends NightscoutCallback<List<DeviceStatus>> {
        private final String url;

        DeviceStatusCallback(final Session session, final String url) {
            super("NS devicestatus download", session, statusList -> {
                if (!statusList.isEmpty()) {
                    applyDeviceStatus(statusList.get(0));
                }
            }, null);
            this.url = url;
        }

        @Override
        public void onResponse(final Call<List<DeviceStatus>> call, final Response<List<DeviceStatus>> response) {
            super.onResponse(call, response);
            final int code = response.code();
            if (response.isSuccessful()) {
                // Self-heal: endpoint works, clear any prior unsupported mark for this server.
                NsServerCapabilities.markDeviceStatusSupported(url);
            } else if (code >= 400 && code < 500) {
                UserError.Log.d(TAG, "devicestatus unsupported (" + code + ") — disabling for this server");
                NsServerCapabilities.markDeviceStatusUnsupported(url);
            }
        }
    }
}
