package com.eveningoutpost.dexdrip.tidepool;

import android.app.AlertDialog;
import android.content.Context;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
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
import retrofit2.http.Query;

/** jamorham
 *
 * Tidepool Uploader
 *
 * huge thanks to bassettb for a working c# reference implementation upon which this is based
 */

public class TidepoolUploader {

    protected static final String TAG = "TidepoolUploader";
    private static final boolean D = true;
    private static final boolean REPEAT = false;

    private static Retrofit retrofit;
//    private static final String BASE_URL = "https://int-api.tidepool.org";
    private static final String BASE_URL = "https://132e3caa.ngrok.io";
    private static final String SESSION_TOKEN_HEADER = "x-tidepool-session-token";

    public interface Tidepool {
        @Headers({
                "User-Agent: xDrip+ " + BuildConfig.VERSION_NAME,
                "X-Tidepool-Client-Name: " + BuildConfig.APPLICATION_ID,
                "X-Tidepool-Client-Version: 0.1.0", // TODO: const it
        })

        @POST("/auth/login")
        Call<MAuthReply> getLogin(@Header("Authorization") String secret);

        @DELETE("/v1/users/{userId}/data")
        Call<MDatasetReply> deleteAllData(@Header(SESSION_TOKEN_HEADER) String token, @Path("userId") String id);

        @DELETE("/v1/datasets/{dataSetId}")
        Call<MDatasetReply> deleteDataSet(@Header(SESSION_TOKEN_HEADER) String token, @Path("dataSetId") String id);

        @GET("/v1/users/{userId}/data_sets")
        Call<List<MDatasetReply>> getOpenDataSets(@Header(SESSION_TOKEN_HEADER) String token,
                                                  @Path("userId") String id,
                                                  @Query("client.name") String clientName,
                                                  @Query("deviceId") String deviceId,
                                                  @Query("size") int size);

        @GET("/v1/datasets/{dataSetId}")
        Call<MDatasetReply> getDataSet(@Header(SESSION_TOKEN_HEADER) String token, @Path("dataSetId") String id);

        @POST("/v1/users/{userId}/data_sets")
        Call<MDatasetReply> openDataSet(@Header(SESSION_TOKEN_HEADER) String token, @Path("userId") String id, @Body RequestBody body);

        @POST("/v1/datasets/{sessionId}/data")
        Call<MUploadReply> doUpload(@Header(SESSION_TOKEN_HEADER) String token, @Path("sessionId") String id, @Body RequestBody body);

        @PUT("/v1/datasets/{sessionId}")
        Call<MDatasetReply> closeDataSet(@Header(SESSION_TOKEN_HEADER) String token, @Path("sessionId") String id, @Body RequestBody body);

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
        if (JoH.ratelimit("tidepool-login", 1)) {

            final Session session = new Session(MAuthRequest.getAuthRequestHeader(), SESSION_TOKEN_HEADER);
            if (session.authHeader != null) {
                final Call<MAuthReply> call = session.service.getLogin(session.authHeader);
                call.enqueue(new TidepoolCallback<>(session, "Login", () -> startSession(session)));
            } else {
                UserError.Log.e(TAG,"Cannot do login as user credentials have not been set correctly");
            }
        }
    }

    public static void testLogin(Context rootContext) {
        if (JoH.ratelimit("tidepool-login", 1)) {

            String message = "Failed to log into Tidepool.\n" +
                    "Check that your user name and password are correct.";

            final Session session = new Session(MAuthRequest.getAuthRequestHeader(), SESSION_TOKEN_HEADER);
            if (session.authHeader != null) {
                final Call<MAuthReply> call = session.service.getLogin(session.authHeader);

                try {
                    Response<MAuthReply> response = call.execute();
                    UserError.Log.e(TAG, "Header: " + response.code());
                    message = "Successfully logged into Tidepool.";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                UserError.Log.e(TAG,"Cannot do login as user credentials have not been set correctly");
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(rootContext);

            builder.setTitle("Tidepool Login");

            builder.setMessage(message);

            builder.setPositiveButton("OK", (dialog, id) -> {
                dialog.dismiss();
            });

            final AlertDialog alert = builder.create();
            alert.show();
        }
    }


    private static void startSession(final Session session) {
        if (JoH.ratelimit("tidepool-start-session", 60)) {

            if (session.authReply.userid != null) {
                // See if we already have an open data set to write to
                Call<List<MDatasetReply>> datasetCall = session.service.getOpenDataSets(session.token,
                        session.authReply.userid, BuildConfig.APPLICATION_ID, MOpenDatasetRequest.DEVICE_ID, 1);

                datasetCall.enqueue(new TidepoolCallback<>(session, "Get Open Datasets", () -> {
                    UserError.Log.d(TAG, "Existing Dataset: " + session.datasetReply);
                    if(session.datasetReply == null) {
                        Call<MDatasetReply> call = session.service.openDataSet(session.token, session.authReply.userid, new MOpenDatasetRequest().getBody());
                        call.enqueue(new TidepoolCallback<>(session, "Open New Dataset", () -> doUpload(session)));
                    } else {
                        // TODO: Wouldn't need to do this if we could block on the above `call.enqueue`.
                        // ie, do the openDataSet conditionally, and then do `doUpload` either way.
                        doUpload(session);
                    }
                }));
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
                final Call<MUploadReply> call = session.service.doUpload(session.token, session.datasetReply.data.uploadId, body);
                call.enqueue(new TidepoolCallback<>(session, "Data Upload", () -> {
                    UploadChunk.setLastEnd(session.end);

                    if (REPEAT && !session.exceededIterations()) {
                        UserError.Log.d(TAG, "Scheduling next upload");
                        Inevitable.task("Tidepool-next", 10000, () -> doUpload(session));
                    } else {

                        if (MOpenDatasetRequest.isNormal()) {
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
        final Call<MDatasetReply> call = session.service.closeDataSet(session.token, session.datasetReply.data.uploadId, new MCloseDatasetRequest().getBody());
        call.enqueue(new TidepoolCallback<>(session, "Session Stop", null));
    }

    private static void doCompleted(final Session session) {
        UserError.Log.d(TAG, "ALL COMPLETED OK!");
    }

    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("cloud_storage_tidepool_enable");
    }

    // experimental - not used

    private static void deleteData(final Session session) {
        if (session.authReply.userid != null) {
            Call<MDatasetReply> call = session.service.deleteAllData(session.token, session.authReply.userid);
            call.enqueue(new TidepoolCallback<>(session, "Delete Data", null));
        } else {
            UserError.Log.wtf(TAG, "Got login response but cannot determine userid - cannot proceed");
        }
    }

    private static void getDataSet(final Session session) {
        Call<MDatasetReply> call = session.service.getDataSet(session.token, "bogus");
        call.enqueue(new TidepoolCallback<>(session, "Get Data", null));
    }

    private static void deleteDataSet(final Session session) {
        Call<MDatasetReply> call = session.service.deleteDataSet(session.token, "bogus");
        call.enqueue(new TidepoolCallback<>(session, "Delete Data", null));
    }

}
