package com.eveningoutpost.dexdrip.tidepool;

import static com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper.enableTls12OnPreLollipop;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.store.FastStore;

import java.util.List;

import lombok.Getter;
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
import retrofit2.http.Query;

/** jamorham
 *
 * Tidepool Uploader
 *
 * huge thanks to bassettb for a working c# reference implementation upon which this is based
 */

public class TidepoolUploader {

    protected static final String TAG = "TidepoolUploader";
    protected static final String STATUS_KEY = "Tidepool-Status";
    private static final boolean D = true;
    private static final boolean REPEAT = false;

    private static Retrofit retrofit;
    private static final String INTEGRATION_BASE_URL = "https://int-api.tidepool.org";
    private static final String PRODUCTION_BASE_URL = "https://api.tidepool.org";
    @Getter
    private static final String SESSION_TOKEN_HEADER = "x-tidepool-session-token";

    private static PowerManager.WakeLock wl;

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

            final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            if (D) {
                httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            final OkHttpClient client = enableTls12OnPreLollipop(new OkHttpClient.Builder())
                    .addInterceptor(httpLoggingInterceptor)
                    .addInterceptor(new InfoInterceptor(TAG))
                    //          .addInterceptor(new GzipRequestInterceptor())
                    .build();

            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(Pref.getBooleanDefaultFalse("tidepool_dev_servers") ? INTEGRATION_BASE_URL : PRODUCTION_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static void resetInstance() {
        retrofit = null;
        AuthFlowOut.clearAllSavedData();
        UserError.Log.d(TAG, "Instance reset");
    }

    public static void doLoginFromUi() {
        doLogin(true);
    }

    public static synchronized void doLogin(final boolean fromUi) {
        if (!TidepoolEntry.enabled()) {
            UserError.Log.d(TAG, "Cannot login as disabled by preference");
            if (fromUi) {
                JoH.static_toast_long("Cannot login as Tidepool feature not enabled");
            }
            return;
        }
        // TODO failure backoff
        if (JoH.ratelimit("tidepool-login", 10)) {
            extendWakeLock(30000);
            if (Pref.getBooleanDefaultFalse("tidepool_new_auth")) {
                UserError.Log.d(TAG, "Using new auth method");
                AuthFlowIn.handleTokenLoginAndStartSession();
            } else {
                UserError.Log.d(TAG, "Using old auth method");
                final Session session = new Session(MAuthRequest.getAuthRequestHeader(), SESSION_TOKEN_HEADER);
                if (session.authHeader != null) {
                    final Call<MAuthReply> call = session.service.getLogin(session.authHeader);
                    status("Connecting");
                    if (fromUi) {
                        JoH.static_toast_long("Connecting to Tidepool");
                    }
                    call.enqueue(new TidepoolCallback<MAuthReply>(session, "Login", () -> startSession(session, fromUi))
                            .setOnFailure(() -> loginFailed(fromUi)));
                } else {
                    UserError.Log.e(TAG, "Cannot do login as user credentials have not been set correctly");
                    status("Invalid credentials");
                    if (fromUi) {
                        JoH.static_toast_long("Cannot login as Tidepool credentials have not been set correctly");
                    }
                }
                releaseWakeLock();
            }
        }
    }

    private static void loginFailed(boolean fromUi) {
        if (fromUi) {
            JoH.static_toast_long("Login failed - see event log for details");
        }
        releaseWakeLock();
    }

/*    public static void testLogin(Context rootContext) {
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
    }*/


    public static void startSession(final Session session, boolean fromUi) {
        if (JoH.ratelimit("tidepool-start-session", 60)) {
            extendWakeLock(30000);
            if (session.authReply.userid != null) {
                // See if we already have an open data set to write to
                Call<List<MDatasetReply>> datasetCall = session.service.getOpenDataSets(session.token,
                        session.authReply.userid, BuildConfig.APPLICATION_ID, 1);

                datasetCall.enqueue(new TidepoolCallback<List<MDatasetReply>>(session, "Get Open Datasets", () -> {
                    if (session.datasetReply == null) {
                        status("New data set");
                        if (fromUi) {
                            JoH.static_toast_long("Creating new data set - all good");
                        }
                        Call<MDatasetReply> call = session.service.openDataSet(session.token, session.authReply.userid, new MOpenDatasetRequest().getBody());
                        call.enqueue(new TidepoolCallback<MDatasetReply>(session, "Open New Dataset", () -> doUpload(session))
                                .setOnFailure(TidepoolUploader::releaseWakeLock));
                    } else {
                        UserError.Log.d(TAG, "Existing Dataset: " + session.datasetReply.getUploadId());
                        // TODO: Wouldn't need to do this if we could block on the above `call.enqueue`.
                        // ie, do the openDataSet conditionally, and then do `doUpload` either way.
                        status("Appending");
                        if (fromUi) {
                            JoH.static_toast_long("Found existing remote data set - all good");
                        }
                        doUpload(session);
                    }
                }).setOnFailure(TidepoolUploader::releaseWakeLock));
            } else {
                UserError.Log.wtf(TAG, "Got login response but cannot determine userid - cannot proceed");
                if (fromUi) {
                    JoH.static_toast_long("Error: Cannot determine userid");
                }
                status("Error userid");
                releaseWakeLock();
            }
        } else {
            status("Cool Down Wait");
            if (fromUi) {
                JoH.static_toast_long("In cool down period, please wait 1 minute");
            }
        }
    }


    private static void doUpload(final Session session) {
        if (!TidepoolEntry.enabled()) {
            UserError.Log.e(TAG, "Cannot upload - preference disabled");
            return;
        }
        extendWakeLock(60000);
        session.iterations++;
        final String chunk = UploadChunk.getNext(session);
        if (chunk != null) {
            if (chunk.length() == 2) {
                UserError.Log.d(TAG, "Empty data set - marking as succeeded");
                doCompleted(session);
            } else {
                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), chunk);

                final Call<MUploadReply> call = session.service.doUpload(session.token, session.datasetReply.getUploadId(), body);
                status("Uploading");
                call.enqueue(new TidepoolCallback<MUploadReply>(session, "Data Upload", () -> {
                    UploadChunk.setLastEnd(session.end);

                    if (REPEAT && !session.exceededIterations()) {
                        status("Queued Next");
                        UserError.Log.d(TAG, "Scheduling next upload");
                        Inevitable.task("Tidepool-next", 10000, () -> doUpload(session));
                    } else {

                        if (MOpenDatasetRequest.isNormal()) {
                            doClose(session);
                        } else {
                            doCompleted(session);
                        }
                    }
                }).setOnFailure(TidepoolUploader::releaseWakeLock));
            }
        } else {
            UserError.Log.e(TAG, "Upload chunk is null, cannot proceed");
            releaseWakeLock();
        }
    }


    private static void doClose(final Session session) {
        status("Closing");
        extendWakeLock(20000);
        final Call<MDatasetReply> call = session.service.closeDataSet(session.token, session.datasetReply.getUploadId(), new MCloseDatasetRequest().getBody());
        call.enqueue(new TidepoolCallback<>(session, "Session Stop", TidepoolUploader::closeSuccess));
    }

    private static void closeSuccess() {
        status("Closed");
        UserError.Log.d(TAG, "Close success");
        releaseWakeLock();
    }

    private static void doCompleted(final Session session) {
        status("Completed OK");
        UserError.Log.d(TAG, "ALL COMPLETED OK!");
        releaseWakeLock();
    }

    private static void status(final String status) {
        FastStore.getInstance().putS(STATUS_KEY, status);
    }

    private static synchronized void extendWakeLock(long ms) {
        if (wl == null) {
            wl = JoH.getWakeLock("tidepool-uploader", (int) ms);
        } else {
            JoH.releaseWakeLock(wl); // lets not get too messy
            wl.acquire(ms);
        }
    }

    protected static synchronized void releaseWakeLock() {
        UserError.Log.d(TAG, "Releasing wakelock");
        JoH.releaseWakeLock(wl);
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
