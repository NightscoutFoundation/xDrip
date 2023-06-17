package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.MegaStatus;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.DateUtil;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Mdns;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.WriteResult;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.CipherSuite;
import okhttp3.Handshake;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.TlsVersion;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper.enableTls12OnPreLollipop;

/**
 * THIS CLASS WAS BUILT BY THE NIGHTSCOUT GROUP FOR THEIR NIGHTSCOUT ANDROID UPLOADER
 * https://github.com/nightscout/android-uploader/
 * I have modified this class to make it fit my needs
 * Modifications include field remappings and lists instead of arrays
 * A DTO would probably be a better future implementation
 * -Emma Black
 */
public class NightscoutUploader {

        private static final String TAG = NightscoutUploader.class.getSimpleName();
        private static final int SOCKET_TIMEOUT = 60000;
        private static final int CONNECTION_TIMEOUT = 30000;
        private static final boolean d = false;
        private static final boolean USE_GZIP = true; // conditional inside interceptor
        public static final String VIA_NIGHTSCOUT_LOADER_TAG = "Nightscout Loader";

        public static long last_success_time = -1;
        public static long last_exception_time = -1;
        public static int last_exception_count = 0;
        public static int last_exception_log_count = 0;
        public static String last_exception;
        public static final String VIA_NIGHTSCOUT_TAG = "via Nightscout";

        private static boolean notification_shown = false;

        private static final String LAST_SUCCESS_TREATMENT_DOWNLOAD = "NS-Last-Treatment-Download-Modified";
        private static final String ETAG = "ETAG";


        private static int failurecount = 0;

        public static final int FAIL_NOTIFICATION_PERIOD = 24 * 60 * 60; // Failed upload notification will be shown if there is no upload for 24 hours.
        public static final int FAIl_COUNT_NOTIFICATION = FAIL_NOTIFICATION_PERIOD / 60 / 5 -1; // Number of 5-minute read cycles corresponding to notification period
        public static final int FAIL_LOG_PERIOD = 6 * 60 * 60; // FAILED upload/download log will be shown if there is no upload/download for 6 hours.
        public static final int FAIL_COUNT_LOG = FAIL_LOG_PERIOD / 60 / 5 -1; // Number of 5-minute read cycles corresponding to log period

        private Context mContext;
        private Boolean enableRESTUpload;
        private Boolean enableMongoUpload;
        private SharedPreferences prefs;
        private OkHttpClient client;

        public interface NightscoutService {
            @POST("entries")
            Call<ResponseBody> upload(@Header("api-secret") String secret, @Body RequestBody body);

            @POST("entries")
            Call<ResponseBody> upload(@Body RequestBody body);

            @POST("devicestatus")
            Call<ResponseBody> uploadDeviceStatus(@Body RequestBody body);

            @POST("devicestatus")
            Call<ResponseBody> uploadDeviceStatus(@Header("api-secret") String secret, @Body RequestBody body);

            @GET("status.json")
            Call<ResponseBody> getStatus(@Header("api-secret") String secret);

            @POST("treatments")
            Call<ResponseBody> uploadTreatments(@Header("api-secret") String secret, @Body RequestBody body);

            @PUT("treatments")
            Call<ResponseBody> upsertTreatments(@Header("api-secret") String secret, @Body RequestBody body);

            @GET("treatments")
                // retrofit2/okhttp3 could do the if-modified-since natively using cache
            Call<ResponseBody> downloadTreatments(@Header("api-secret") String secret, @Header("BROKEN-If-Modified-Since") String ifmodified);

            @GET("treatments.json")
            Call<ResponseBody> findTreatmentByUUID(@Header("api-secret") String secret, @Query("find[uuid]") String uuid);

            @DELETE("treatments/{id}")
            Call<ResponseBody> deleteTreatment(@Header("api-secret") String secret, @Path("id") String id);

            @POST("activity")
            Call<ResponseBody> uploadActivity(@Header("api-secret") String secret, @Body RequestBody body);

        }

        private class UploaderException extends RuntimeException {
            int code;

            public UploaderException (String message, int code) {
                super(message);
                this.code = code;
            }
        }

        public NightscoutUploader(Context context) {
            mContext = context;
            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final OkHttpClient.Builder okHttp3Builder = enableTls12OnPreLollipop(new OkHttpClient.Builder());
            if (UserError.ExtraLogTags.shouldLogTag(TAG, android.util.Log.VERBOSE)) {
                okHttp3Builder.addInterceptor(new SSLHandshakeInterceptor());
            }
            if (USE_GZIP) okHttp3Builder.addInterceptor(new GzipRequestInterceptor());
            okHttp3Builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            okHttp3Builder.writeTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
            okHttp3Builder.readTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
            client = okHttp3Builder.build();
            enableRESTUpload = prefs.getBoolean("cloud_storage_api_enable", false);
            enableMongoUpload = prefs.getBoolean("cloud_storage_mongodb_enable", false);
        }

    public static void launchDownloadRest() {
        if (Pref.getBooleanDefaultFalse("cloud_storage_api_enable")
                && Pref.getBooleanDefaultFalse("cloud_storage_api_download_enable")) {
            if (JoH.ratelimit("cloud_treatment_download", 60)) {
                final NightscoutUploader uploader = new NightscoutUploader(xdrip.getAppContext());
                uploader.downloadRest(500);
            }
        }
    }

    public static boolean isNightscoutCompatible(String url) {
        final String vers = getNightscoutVersion(url);
        return !(vers.startsWith("0.8") || vers.startsWith("0.7") || vers.startsWith("0.6"));
    }

