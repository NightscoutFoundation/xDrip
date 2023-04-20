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
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper.enableTls12OnPreLollipop;
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
        Call<List<Entry>> getEntries(@Header("api-secret") String secret, @Query("count") int count, @Query("rr") String rr);

        @GET("/api/v1/treatments")
        Call<ResponseBody> getTreatments(@Header("api-secret") String secret);
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

    public static final TypeAdapter<Number> UNRELIABLE_INTEGER = new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
            JsonToken jsonToken = in.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    String s = in.nextString();
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                    try {
                        return (int)Double.parseDouble(s);
                    } catch (NumberFormatException ignored) {
                    }
                    return null;
                case NULL:
                    in.nextNull();
                    return null;
                case BOOLEAN:
                    in.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("Expecting number, got: " + jsonToken);
            }
        }
        @Override
        public void write(JsonWriter out, Number value) throws IOException {
            out.value(value);
        }
    };
    public static final TypeAdapterFactory UNRELIABLE_INTEGER_FACTORY = TypeAdapters.newFactory(int.class, Integer.class, UNRELIABLE_INTEGER);

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

            final Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(UNRELIABLE_INTEGER_FACTORY)
                    .create();
            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
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