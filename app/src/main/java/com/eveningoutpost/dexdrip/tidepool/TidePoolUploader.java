package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/** jamorham
 *
 * Tidepool Uploader
 *
 * huge thanks to bassettb for a working c# reference implementation upon which this is based
 */

public class TidePoolUploader {

    protected static final String TAG = "TidePoolUploader";
    private static final boolean D = true;
    private static final boolean REPEAT = false;

    private static Retrofit retrofit;
    private static final String BASE_URL = "https://int-api.tidepool.org";
    private static final String SESSION_TOKEN_HEADER = "x-tidepool-session-token";

    public interface TidePool {
        @Headers({
                "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME,
        })

        @POST("/auth/login")
        Call<MAuthReply> getLogin(@Header("Authorization") String secret);

        @DELETE("dataservices/v1/users/{userId}/data")
        Call<MStartReply> deleteAllData(@Header(SESSION_TOKEN_HEADER) String token, @Path("userId") String id);

        @DELETE("dataservices/v1/datasets/{dataSetId}")
        Call<MStartReply> deleteDataSet(@Header(SESSION_TOKEN_HEADER) String token, @Path("dataSetId") String id);

        @GET("dataservices/v1/datasets/{dataSetId}")
        Call<MStartReply> getDataSet(@Header(SESSION_TOKEN_HEADER) String token, @Path("dataSetId") String id);

        @GET("dataservices/v1/users/{userId}/datasets")
        Call<MStartReply> getReadStart(@Header(SESSION_TOKEN_HEADER) String token, @Path("userId") String id);

        @POST("dataservices/v1/users/{userId}/datasets")
        Call<MStartReply> getStart(@Header(SESSION_TOKEN_HEADER) String token, @Path("userId") String id, @Body RequestBody body);

        @POST("dataservices/v1/datasets/{sessionId}/data")
        Call<MUploadReply> doUpload(@Header(SESSION_TOKEN_HEADER) String token, @Path("sessionId") String id, @Body RequestBody body);

        @PUT("dataservices/v1/datasets/{sessionId}")
        Call<MStartReply> getStop(@Header(SESSION_TOKEN_HEADER) String token, @Path("sessionId") String id, @Body RequestBody body);

    }


    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            if (D) {
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            final OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    //.addInterceptor(new GzipRequestInterceptor())
                    .build();

            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }


    public static void doLogin() {
        if (!enabled()) {
            UserError.Log.d(TAG,"Cannot login as disabled by preference");
            return;
        }
        // TODO failure backoff
        if (JoH.ratelimit("tidepool-login", 60)) {

            final Session session = new Session(MAuthRequest.getAuthRequestHeader(), SESSION_TOKEN_HEADER);
            if (session.authHeader != null) {
                final Call<MAuthReply> call = session.service.getLogin(session.authHeader);
                call.enqueue(new TidePoolCallback<>(session, "Login", () -> startSession(session)));
            } else {
                UserError.Log.e(TAG,"Cannot do login as user credentials have not been set correctly");
            }
        }
    }


    private static void startSession(final Session session) {
        if (JoH.ratelimit("tidepool-start-session", 60)) {

            if (session.authReply.userid != null) {
                Call<MStartReply> call = session.service.getStart(session.token, session.authReply.userid, new MStartRequest().getBody());
                call.enqueue(new TidePoolCallback<>(session, "Session Start", () -> doUpload(session)));
            } else {
                UserError.Log.wtf(TAG, "Got login response but cannot determine userid - cannot proceed");
            }
        }
    }


    private static void doUpload(final Session session) {
        if (!enabled()) {
            UserError.Log.e(TAG,"Cannot upload - preference disabled");
            return;
        }
        session.iterations++;
        final String chunk = UploadChunk.getNext(session);
        if (chunk != null) {
            if (chunk.length() == 2) {
                UserError.Log.d(TAG, "Empty data set - marking as succeeded");
                doCompleted(session);
            } else {
                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), chunk);
                final Call<MUploadReply> call = session.service.doUpload(session.token, session.MStartReply.data.uploadId, body);
                call.enqueue(new TidePoolCallback<>(session, "Data Upload", () -> {
                    UploadChunk.setLastEnd(session.end);

                    if (REPEAT && !session.exceededIterations()) {
                        UserError.Log.d(TAG, "Scheduling next upload");
                        Inevitable.task("Tidepool-next", 10000, () -> doUpload(session));
                    } else {

                        if (MStartRequest.isNormal()) {
                            doClose(session);
                        } else {
                            doCompleted(session);
                        }
                    }
                }));
            }
        } else {
            UserError.Log.e(TAG, "Upload chunk is null, cannot proceed");
        }
    }


    private static void doClose(final Session session) {
        final Call<MStartReply> call = session.service.getStop(session.token, session.MStartReply.data.uploadId, new MStopRequest().getBody());
        call.enqueue(new TidePoolCallback<>(session, "Session Stop", null));
    }

    private static void doCompleted(final Session session) {
        UserError.Log.d(TAG, "ALL COMPLETED OK!");
    }

    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("cloud_storage_tidepool_enable");
    }

    // experimental - not used

    private static void readData(final Session session) {
        Call<MStartReply> call = session.service.getReadStart(session.token, session.authReply.userid);
        call.enqueue(new TidePoolCallback<>(session, "Read Data", null));
    }

    private static void deleteData(final Session session) {
        if (session.authReply.userid != null) {
            Call<MStartReply> call = session.service.deleteAllData(session.token, session.authReply.userid);
            call.enqueue(new TidePoolCallback<>(session, "Delete Data", null));
        } else {
            UserError.Log.wtf(TAG, "Got login response but cannot determine userid - cannot proceed");
        }
    }

    private static void getDataSet(final Session session) {
        Call<MStartReply> call = session.service.getDataSet(session.token, "bogus");
        call.enqueue(new TidePoolCallback<>(session, "Get Data", null));
    }

    private static void deleteDataSet(final Session session) {
        Call<MStartReply> call = session.service.deleteDataSet(session.token, "bogus");
        call.enqueue(new TidePoolCallback<>(session, "Delete Data", null));
    }

}
