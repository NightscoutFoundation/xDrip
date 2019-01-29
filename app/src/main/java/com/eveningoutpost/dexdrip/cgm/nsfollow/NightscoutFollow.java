package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.NightscoutUrl;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.UtilityModels.OkHttpWrapper.enableTls12OnPreLollipop;
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

    private static Retrofit retrofit;
    private static Nightscout service;


    public interface Nightscout {
        @Headers({
                "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME,
        })

        @GET("/api/v1/entries.json")
        Call<List<Entry>> getEntries(@Header("api-secret") String secret, @Query("rr") String rr);
    }

    private static Nightscout getService() {
        if (service == null) {
            try {
                service = getRetrofitInstance().create(Nightscout.class);
            } catch (NullPointerException e) {
                UserError.Log.e(TAG, "Null pointer trying to getService()");
            }
        }
        return service;
    }

    public static void work(final boolean live) {
        msg("Connecting to Nightscout");
        final Session session = new Session();
        final String urlString = getUrl();
        session.url = new NightscoutUrl(urlString);

        session.callback = new NightscoutCallback<List<Entry>>("NS entries download", session, () -> {
            // process data
            EntryProcessor.processEntries(session.entries, live);
            NightscoutFollowService.scheduleWakeUp();
            msg("");
        })
                .setOnFailure(() -> msg(session.callback.getStatus()));

        if (!emptyString(urlString)) {
            try {
                getService().getEntries(session.url.getHashedSecret(), JoH.tsl() + "").enqueue(session.callback);
            } catch (Exception e) {
                UserError.Log.e(TAG, "Exception in work() " + e);
                msg("Nightscout follow error: " + e);
            }
        } else {
            msg("Please define Nightscout follow URL");
        }
    }

    private static String getUrl() {
        return Pref.getString("nsfollow_url", "");
    }

    // TODO make reusable
    public static Retrofit getRetrofitInstance() throws IllegalArgumentException {
        if (retrofit == null) {
            final String url = getUrl();
            if (emptyString(url)) {
                UserError.Log.d(TAG, "Empty url - cannot create instance");
                return null;
            }
            final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            if (D) {
                httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            final OkHttpClient client = enableTls12OnPreLollipop(new OkHttpClient.Builder())
                    .addInterceptor(httpLoggingInterceptor)
                    .addInterceptor(new InfoInterceptor(TAG))
                    .addInterceptor(new GzipRequestInterceptor())
                    .build();

            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static void resetInstance() {
        retrofit = null;
        service = null;
        UserError.Log.d(TAG, "Instance reset");
        CollectionServiceStarter.restartCollectionServiceBackground();
    }

}
