package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.POST;

/**
 * THIS CLASS WAS BUILT BY THE NIGHTSCOUT GROUP FOR THEIR NIGHTSCOUT ANDROID UPLOADER
 * https://github.com/nightscout/android-uploader/
 * I have modified this class to make it fit my needs
 * Modifications include field remappings and lists instead of arrays
 * A DTO would probably be a better future implementation
 * -Stephen Black
 */
public class NightscoutUploader {

        private static final String TAG = NightscoutUploader.class.getSimpleName();
        private static final int SOCKET_TIMEOUT = 60000;
        private static final int CONNECTION_TIMEOUT = 30000;

        private static int failurecount = 0;
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

            @POST("treatments")
            Call<ResponseBody> uploadTreatments(@Header("api-secret") String secret, @Body RequestBody body);

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
            client = new OkHttpClient();
            client.setConnectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            client.setWriteTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
            client.setReadTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
            enableRESTUpload = prefs.getBoolean("cloud_storage_api_enable", false);
            enableMongoUpload = prefs.getBoolean("cloud_storage_mongodb_enable", false);
        }

        public boolean upload(List<BgReading> glucoseDataSets, List<Calibration> meterRecords, List<Calibration> calRecords) {
            boolean mongoStatus = false;
            boolean apiStatus = false;

            if (enableRESTUpload) {
                long start = System.currentTimeMillis();
                Log.i(TAG, String.format("Starting upload of %s record using a REST API", glucoseDataSets.size()));
                apiStatus = doRESTUpload(prefs, glucoseDataSets, meterRecords, calRecords);
                Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms", glucoseDataSets.size(), System.currentTimeMillis() - start));
            }

            if (enableMongoUpload) {
                double start = new Date().getTime();
                mongoStatus = doMongoUpload(prefs, glucoseDataSets, meterRecords, calRecords);
                Log.i(TAG, String.format("Finished upload of %s record using a Mongo in %s ms", glucoseDataSets.size() + meterRecords.size(), System.currentTimeMillis() - start));
            }

            return apiStatus || mongoStatus;
        }

        private boolean doRESTUpload(SharedPreferences prefs, List<BgReading> glucoseDataSets, List<Calibration> meterRecords, List<Calibration> calRecords) {
            String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
            ArrayList<String> baseURIs = new ArrayList<String>();

            try {
                for (String baseURLSetting : baseURLSettings.split(" ")) {
                    String baseURL = baseURLSetting.trim();
                    if (baseURL.isEmpty()) continue;
                    baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to process API Base URL");
                return false;
            }

            for (String baseURI : baseURIs) {
                try {
                    int apiVersion = 0;
                    URI uri = new URI(baseURI);
                    if ((uri.getHost().startsWith("192.168.")) && (!JoH.isLANConnected()))
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
                        throw new Exception("Unexpected baseURI");
                    }

                    Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURL).client(client).build();
                    NightscoutService nightscoutService = retrofit.create(NightscoutService.class);

                    if (apiVersion == 1) {
                        String hashedSecret = Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();
                        doRESTUploadTo(nightscoutService, hashedSecret, glucoseDataSets, meterRecords, calRecords);
                    } else {
                        doLegacyRESTUploadTo(nightscoutService, glucoseDataSets);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unable to do REST API Upload " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }

        private void doLegacyRESTUploadTo(NightscoutService nightscoutService, List<BgReading> glucoseDataSets) throws Exception {
            for (BgReading record : glucoseDataSets) {
                Response<ResponseBody> r = nightscoutService.upload(populateLegacyAPIEntry(record)).execute();
                if (!r.isSuccess()) throw new UploaderException(r.message(), r.code());

            }
            postDeviceStatus(nightscoutService, null);
        }

        private void doRESTUploadTo(NightscoutService nightscoutService, String secret, List<BgReading> glucoseDataSets, List<Calibration> meterRecords, List<Calibration> calRecords) throws Exception {
            JSONArray array = new JSONArray();

            for (BgReading record : glucoseDataSets) {
                populateV1APIBGEntry(array, record);
            }
            for (Calibration record : meterRecords) {
                populateV1APIMeterReadingEntry(array, record);
            }
            for (Calibration record : calRecords) {
                populateV1APICalibrationEntry(array, record);
            }

            if (array.length() > 0) {//KS
                RequestBody body = RequestBody.create(MediaType.parse("application/json"), array.toString());
                Response<ResponseBody> r = nightscoutService.upload(secret, body).execute();
                if (!r.isSuccess()) throw new UploaderException(r.message(), r.code());

                postDeviceStatus(nightscoutService, secret);
            }
                postTreatments(nightscoutService,secret);
        }

    private void populateV1APIBGEntry(JSONArray array, BgReading record) throws Exception {
        JSONObject json = new JSONObject();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        json.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
        if (record != null) {//KS
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", (int) record.calculated_value);
            json.put("direction", record.slopeName());
            json.put("type", "sgv");
            json.put("filtered", record.ageAdjustedFiltered() * 1000);
            json.put("unfiltered", record.usedRaw() * 1000);
            json.put("rssi", 100);
            json.put("noise", record.noiseValue());
            json.put("delta", new BigDecimal(record.currentSlope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP)); // jamorham for automation
            array.put(json);
        }
        else
            Log.e(TAG, "doRESTUploadTo BG record is null.");
    }

        private RequestBody populateLegacyAPIEntry(BgReading record) throws Exception {
            JSONObject json = new JSONObject();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", (int)record.calculated_value);
            json.put("direction", record.slopeName());
            return RequestBody.create(MediaType.parse("application/json"), json.toString());
        }

        private void populateV1APIMeterReadingEntry(JSONArray array, Calibration record) throws Exception {
            JSONObject json = new JSONObject();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("type", "mbg");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("mbg", record.bg);
            array.put(json);
        }

        private void populateV1APICalibrationEntry(JSONArray array, Calibration record) throws Exception {

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
            array.put(json);
        }

    private void populateV1APITreatmentEntry(JSONArray array, Treatments treatment) throws Exception {

        if (treatment == null) return;
        final JSONObject record = new JSONObject();
        record.put("timestamp", treatment.timestamp);
        record.put("eventType", treatment.eventType);
        record.put("enteredBy", treatment.enteredBy);
        record.put("notes", treatment.notes);
        record.put("uuid", treatment.uuid);
        record.put("carbs", treatment.carbs);
        record.put("insulin", treatment.insulin);
        record.put("created_at", treatment.created_at);
        array.put(record);
    }

    private void postTreatments(NightscoutService nightscoutService, String apiSecret) throws Exception {
        Log.d(TAG, "Processing treatments for RESTAPI");
        final long THIS_QUEUE = UploaderQueue.NIGHTSCOUT_RESTAPI;
        final List<UploaderQueue> tups = UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), THIS_QUEUE);
        if (tups != null) {
            JSONArray array = new JSONArray();
            for (UploaderQueue up : tups) {
                if ((up.action.equals("insert") || (up.action.equals("update")))) {
                    Treatments treatment = Treatments.byid(up.reference_id);
                    populateV1APITreatmentEntry(array, treatment);
                } else if (up.action.equals("delete")) {
                    if (up.reference_uuid != null) {
                        Log.d(TAG, "Cannot delete treatment using REST-API: " + up.reference_uuid);
                        up.completed(THIS_QUEUE); // mark as completed so as not to tie up the queue for now
                    }
                } else {
                    Log.e(TAG, "Unsupported operation type for treatment: " + up.action);
                }
            }
            if (array.length() == 0) return;
            final RequestBody body = RequestBody.create(MediaType.parse("application/json"), array.toString());
            final Response<ResponseBody> r;
            if (apiSecret != null) {
                r = nightscoutService.uploadTreatments(apiSecret, body).execute();
                if (!r.isSuccess()) {
                    throw new UploaderException(r.message(), r.code());
                } else {
                    Log.d(TAG, "Success for RESTAPI treatment upload");
                    for (UploaderQueue up : tups) {
                        up.completed(THIS_QUEUE); // approve all types for this queue
                    }
                }
            } else {
                Log.wtf(TAG, "Cannot upload treatments without api secret being set");
            }
        }
    }

        private void postDeviceStatus(NightscoutService nightscoutService, String apiSecret) throws Exception {
            JSONObject json = new JSONObject();
            json.put("uploaderBattery", getBatteryLevel());
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
            Response<ResponseBody> r;
            if (apiSecret != null) {
                r = nightscoutService.uploadDeviceStatus(apiSecret, body).execute();
            } else
                r = nightscoutService.uploadDeviceStatus(body).execute();
            if (!r.isSuccess()) throw new UploaderException(r.message(), r.code());
        }

        private boolean doMongoUpload(SharedPreferences prefs, List<BgReading> glucoseDataSets,
                                      List<Calibration> meterRecords,  List<Calibration> calRecords) {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            format.setTimeZone(TimeZone.getDefault());

            final String dbURI = prefs.getString("cloud_storage_mongodb_uri", null);
            if (dbURI != null) {
                try {
                    final URI uri = new URI(dbURI.trim());
                    if ((uri.getHost().startsWith("192.168.")) && (!JoH.isLANConnected())) {
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

                        Log.i(TAG, "The number of MBG records being sent to MongoDB is " + meterRecords.size());
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
                                        record.put("notes", treatment.notes);
                                        record.put("uuid", treatment.uuid);
                                        record.put("carbs", treatment.carbs);
                                        record.put("insulin", treatment.insulin);
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
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level == -1 || scale == -1) {
                return 50;
            }
            return (int) (((float) level / (float) scale) * 100.0f);
        } else return 50;
    }
}