    public static String getNightscoutVersion(String url) {
        try {
            final String store_marker = "nightscout-status-poll-" + url;
            final JSONObject status = new JSONObject(PersistentStore.getString(store_marker));
            return status.getString("version");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void doStatusUpdate(NightscoutService nightscoutService, String url, String hashedSecret) {
        final String store_marker = "nightscout-status-poll-" + url;
        final String old_data = PersistentStore.getString(store_marker);
        int retry_secs = (old_data.length() == 0) ? 20 : 86400;
        if (old_data.equals("error")) retry_secs = 3600;
        if (JoH.pratelimit("poll-nightscout-status-" + url, retry_secs)) {
            try {
                final Response<ResponseBody> r;
                r = nightscoutService.getStatus(hashedSecret).execute();
                if ((r != null) && (r.isSuccessful())) {
                    final String response = r.body().string();
                    if (d) Log.d(TAG, "Status Response: " + response);
                    // TODO do we need to parse json here or should we just store string?
                    final JSONObject tr = new JSONObject(response);
                    if (d) Log.d(TAG, url + " " + tr.toString());
                    PersistentStore.setString(store_marker, tr.toString());
                    checkGzipSupport(r);
                } else {
                    PersistentStore.setString(store_marker, "error");
                    Log.d(TAG, "Failure to get status data from: " + url + " " + ((r != null) ? r.message() : ""));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Got json exception in status update parsing: " + e);
            } catch (IOException e) {
                Log.e(TAG, "Got exception attempting status update: " + e);
            }
        }
    }

    public static String uuid_to_id(String uuid) {
        if (uuid.length() == 24) return uuid; // already converted
        if (uuid.length() < 24) {
            // convert non-standard uuids to compatible ones
            return CipherUtils.getMD5(uuid).substring(0,24);
        }
        return uuid.replaceAll("-", "").substring(0, 24);
    }

    public boolean downloadRest(final long sleep) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = JoH.getWakeLock("ns-download-rest", 180000);
                try {
                    try {
                        if (sleep > 0) Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        //
                    }
                    if (doRESTtreatmentDownload(prefs)) {
                        Home.staticRefreshBGCharts();
                    }
                } finally {
                    JoH.releaseWakeLock(wl);
                }
            }
        }).start();
        return true;
    }

    public boolean uploadRest(List<BgReading> glucoseDataSets, List<BloodTest> meterRecords, List<Calibration> calRecords) {

        boolean apiStatus = false;

        if (enableRESTUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a REST API", glucoseDataSets.size()));
            apiStatus = doRESTUpload(prefs, glucoseDataSets, meterRecords, calRecords);
            Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms result: %b", glucoseDataSets.size(), System.currentTimeMillis() - start, apiStatus));

            if (prefs.getBoolean("cloud_storage_api_download_enable", false)) {
                start = System.currentTimeMillis();
                final boolean substatus = doRESTtreatmentDownload(prefs);
                if (substatus) {
                    Home.staticRefreshBGCharts();
                }
                Log.i(TAG, String.format("Finished download using a REST API in %s ms result: %b", System.currentTimeMillis() - start, substatus));
            }
        }
        return apiStatus;
    }

    public boolean uploadMongo(List<BgReading> glucoseDataSets, List<Calibration> meterRecords, List<Calibration> calRecords, List<TransmitterData> transmittersData, List<LibreBlock> libreBlock) {
        boolean mongoStatus = false;


        if (enableMongoUpload) {
            double start = new Date().getTime();
            mongoStatus = doMongoUpload(prefs, glucoseDataSets, meterRecords, calRecords, transmittersData, libreBlock);
            Log.i(TAG, String.format("Finished upload of %s record using a Mongo in %s ms result: %b", 
                    glucoseDataSets.size() + meterRecords.size() + calRecords.size() + transmittersData.size() + libreBlock.size(), System.currentTimeMillis() - start, mongoStatus));
        }

        return mongoStatus;
    }
    
    private String TryResolveName(String baseURI) {
        Log.d(TAG,  "Resolveing name" );
        URI uri;
        try {
            uri = new URI(baseURI);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error URISyntaxException for the base URL", e);
            return baseURI;
        }
        String host = uri.getHost();
        Log.d(TAG, "host = " + host);
        // Host has either to end with .local, or be one word (with no dots) for us to try and resolve it.
        if (host == null ||  (host.contains(".") && (!host.endsWith(".local")))) {
            return baseURI;
        }
        // So, we need to resolve this name
        String fullHost = host;
        try {
            if(!fullHost.endsWith(".local")) {
                fullHost += ".local";
            }
            String ip = Mdns.genericResolver(fullHost);
            if(ip == null) {
                Log.d(TAG, "Recieved null resolving " + fullHost);
                return baseURI;
            }
            // Resolve succeeded, replace host, with the resovled address.
            String newUri = baseURI.replace(host, ip);
            Log.d(TAG, "Returning new uri " + newUri);
            return newUri;
            
            
        } catch (UnknownHostException e) {
            Log.w(TAG, "UnknownHostException error nanme not resovled" + fullHost);
            return baseURI;
        }
    }
    
    private synchronized boolean doRESTtreatmentDownload(SharedPreferences prefs) {
        final String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
        final ArrayList<String> baseURIs = new ArrayList<>();

        boolean new_data = false;
        Log.d(TAG, "doRESTtreatmentDownload() starting run");

        try {
            for (String baseURLSetting : baseURLSettings.split(" ")) {
                String baseURL = baseURLSetting.trim();
                if (baseURL.isEmpty()) continue;
                baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to process API Base URL: " + e);
            return false;
        }



        // process a list of base uris
        for (String baseURI : baseURIs) {
            try {
                baseURI = TryResolveName(baseURI);
                int apiVersion = 0;
                URI uri = new URI(baseURI);
                if ((uri.getHost().startsWith("192.168.")) && prefs.getBoolean("skip_lan_uploads_when_no_lan", true) && (!JoH.isLANConnected())) {
                    Log.d(TAG, "Skipping Nighscout download from: " + uri.getHost() + " due to no LAN connection");
                    continue;
                }

                if (uri.getPath().endsWith("/v1/")) apiVersion = 1;
                String baseURL;
                String secret = uri.getUserInfo();
                if ((secret == null || secret.isEmpty()) && apiVersion == 0) {
                    baseURL = baseURI;
                } else if ((secret == null || secret.isEmpty())) {
                    throw new Exception("Starting with API v1, a pass phase is required");
                } else if (apiVersion > 0) {
                    baseURL = baseURI.replaceFirst("//[^@]+@", "//");
                } else {
                    throw new Exception("Unexpected baseURI: " + baseURI);
                }

                final Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURL).client(client).build();
                final NightscoutService nightscoutService = retrofit.create(NightscoutService.class);

                final String checkurl = retrofit.baseUrl().url().toString();
                if (!isNightscoutCompatible(checkurl)) {
                    Log.e(TAG, "Nightscout version: " + getNightscoutVersion(checkurl) + " on " + checkurl + " is not compatible with the Rest-API download feature!");
                    continue;
                }

                if (apiVersion == 1) {
                    final String hashedSecret = Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();
                    final Response<ResponseBody> r;
                    if (hashedSecret != null) {
                        doStatusUpdate(nightscoutService, retrofit.baseUrl().url().toString(), hashedSecret); // update status if needed
                        final String LAST_MODIFIED_KEY = LAST_SUCCESS_TREATMENT_DOWNLOAD + CipherUtils.getMD5(uri.toString()); // per uri marker
                        String last_modified_string = PersistentStore.getString(LAST_MODIFIED_KEY);
                        if (last_modified_string.equals("")) last_modified_string = JoH.getRFC822String(0);
                        final long request_start = JoH.tsl();
                        r = nightscoutService.downloadTreatments(hashedSecret, last_modified_string).execute();

                        if ((r != null) && (r.raw().networkResponse().code() == HttpURLConnection.HTTP_NOT_MODIFIED)) {
                            Log.d(TAG, "Treatments on " + uri.getHost() + ":" + uri.getPort() + " not modified since: " + last_modified_string);
                            continue; // skip further processing of this url
                        }

                        if ((r != null) && (r.isSuccessful())) {

                            last_modified_string = r.raw().header("Last-Modified", JoH.getRFC822String(request_start));
                            final String this_etag = r.raw().header("Etag", "");
                            if (this_etag.length() > 0) {
                                // older versions of nightscout don't support if-modified-since so check the etag for duplication
                                if (this_etag.equals(PersistentStore.getString(ETAG + LAST_MODIFIED_KEY))) {
                                    Log.d(TAG, "Skipping Treatments on " + uri.getHost() + ":" + uri.getPort() + " due to etag duplicate: " + this_etag);
                                    continue;
                                }
                                PersistentStore.setString(ETAG + LAST_MODIFIED_KEY, this_etag);
                            }
                            final String response = r.body().string();
                            if (d) Log.d(TAG, "Response: " + response);

                            new_data = NightscoutTreatments.processTreatmentResponse(response);
                            PersistentStore.setString(LAST_MODIFIED_KEY, last_modified_string);
                            checkGzipSupport(r);
                        } else {
                            Log.d(TAG, "Failed to get treatments from the base URL");
                        }

                    } else {
                        Log.d(TAG, "Old api version not supported");
                    }
                }


            } catch (Exception e) {
                String msg = "Unable to do REST API Download " + e + e.getMessage();
                handleRestFailure(msg);
            }
        }
        Log.d(TAG, "doRESTtreatmentDownload() finishing run");
        return new_data;
    }

    private boolean doRESTUpload(SharedPreferences prefs, List<BgReading> glucoseDataSets, List<BloodTest> meterRecords, List<Calibration> calRecords) {
            String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
            ArrayList<String> baseURIs = new ArrayList<String>();

            try {
                for (String baseURLSetting : baseURLSettings.split(" ")) {
                    String baseURL = baseURLSetting.trim();
                    if (baseURL.isEmpty()) continue;
                    baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to process API Base URL: "+e);
                return false;
            }
            boolean any_successes = false;
            for (String baseURI : baseURIs) {
                try {
                    baseURI = TryResolveName(baseURI);
                    int apiVersion = 0;
                    URI uri = new URI(baseURI);
                    if ((uri.getHost().startsWith("192.168.")) && prefs.getBoolean("skip_lan_uploads_when_no_lan", true) && (!JoH.isLANConnected()))
                    {
                        Log.d(TAG,"Skipping Nighscout upload to: "+uri.getHost()+" due to no LAN connection");
                        continue;
                    }
                    if (uri.getPath().endsWith("/v1/")) apiVersion = 1;
                    String baseURL;
                    String secret = uri.getUserInfo();
                    if ((secret == null || secret.isEmpty()) && apiVersion == 0) {
                        baseURL = baseURI;
                    } else if ((secret == null || secret.isEmpty())) {
                        throw new Exception("Starting with API v1, a pass phase is required");
                    } else if (apiVersion > 0) {
                        baseURL = baseURI.replaceFirst("//[^@]+@", "//");
                    } else {
                        throw new Exception("Unexpected baseURI: "+baseURI);
                    }

                    final Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURL).client(client).build();
                    final NightscoutService nightscoutService = retrofit.create(NightscoutService.class);

                    if (apiVersion == 1) {
                        String hashedSecret = Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();
                        doStatusUpdate(nightscoutService, retrofit.baseUrl().url().toString(), hashedSecret); // update status if needed
                        doRESTUploadTo(nightscoutService, hashedSecret, glucoseDataSets, meterRecords, calRecords);
                    } else {
                        doLegacyRESTUploadTo(nightscoutService, glucoseDataSets);
                    }
                    any_successes = true;
                    last_success_time = JoH.tsl();
                    last_exception_count = 0;
                    last_exception_log_count = 0;
                } catch (Exception e) {
                    String msg = "Unable to do REST API Upload: " + e.getMessage() + " marking record: " + (any_successes ? "succeeded" : "failed");
                    handleRestFailure(msg);
                }
            }
            return any_successes;
        }

        private void doLegacyRESTUploadTo(NightscoutService nightscoutService, List<BgReading> glucoseDataSets) throws Exception {
            for (BgReading record : glucoseDataSets) {
                Response<ResponseBody> r = nightscoutService.upload(populateLegacyAPIEntry(record)).execute();
                if (!r.isSuccessful()) throw new UploaderException(r.message(), r.code());

            }
            try {
                postDeviceStatus(nightscoutService, null);
            } catch (Exception e) {
                Log.e(TAG, "Ignoring legacy devicestatus post exception: " + e);
            }
        }

        private void doRESTUploadTo(NightscoutService nightscoutService, String secret, List<BgReading> glucoseDataSets, List<BloodTest> meterRecords, List<Calibration> calRecords) throws Exception {
            final JSONArray array = new JSONArray();

            for (BgReading record : glucoseDataSets) {
                populateV1APIBGEntry(array, record);
            }
            for (BloodTest record : meterRecords) {
                populateV1APIMeterReadingEntry(array, record);
            }
            for (Calibration record : calRecords) {
                final BloodTest dupe = BloodTest.getForPreciseTimestamp(record.timestamp, 60000);
                if (dupe == null) {
                    populateV1APIMeterReadingEntry(array, record); // also add calibrations as meter records
                } else {
                    Log.d(TAG, "Found duplicate blood test entry for this calibration record: " + record.bg + " vs " + dupe.mgdl + " mg/dl");
                }
                populateV1APICalibrationEntry(array, record);
            }

            if (array.length() > 0) {//KS
                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), array.toString());
                final Response<ResponseBody> r = nightscoutService.upload(secret, body).execute();
                if (!r.isSuccessful()) throw new UploaderException(r.message(), r.code());
                checkGzipSupport(r);
                try {
                    postDeviceStatus(nightscoutService, secret);
                } catch (Exception e) {
                    Log.e(TAG, "Ignoring devicestatus post exception: " + e);
                }
            }

            try {
                if (Pref.getBooleanDefaultFalse("send_treatments_to_nightscout")) {
                    postTreatments(nightscoutService, secret);
                } else {
                    Log.d(TAG,"Skipping treatment upload due to preference disabled");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception uploading REST API treatments: ", e);
                if (e.getMessage().equals("Not Found")) {
                    final String msg = "Please ensure careportal plugin is enabled on nightscout for treatment upload!";
                    Log.wtf(TAG, msg);
                    Home.toaststaticnext(msg);
                    handleRestFailure(msg);
                }
            }
            // TODO we may want to check nightscout version before trying to upload!!
            // TODO in the future we may want to merge these in to a single post
            if (Pref.getBooleanDefaultFalse("use_pebble_health") && (Home.get_engineering_mode())) {
                try {
                    postHeartRate(nightscoutService, secret);
                    postStepsCount(nightscoutService, secret);
                    postMotionTracking(nightscoutService, secret);
                } catch (Exception e) {
                  if (JoH.ratelimit("heartrate-upload-exception", 3600)) {
                      Log.e(TAG, "Exception uploading REST API heartrate: " + e.getMessage());
                  }
                }
            }

        }

    private static synchronized void handleRestFailure(String msg) {
        last_exception = msg;
        last_exception_time = JoH.tsl();
        last_exception_count++;
        last_exception_log_count++;
        if (last_exception_log_count > FAIL_COUNT_LOG) { // If the number of failed uploads/downloads crosses the logging target
            if (JoH.pratelimit("nightscout-error-log", FAIL_LOG_PERIOD)) { // If there has been more than 6 hours since the last log
                Log.e(TAG, msg);
                last_exception_log_count = 0; // Reset the fail count for logging.
            }
            if (last_exception_count > FAIl_COUNT_NOTIFICATION) { // If the number of failed uploads crosses the notification target
                if (JoH.pratelimit("nightscout-error-notification", FAIL_NOTIFICATION_PERIOD)) { // If there has been more than 24 hours since the last notification
                    if (Pref.getBooleanDefaultFalse("warn_nightscout_failures")) {
                        notification_shown = true;
                        JoH.showNotification("Nightscout Failure", "REST-API upload to Nightscout has failed " + last_exception_count
                                        + " times. With message: " + last_exception + " " + ((last_success_time > 0) ? "Last succeeded: " + JoH.dateTimeText(last_success_time) : ""),

                                MegaStatus.getStatusPendingIntent("Uploaders"), Constants.NIGHTSCOUT_ERROR_NOTIFICATION_ID, NotificationChannels.NIGHTSCOUT_UPLOADER_CHANNEL, false, false, null, null, msg);
                    } else {
                        Log.e(TAG, "Cannot alert for nightscout failures as preference setting is disabled");
                    }
                }
            } else {
                if (notification_shown) {
                    JoH.cancelNotification(Constants.NIGHTSCOUT_ERROR_NOTIFICATION_ID);
                    notification_shown = false;
                }
            }
        }
    }

    private String getDeviceString(BgReading record) {
        String withMethod = "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel");
        if (Pref.getBooleanDefaultFalse("nightscout_device_append_source_info") &&
                record.source_info != null &&
                record.source_info.length() > 0) {
            return withMethod + " " + record.source_info;
        }
        return withMethod;
    }


    private void populateV1APIBGEntry(JSONArray array, BgReading record) throws Exception {
        JSONObject json = new JSONObject();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        json.put("device", getDeviceString(record));
        if (record != null) {//KS
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            if(prefs.getBoolean("cloud_storage_api_use_best_glucose", false)){
                json.put("sgv", (int) record.getDg_mgdl());
                try {
                    json.put("delta", new BigDecimal(record.getDg_slope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP));
                } catch (NumberFormatException e) {
                        UserError.Log.e(TAG, "Problem calculating delta from getDg_slope() for Nightscout REST Upload, skipping");
                }
                json.put("direction", record.getDg_deltaName());
            } else {
                json.put("sgv", (int) record.calculated_value);
                try {
                    json.put("delta", new BigDecimal(record.currentSlope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP)); // jamorham for automation
                } catch (NumberFormatException e) {
                        UserError.Log.e(TAG, "Problem calculating delta from currentSlope() for Nightscout REST Upload, skipping");
                }
                json.put("direction", record.slopeName());
            }
            json.put("type", "sgv");
            json.put("filtered", record.ageAdjustedFiltered() * 1000);
            json.put("unfiltered", record.usedRaw() * 1000);
            json.put("rssi", 100);
            json.put("noise", record.noiseValue());
            json.put("sysTime", format.format(record.timestamp));
            array.put(json);
        }
        else
            Log.e(TAG, "doRESTUploadTo BG record is null.");
    }

        private RequestBody populateLegacyAPIEntry(BgReading record) throws Exception {
            JSONObject json = new JSONObject();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", getDeviceString(record));
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", (int)record.calculated_value);
            json.put("direction", record.slopeName());
            return RequestBody.create(MediaType.parse("application/json"), json.toString());
        }

        private void populateV1APIMeterReadingEntry(JSONArray array, Calibration record) throws Exception {
            if (record == null) {
                Log.e(TAG, "Received null calibration record in populateV1ApiMeterReadingEntry !");
                return;
            }
            JSONObject json = new JSONObject();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("type", "mbg");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("mbg", record.bg);
            json.put("sysTime", format.format(record.timestamp));
            array.put(json);
        }

        private void populateV1APIMeterReadingEntry(JSONArray array, BloodTest record) throws Exception {
            if (record == null) {
                Log.e(TAG, "Received null bloodtest record in populateV1ApiMeterReadingEntry !");
                return;
            }
            JSONObject json = new JSONObject();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", record.source);
            json.put("type", "mbg");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("mbg", record.mgdl);
            json.put("sysTime", format.format(record.timestamp));
            array.put(json);
        }


        private void populateV1APICalibrationEntry(JSONArray array, Calibration record) throws Exception {
            if (record == null) {
                Log.e(TAG, "Received null calibration record in populateV1ApiCalibrationEntry !");
                return;
            }
            //do not upload undefined slopes
            if(record.slope == 0d) return;

            JSONObject json = new JSONObject();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("type", "cal");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            if(record.check_in) {
                json.put("slope", (record.first_slope));
                json.put("intercept", ((record.first_intercept)));
                json.put("scale", record.first_scale);
            } else {
                json.put("slope", (1000/record.slope));
                json.put("intercept", ((record.intercept * -1000) / (record.slope)));
                json.put("scale", 1);
            }
            json.put("sysTime", format.format(record.timestamp));
            array.put(json);
        }

    private void populateV1APITreatmentEntry(JSONArray array, Treatments treatment) throws Exception {

        if (treatment == null) return;
        if (treatment.enteredBy != null && ((treatment.enteredBy.endsWith(VIA_NIGHTSCOUT_TAG)) || (treatment.enteredBy.contains(VIA_NIGHTSCOUT_LOADER_TAG)))) return; // don't send back to nightscout what came from there
        final JSONObject record = new JSONObject();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        record.put("timestamp", treatment.timestamp);
        record.put("eventType", treatment.eventType);
        record.put("enteredBy", treatment.enteredBy);
        record.put("notes", treatment.notes);
        record.put("uuid", treatment.uuid);
        record.put("carbs", treatment.carbs);
        record.put("insulin", treatment.insulin);
        if (treatment.insulinJSON != null) {
            record.put("insulinInjections", treatment.insulinJSON);
        }
        record.put("created_at", treatment.created_at);
        record.put("sysTime", format.format(treatment.timestamp));
        array.put(record);
    }

    private void postTreatments(NightscoutService nightscoutService, String apiSecret) throws Exception {
        Log.d(TAG, "Processing treatments for RESTAPI");
        final long THIS_QUEUE = UploaderQueue.NIGHTSCOUT_RESTAPI;
        final List<UploaderQueue> tups = UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), THIS_QUEUE);
        if (tups != null) {
            JSONArray insert_array = new JSONArray();
            JSONArray upsert_array = new JSONArray();
            for (UploaderQueue up : tups) {
                if ((up.action.equals("insert") || (up.action.equals("update")))) {
                    final Treatments treatment = Treatments.byid(up.reference_id);
                    if (up.action.equals("insert")) {
                        //populateV1APITreatmentEntry(insert_array, treatment);
                        // TODO always use singular upserts for now
                        populateV1APITreatmentEntry(upsert_array, treatment);
                    } else if (up.action.equals("update")) {
                        populateV1APITreatmentEntry(upsert_array, treatment);
                    }
                } else if (up.action.equals("delete")) {
                    if (up.reference_uuid != null) {
                        if (apiSecret != null) {
                            // do we already have a nightscout style reference id
                            String this_id = up.reference_uuid.length() == 24 ? up.reference_uuid : null;
                            Response<ResponseBody> lookup = null;
                            if (this_id == null) {
                                // look up the _id to delete as we can't use find with delete action nor can we specify our own _id on submission circa nightscout 0.9.2
                                lookup = nightscoutService.findTreatmentByUUID(apiSecret, up.reference_uuid).execute();
                            }
                            // throw an exception if we failed lookup
                            if ((this_id == null) && (lookup != null) && !lookup.isSuccessful()) {
                                throw new UploaderException(lookup.message(), lookup.code());
                            } else {
                                // parse the result
                                if (this_id == null) {
                                    try {
                                        final String response = lookup.body().string();
                                        final JSONArray jsonArray = new JSONArray(response);
                                        final JSONObject tr = (JSONObject) jsonArray.get(0); // can only be one
                                        this_id = tr.getString("_id");
                                    } catch (Exception e) {
                                        Log.e(TAG, "Got exception parsing treatment lookup response: " + e);
                                    }
                                }
                                // is the id valid now?
                                if ((this_id != null) && (this_id.length() == 24)) {
                                    final Response<ResponseBody> r = nightscoutService.deleteTreatment(apiSecret, this_id).execute();
                                    if (!r.isSuccessful()) {
                                        throw new UploaderException(r.message(), r.code());
                                    } else {
                                        up.completed(THIS_QUEUE);
                                        Log.d(TAG, "Success for RESTAPI treatment delete: " + up.reference_uuid + " _id: " + this_id);
                                    }
                                } else {
                                    Log.wtf(TAG, "Couldn't find a reference _id for uuid: " + up.reference_uuid + " got: " + this_id);
                                    up.completed(THIS_QUEUE); // don't retry
                                }
                            }
                        } else {
                            Log.wtf(TAG, "Cannot delete treatments without api secret being set");
                        }
                    }
                } else {
                    Log.wtf(TAG, "Unsupported operation type for treatment: " + up.action);
                    up.completed(THIS_QUEUE); // don't retry it
                }
            }
            // handle insert types
            if (insert_array.length() != 0) {
                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), insert_array.toString());
                final Response<ResponseBody> r;
                if (apiSecret != null) {
                    r = nightscoutService.uploadTreatments(apiSecret, body).execute();
                    if (!r.isSuccessful()) {
                        throw new UploaderException(r.message(), r.code());
                    } else {
                        Log.d(TAG, "Success for RESTAPI treatment insert upload");
                        for (UploaderQueue up : tups) {
                            if (up.action.equals("insert")) {
                                up.completed(THIS_QUEUE); // approve all types for this queue
                            }
                        }
                        checkGzipSupport(r);
                    }
                } else {
                    Log.wtf(TAG, "Cannot upload treatments without api secret being set");
                }
            }
            // handle upsert types
            if (upsert_array.length() != 0) {
                for (int i = 0; i < upsert_array.length(); i++) {
                    JSONObject item = (JSONObject) upsert_array.get(i);
                    final String match_uuid = item.getString("uuid");
                    item.put("_id", uuid_to_id(match_uuid));
                    final RequestBody body = RequestBody.create(MediaType.parse("application/json"), item.toString());
                    final Response<ResponseBody> r;
                    if (apiSecret != null) {
                        r = nightscoutService.upsertTreatments(apiSecret, body).execute();
                        if (!r.isSuccessful()) {
                            throw new UploaderException(r.message(), r.code());
                        } else {
                            Log.d(TAG, "Success for RESTAPI treatment upsert upload: " + match_uuid);

                            for (UploaderQueue up : tups) {
                                if (d) Log.d(TAG, "upsert: " + match_uuid + " / " + up.reference_uuid + " " + up.action + " " + up.reference_id);
                                if ((up.action.equals("update") || (up.action.equals("insert")))
                                        && (up.reference_uuid.equals(match_uuid) || (uuid_to_id(up.reference_uuid).equals(match_uuid)))) {
                                    if( d) Log.d(TAG, "upsert: matched");
                                    up.completed(THIS_QUEUE); // approve all types for this queue
                                    break;
                                }
                            }
                            checkGzipSupport(r);
                        }
                    } else {
                        Log.wtf(TAG, "Cannot upload treatments without api secret being set");
                        return;
                    }
                }
                // if we got this far without exception then mark everything as completed to fix harmless erroneous queue entries
                for (UploaderQueue up : tups) {
                    if (d) Log.d(TAG, "Marking all items completed");
                    up.completed(THIS_QUEUE);
                }
            }
        }
    }

    private static int activityErrorCount = 0;
    private static final int MAX_ACTIVITY_RECORDS = 500;

    private void postHeartRate(NightscoutService nightscoutService, String apiSecret) throws Exception {
        Log.d(TAG, "Processing heartrate for RESTAPI");
        if (apiSecret != null) {
            final String STORE_COUNTER = "nightscout-rest-heartrate-synced-time";
            final long syncedTillTime = Math.max(PersistentStore.getLong(STORE_COUNTER), JoH.tsl() - Constants.DAY_IN_MS * 7);
            final List<HeartRate> readings = HeartRate.latestForGraph((MAX_ACTIVITY_RECORDS / Math.min(1, Math.max(activityErrorCount, MAX_ACTIVITY_RECORDS / 10))), syncedTillTime);
            final JSONArray data = new JSONArray();
            //final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            //format.setTimeZone(TimeZone.getDefault());
            long highest_timestamp = 0;
            if (readings.size() > 0) {
                for (HeartRate reading : readings) {
                    final JSONObject json = new JSONObject();
                    json.put("type", "hr-bpm");
                    json.put("timeStamp", reading.timestamp);
                    //json.put("dateString", format.format(reading.timestamp));
                    json.put("created_at", DateUtil.toISOString(reading.timestamp));

                    json.put("bpm", reading.bpm);
                    if (reading.accuracy != 1) json.put("accuracy", reading.accuracy);
                    data.put(json);

                    highest_timestamp = Math.max(highest_timestamp, reading.timestamp);
                }
                // send to nightscout - update counter

                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());
                Response<ResponseBody> r;

                r = nightscoutService.uploadActivity(apiSecret, body).execute();

                if (!r.isSuccessful()) {
                    activityErrorCount++;
                   if (JoH.ratelimit("heartrate-unable-upload",3600)) {
                       UserError.Log.e(TAG, "Unable to upload heart-rate data to Nightscout - check nightscout version");
                   }
                    throw new UploaderException(r.message(), r.code());
                } else {
                    PersistentStore.setLong(STORE_COUNTER, highest_timestamp);
                    UserError.Log.d(TAG, "Updating heartrate synced record count (success) " + JoH.dateTimeText(highest_timestamp) + " Processed: " + readings.size() + " records");
                    checkGzipSupport(r);
                }
            }
        } else {
            UserError.Log.e(TAG, "Api secret is null");
        }
    }



    private void postStepsCount(NightscoutService nightscoutService, String apiSecret) throws Exception {
        Log.d(TAG, "Processing steps for RESTAPI");
        final String STORE_COUNTER = "nightscout-rest-steps-synced-time";
        if (apiSecret != null) {
            final long syncedTillTime = Math.max(PersistentStore.getLong(STORE_COUNTER), JoH.tsl() - Constants.DAY_IN_MS * 7);
            final List<StepCounter> readings = StepCounter.latestForGraph((MAX_ACTIVITY_RECORDS / Math.min(1, Math.max(activityErrorCount, MAX_ACTIVITY_RECORDS / 10))), syncedTillTime);
            final JSONArray data = new JSONArray();
            //final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            //format.setTimeZone(TimeZone.getDefault());
            long highest_timestamp = 0;
            if (readings.size() > 0) {
                for (StepCounter reading : readings) {
                    final JSONObject json = new JSONObject();
                    json.put("type", "steps-total");
                    json.put("timeStamp", reading.timestamp);
                    //json.put("dateString", format.format(reading.timestamp));
                    json.put("created_at", DateUtil.toISOString(reading.timestamp));

                    json.put("steps", reading.metric);
                    data.put(json);

                    highest_timestamp = Math.max(highest_timestamp, reading.timestamp);
                }
                // send to nightscout - update counter

                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());
                Response<ResponseBody> r;

                r = nightscoutService.uploadActivity(apiSecret, body).execute();

                if (!r.isSuccessful()) {
                    activityErrorCount++;
                    UserError.Log.e(TAG, "Unable to upload steps data to Nightscout - check nightscout version");
                    throw new UploaderException(r.message(), r.code());
                } else {
                    PersistentStore.setLong(STORE_COUNTER, highest_timestamp);
                    UserError.Log.e(TAG, "Updating steps synced record count (success) " + JoH.dateTimeText(highest_timestamp) + " Processed: " + readings.size() + " records");
                    checkGzipSupport(r);
                }
            }
        } else {
            UserError.Log.e(TAG, "Api secret is null");
        }
    }

    private void postMotionTracking(NightscoutService nightscoutService, String apiSecret) throws Exception {
        Log.d(TAG, "Processing motion tracking for RESTAPI");
        final String STORE_COUNTER = "nightscout-rest-motion-synced-time";
        if (apiSecret != null) {
            final long syncedTillTime = Math.max(PersistentStore.getLong(STORE_COUNTER), JoH.tsl() - Constants.DAY_IN_MS * 7);
            final ArrayList<ActivityRecognizedService.motionData> readings = ActivityRecognizedService.getForGraph(syncedTillTime, JoH.tsl());
            int counter = 0;

            final JSONArray data = new JSONArray();
            //final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            //format.setTimeZone(TimeZone.getDefault());
            long highest_timestamp = 0;
            if (readings.size() > 0) {
                for (ActivityRecognizedService.motionData reading : readings) {
                    counter++;
                    if (counter > (MAX_ACTIVITY_RECORDS / Math.min(1, Math.max(activityErrorCount, MAX_ACTIVITY_RECORDS / 10)))) break;
                    final JSONObject json = new JSONObject();
                    json.put("type", "motion-class");

                    json.put("timeStamp", reading.timestamp);
                    //json.put("dateString", format.format(reading.timestamp));
                    json.put("created_at", DateUtil.toISOString(reading.timestamp));

                    json.put("class", reading.toPrettyType());
                    data.put(json);

                    highest_timestamp = Math.max(highest_timestamp, reading.timestamp);
                }
                // send to nightscout - update counter

                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());
                Response<ResponseBody> r;

                r = nightscoutService.uploadActivity(apiSecret, body).execute();

                if (!r.isSuccessful()) {
                    activityErrorCount++;
                    UserError.Log.e(TAG, "Unable to upload motion data to Nightscout - check nightscout version");
                    throw new UploaderException(r.message(), r.code());
                } else {
                    PersistentStore.setLong(STORE_COUNTER, highest_timestamp);
                    UserError.Log.e(TAG, "Updating motion synced record count (success) " + JoH.dateTimeText(highest_timestamp) + " Processed: " + readings.size() + " records");
                    checkGzipSupport(r);
                }
            }
        } else {
            UserError.Log.e(TAG, "Api secret is null");
        }
    }

    // attempt to determine if a server supports gzip encoding based on response
    private void checkGzipSupport(Response r) {
        try {
            boolean hasGzip = false;

            // look for a header, doesn't normally seem to be present though
            if (!hasGzip) {
                try {
                    hasGzip = r.headers().get("Accept-Encoding").contains("gzip");
                } catch (Exception e) {
                    //
                }
            }

            // see if we can guess based on server name
            if (!hasGzip) {
                try {
                    final String poweredby = r.headers().get("X-Powered-By");
                    hasGzip = poweredby.contains("Express") || poweredby.contains("ASP.NET");
                } catch (Exception e) {
                    //
                }
            }
            // TODO this currently never unsets
            if (hasGzip) {
                try {
                    setSupportsGzip(r.raw().request().url().uri().getHost() + r.raw().request().url().uri().getPort(), true);
                } catch (Exception e) {
                    // unprocessable
                    UserError.Log.d(TAG, "check gzip: E1 :" + e);
                }
            }
        } catch (Exception e) {
            // unprocessable
            UserError.Log.d(TAG, "check gzip: E2 :" + e);
        }
    }

    private static final String LAST_NIGHTSCOUT_BATTERY_LEVEL = "last-nightscout-battery-level";

    private long getLastBatteryLevel(NightscoutBatteryDevice type) {
        return PersistentStore.getLong(LAST_NIGHTSCOUT_BATTERY_LEVEL + "-" + type.name());
    }

    private void setLastBatteryLevel(NightscoutBatteryDevice type, long value) {
        PersistentStore.setLong(LAST_NIGHTSCOUT_BATTERY_LEVEL + "-" + type.name(), value);
    }

    /**
     * Uploads the device status (containing battery details) to Nightscout for
     */
    private void postDeviceStatus(NightscoutService nightscoutService, String apiSecret) throws Exception {
        // TODO optimize based on changes avoiding stale marker issues

        final List<NightscoutBatteryDevice> batteries = new ArrayList<>();

        batteries.add(NightscoutBatteryDevice.PHONE);

        if ((DexCollectionType.hasBattery() && (Pref.getBoolean("send_bridge_battery_to_nightscout", true)))
                || (Home.get_forced_wear() && DexCollectionType.getDexCollectionType().equals(DexCollectionType.DexcomG5))) {
            batteries.add(NightscoutBatteryDevice.BRIDGE);
        }

        if (DexCollectionType.hasWifi()) {
            batteries.add(NightscoutBatteryDevice.PARAKEET);
        }

        boolean sendDexcomTxBattery = Pref.getBooleanDefaultFalse("send_ob1dex_tx_battery_to_nightscout");
        if (sendDexcomTxBattery) {
            batteries.add(NightscoutBatteryDevice.DEXCOM_TRANSMITTER);
        }

        for (NightscoutBatteryDevice batteryType : batteries) {
            final long last_battery_level = getLastBatteryLevel(batteryType);
            final int new_battery_level = batteryType.getBatteryLevel(mContext);

            if ((new_battery_level > 0) && (new_battery_level != last_battery_level || batteryType.alwaysSendBattery())) {
                setLastBatteryLevel(batteryType, new_battery_level);
                // UserError.Log.d(TAG, "Uploading battery detail: " + battery_level);
                // json.put("uploaderBattery", battery_level); // old style

                final JSONArray array = new JSONArray();
                final JSONObject json = new JSONObject();
                final JSONObject uploader = batteryType.getUploaderJson(mContext);

                if (uploader == null) {
                    continue;
                }

                json.put("device", batteryType.getDeviceName());
                json.put("uploader", uploader);

                array.put(json);

                // example
                //{
                //    "device": "openaps://ediscout2.local",
                //        "uploader": {
                //    "battery": 60,
                //            "batteryVoltage": 3783,
                //            "temperature": "+51.0°C"
                //}
                //}

                final RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
                Response<ResponseBody> r;
                if (apiSecret != null) {
                    r = nightscoutService.uploadDeviceStatus(apiSecret, body).execute();
                } else
                    r = nightscoutService.uploadDeviceStatus(body).execute();
                if (!r.isSuccessful()) throw new UploaderException(r.message(), r.code());
                // } else {
                //     UserError.Log.d(TAG, "Battery level is same as previous - not uploading: " + battery_level);
                checkGzipSupport(r);
            }
        }
    }


        private boolean doMongoUpload(SharedPreferences prefs, List<BgReading> glucoseDataSets,
                                      List<Calibration> meterRecords,  List<Calibration> calRecords, List<TransmitterData> transmittersData,
                                      List<LibreBlock> libreBlock) {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());

            final String dbURI = prefs.getString("cloud_storage_mongodb_uri", null);
            if (dbURI != null) {
                try {
                    final URI uri = new URI(dbURI.trim());
                    if ((uri.getHost().startsWith("192.168.")) && prefs.getBoolean("skip_lan_uploads_when_no_lan", true) && (!JoH.isLANConnected())) {
                        Log.d(TAG, "Skipping mongo upload to: " + dbURI + " due to no LAN connection");
                        return false;
                    }
                } catch (URISyntaxException e) {
                    UserError.Log.e(TAG, "Invalid mongo URI: " + e);
                }
            }

            final String collectionName = prefs.getString("cloud_storage_mongodb_collection", null);
            final String dsCollectionName = prefs.getString("cloud_storage_mongodb_device_status_collection", "devicestatus");

            if (dbURI != null && collectionName != null) {
                try {

                    // connect to db
                    MongoClientURI uri = new MongoClientURI(dbURI.trim()+"?socketTimeoutMS=180000");
                    MongoClient client = new MongoClient(uri);

                    // get db
                    DB db = client.getDB(uri.getDatabase());

                    // get collection
                    DBCollection dexcomData = db.getCollection(collectionName.trim());

                    try {
                        Log.i(TAG, "The number of EGV records being sent to MongoDB is " + glucoseDataSets.size());
                        for (BgReading record : glucoseDataSets) {
                            // make db object
                            BasicDBObject testData = new BasicDBObject();
                            testData.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
                            if (record != null) {//KS
                                testData.put("date", record.timestamp);
                                testData.put("dateString", format.format(record.timestamp));
                                testData.put("sgv", Math.round(record.calculated_value));
                                testData.put("direction", record.slopeName());
                                testData.put("type", "sgv");
                                testData.put("filtered", record.ageAdjustedFiltered() * 1000);
                                testData.put("unfiltered", record.usedRaw() * 1000);
                                testData.put("rssi", 100);
                                testData.put("noise", record.noiseValue());
                                dexcomData.insert(testData, WriteConcern.UNACKNOWLEDGED);
                            }
                            else
                                Log.e(TAG, "MongoDB BG record is null.");
                        }

                        Log.i(TAG, "REST - The number of MBG records being sent to MongoDB is " + meterRecords.size());
                        for (Calibration meterRecord : meterRecords) {
                            // make db object
                            BasicDBObject testData = new BasicDBObject();
                            testData.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
                            testData.put("type", "mbg");
                            testData.put("date", meterRecord.timestamp);
                            testData.put("dateString", format.format(meterRecord.timestamp));
                            testData.put("mbg", meterRecord.bg);
                            dexcomData.insert(testData, WriteConcern.UNACKNOWLEDGED);
                        }
                        Log.i(TAG, "REST - Finshed upload of mbg");

                        for (Calibration calRecord : calRecords) {
                            //do not upload undefined slopes
                            if(calRecord.slope == 0d) break;
                            // make db object
                            BasicDBObject testData = new BasicDBObject();
                            testData.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
                            testData.put("date", calRecord.timestamp);
                            testData.put("dateString", format.format(calRecord.timestamp));
                            if (calRecord.check_in) {
                                testData.put("slope", (calRecord.first_slope));
                                testData.put("intercept", ((calRecord.first_intercept)));
                                testData.put("scale", calRecord.first_scale);
                            } else {
                                testData.put("slope",  (1000/calRecord.slope));
                                testData.put("intercept", ((calRecord.intercept * -1000) / (calRecord.slope)));
                                testData.put("scale", 1);
                            }
                            testData.put("type", "cal");
                            dexcomData.insert(testData, WriteConcern.UNACKNOWLEDGED);
                        }
                        DBCollection libreCollection = db.getCollection("libre");
                        for (LibreBlock libreBlockEntry : libreBlock) {
                            
                            
                            Log.d(TAG, "uploading new item to mongo");
                            // Checksum might be wrong, for libre 2 or libre us 14 days.
                            boolean ChecksumOk = LibreUtils.verify(libreBlockEntry.blockbytes, libreBlockEntry.patchInfo);
                            
                            // make db object
                            BasicDBObject testData = new BasicDBObject();
                            testData.put("SensorId", PersistentStore.getString("LibreSN"));
                            testData.put("CaptureDateTime", libreBlockEntry.timestamp);
                            testData.put("BlockBytes",Base64.encodeToString(libreBlockEntry.blockbytes, Base64.NO_WRAP));
                            if(libreBlockEntry.patchUid != null && libreBlockEntry.patchUid.length != 0) {
                                testData.put("patchUid",Base64.encodeToString(libreBlockEntry.patchUid, Base64.NO_WRAP));
                            }
                            if(libreBlockEntry.patchInfo != null && libreBlockEntry.patchInfo.length != 0) {
                               testData.put("patchInfo",Base64.encodeToString(libreBlockEntry.patchInfo, Base64.NO_WRAP));
                            }
                            testData.put("ChecksumOk",ChecksumOk ? 1 : 0);
                            testData.put("Uploaded", 1);
                            testData.put("UploaderBatteryLife",getBatteryLevel());
                            testData.put("DebugInfo", android.os.Build.MODEL + " " + new Date(libreBlockEntry.timestamp).toLocaleString());
                            
                            try {
                                testData.put("TomatoBatteryLife", Integer.parseInt(PersistentStore.getString("Tomatobattery")));
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error reading battery daya" + PersistentStore.getString("Tomatobattery") );
                            }
                            testData.put("FwVersion", PersistentStore.getString("TomatoFirmware"));
                            testData.put("HwVersion", PersistentStore.getString("TomatoHArdware"));
                            
                            WriteResult wr = libreCollection.insert(testData, WriteConcern.ACKNOWLEDGED);
                            Log.d(TAG, "uploaded libreblock data with " + new Date(libreBlockEntry.timestamp).toLocaleString()+ " wr = " + wr);
                        }

                        // TODO: quick port from original code, revisit before release
                        DBCollection dsCollection = db.getCollection(dsCollectionName);
                        BasicDBObject devicestatus = new BasicDBObject();
                        devicestatus.put("uploaderBattery", getBatteryLevel());
                        devicestatus.put("created_at", format.format(System.currentTimeMillis()));
                        dsCollection.insert(devicestatus, WriteConcern.UNACKNOWLEDGED);

                        // treatments mongo sync using unified queue
                        Log.d(TAG,"Starting treatments mongo direct");
                        final long THIS_QUEUE = UploaderQueue.MONGO_DIRECT;
                        final DBCollection treatmentDb = db.getCollection("treatments");
                        final List<UploaderQueue> tups = UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), THIS_QUEUE);
                        if (tups != null) {
                            for (UploaderQueue up : tups) {
                                if ((up.action.equals("insert") || (up.action.equals("update")))) {
                                    Treatments treatment = Treatments.byid(up.reference_id);
                                    if (treatment != null) {
                                        BasicDBObject record = new BasicDBObject();
                                        record.put("timestamp", treatment.timestamp);
                                        record.put("eventType", treatment.eventType);
                                        record.put("enteredBy", treatment.enteredBy);
                                        if (treatment.notes != null) record.put("notes", treatment.notes);
                                        record.put("uuid", treatment.uuid);
                                        record.put("carbs", treatment.carbs);
                                        record.put("insulin", treatment.insulin);
                                        if (treatment.insulinJSON != null) {
                                            record.put("insulinInjections", treatment.insulinJSON);
                                        }
                                        record.put("created_at", treatment.created_at);
                                        final BasicDBObject searchQuery = new BasicDBObject().append("uuid", treatment.uuid);
                                        //treatmentDb.insert(record, WriteConcern.UNACKNOWLEDGED);
                                        Log.d(TAG, "Sending upsert for: " + treatment.toJSON());
                                        treatmentDb.update(searchQuery, record, true, false);
                                    } else {
                                        Log.d(TAG, "Got null for treatment id: " + up.reference_id);
                                    }
                                    up.completed(THIS_QUEUE);
                                } else if (up.action.equals("delete")) {
                                    if (up.reference_uuid != null) {
                                        Log.d(TAG,"Processing treatment delete mongo sync for: "+up.reference_uuid);
                                        final BasicDBObject searchQuery = new BasicDBObject().append("uuid", up.reference_uuid);
                                        Log.d(TAG,treatmentDb.remove(searchQuery, WriteConcern.UNACKNOWLEDGED).toString());

                                    }
                                    up.completed(THIS_QUEUE);
                                } else {
                                    Log.e(TAG, "Unsupported operation type for treatment: " + up.action);
                                }
                            }
                            Log.d(TAG, "Processed " + tups.size() + " Treatment mongo direct upload records");
                        }

                        client.close();

                        failurecount=0;
                        return true;

                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload data to mongo " + e.getMessage());
                        failurecount++;
                        if (failurecount>4)
                        {
                            Home.toaststaticnext("Mongo "+failurecount+" up fails: "+e.getMessage().substring(0,51));
                        }
                    } finally {
                        if(client != null) { client.close(); }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload data to mongo " + e.getMessage());
                }
            }
            return false;
        }

    public int getBatteryLevel() {
        return NightscoutBatteryDevice.PHONE.getBatteryLevel(mContext);
    }

    private static boolean isLANhost(String host) {
        return host != null && (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.16."));
    }

    private static final String END_SUPPORTS_GZIP_MARKER = "ns-end-supports-gzip-";

    private static boolean supportsGzip(String id) {
        return PersistentStore.getBoolean(END_SUPPORTS_GZIP_MARKER + id);
    }

    private static void setSupportsGzip(String id, boolean value) {
        if (supportsGzip(id) != value) {
            UserError.Log.e(TAG, "Setting GZIP support: " + id + " " + value);
            PersistentStore.setBoolean(END_SUPPORTS_GZIP_MARKER + id, value);
        }
    }

    /** Prints TLS Version and Cipher Suite for SSL Calls through OkHttp3 */
    public class SSLHandshakeInterceptor implements Interceptor {

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            final okhttp3.Response response = chain.proceed(chain.request());
            printTlsAndCipherSuiteInfo(response);
            return response;
        }

        private void printTlsAndCipherSuiteInfo(okhttp3.Response response) {
            if (response != null) {
                Handshake handshake = response.handshake();
                if (handshake != null) {
                    final CipherSuite cipherSuite = handshake.cipherSuite();
                    final TlsVersion tlsVersion = handshake.tlsVersion();
                    Log.v(TAG, "TLS: " + tlsVersion + ", CipherSuite: " + cipherSuite);
                }
            }
        }
    }

    static class GzipRequestInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            final Request originalRequest = chain.request();
            if (originalRequest.body() == null
                    || originalRequest.header("Content-Encoding") != null
                    || !supportsGzip(originalRequest.url().uri().getHost() + originalRequest.url().uri().getPort())) {
                return chain.proceed(originalRequest);
            }

            final Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), gzip(originalRequest.body()))
                    .build();
            return chain.proceed(compressedRequest);
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override public MediaType contentType() {
                    return body.contentType();
                }

                @Override public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override public void writeTo(BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }
}
