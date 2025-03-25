package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.NightscoutTreatments;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.NightscoutUrl;
import com.eveningoutpost.dexdrip.evaluators.MissedReadingsEstimator;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
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

        @GET("/api/v1/treatments")
        Call<ResponseBody> getTreatments(@Header("api-secret") String secret);
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
                int count = Math.min(MissedReadingsEstimator.estimate() + 1, (int) (Constants.DAY_IN_MS / DEXCOM_PERIOD));
                UserError.Log.d(TAG, "Estimating missed readings as: " + count);
                count = Math.max(10, count); // pep up with a view to potential period mismatches - might be excessive
                getService().getEntries(session.url.getHashedSecret(), count, JoH.tsl() + "").enqueue(session.entriesCallback);
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
        UserError.Log.d(TAG, "Instance reset");
        CollectionServiceStarter.restartCollectionServiceBackground();
    }
}