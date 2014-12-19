package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;

import org.apache.http.Header;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * THIS CLASS WAS BUILT BY THE NIGHTSCOUT GROUP FOR THEIR NIGHTSCOUT ANDROID UPLOADER
 * https://github.com/nightscout/android-uploader/
 * I have modified this class to make it fit my needs
 * -Stephen Black
 */
public class NightscoutUploader {
        private static final String TAG = NightscoutUploader.class.getSimpleName();
        private static final int SOCKET_TIMEOUT = 60000;
        private static final int CONNECTION_TIMEOUT = 30000;
        private Context mContext;
        private Boolean enableRESTUpload;
        private Boolean enableMongoUpload;
        private SharedPreferences prefs;

        public NightscoutUploader(Context context) {
            mContext = context;
            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            enableRESTUpload = prefs.getBoolean("cloud_storage_api_enable", false);
            enableMongoUpload = prefs.getBoolean("cloud_storage_mongodb_enable", false);
        }

        public boolean upload(BgReading glucoseDataSet, Calibration meterRecord, Calibration calRecord) {
            BgReading[] glucoseDataSets = new BgReading[1];
            glucoseDataSets[0] = glucoseDataSet;
            Calibration[] meterRecords = new Calibration[1];
            meterRecords[0] = meterRecord;
            Calibration[] calRecords = new Calibration[1];
            calRecords[0] = calRecord;
            return upload(glucoseDataSets, meterRecords, calRecords);
        }

        public boolean upload(BgReading[] glucoseDataSets, Calibration[] meterRecords, Calibration[] calRecords) {

            boolean mongoStatus = false;
            boolean apiStatus = false;

            if (enableRESTUpload) {
                long start = System.currentTimeMillis();
                Log.i(TAG, String.format("Starting upload of %s record using a REST API", glucoseDataSets.length));
                apiStatus = doRESTUpload(prefs, glucoseDataSets, meterRecords, calRecords);
                Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms", glucoseDataSets.length, System.currentTimeMillis() - start));
            }

            if (enableMongoUpload) {
                long start = System.currentTimeMillis();
                Log.i(TAG, String.format("Starting upload of %s record using a Mongo", glucoseDataSets.length));
                mongoStatus = doMongoUpload(prefs, glucoseDataSets, meterRecords, calRecords);
                Log.i(TAG, String.format("Finished upload of %s record using a Mongo in %s ms", glucoseDataSets.length + meterRecords.length, System.currentTimeMillis() - start));
            }

            return apiStatus || mongoStatus;
        }

        private boolean doRESTUpload(SharedPreferences prefs, BgReading[] glucoseDataSets, Calibration[] meterRecords, Calibration[] calRecords) {
            String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
            ArrayList<String> baseURIs = new ArrayList<String>();

            try {
                for (String baseURLSetting : baseURLSettings.split(" ")) {
                    String baseURL = baseURLSetting.trim();
                    if (baseURL.isEmpty()) continue;
                    baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to process API Base URL setting: " + baseURLSettings, e);
                return false;
            }

            for (String baseURI : baseURIs) {
                try {
                    doRESTUploadTo(baseURI, glucoseDataSets, meterRecords, calRecords);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to do REST API Upload to: " + baseURI, e);
                    return false;
                }
            }
            return true;
        }

        private void doRESTUploadTo(String baseURI, BgReading[] glucoseDataSets, Calibration[] meterRecords, Calibration[] calRecords) {
            try {
                int apiVersion = 0;
                if (baseURI.endsWith("/v1/")) apiVersion = 1;

                String baseURL = null;
                String secret = null;
                String[] uriParts = baseURI.split("@");

                if (uriParts.length == 1 && apiVersion == 0) {
                    baseURL = uriParts[0];
                } else if (uriParts.length == 1 && apiVersion > 0) {
                    throw new Exception("Starting with API v1, a pass phase is required");
                } else if (uriParts.length == 2 && apiVersion > 0) {
                    secret = uriParts[0];
                    baseURL = uriParts[1];
                } else {
                    throw new Exception(String.format("Unexpected baseURI: %s, uriParts.length: %s, apiVersion: %s", baseURI, uriParts.length, apiVersion));
                }

                String postURL = baseURL + "entries";
                Log.i(TAG, "postURL: " + postURL);

                HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
                HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);

                DefaultHttpClient httpclient = new DefaultHttpClient(params);

                HttpPost post = new HttpPost(postURL);

                Header apiSecretHeader = null;

                if (apiVersion > 0) {
                    if (secret == null || secret.isEmpty()) {
                        throw new Exception("Starting with API v1, a pass phase is required");
                    } else {
                        MessageDigest digest = MessageDigest.getInstance("SHA-1");
                        byte[] bytes = secret.getBytes("UTF-8");
                        digest.update(bytes, 0, bytes.length);
                        bytes = digest.digest();
                        StringBuilder sb = new StringBuilder(bytes.length * 2);
                        for (byte b: bytes) {
                            sb.append(String.format("%02x", b & 0xff));
                        }
                        String token = sb.toString();
                        apiSecretHeader = new BasicHeader("api-secret", token);
                    }
                }

                if (apiSecretHeader != null) {
                    post.setHeader(apiSecretHeader);
                }

                for (BgReading record : glucoseDataSets) {
                    JSONObject json = new JSONObject();

                    try {
                        if (apiVersion >= 1)
                            populateV1APIBGEntry(json, record);
                        else
                            populateLegacyAPIEntry(json, record);
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to populate entry, apiVersion: " + apiVersion, e);
                        continue;
                    }

                    String jsonString = json.toString();

                    Log.i(TAG, "SGV JSON: " + jsonString);

                    try {
                        StringEntity se = new StringEntity(jsonString);
                        post.setEntity(se);
                        post.setHeader("Accept", "application/json");
                        post.setHeader("Content-type", "application/json");

                        ResponseHandler responseHandler = new BasicResponseHandler();
                        httpclient.execute(post, responseHandler);
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to post data to: '" + post.getURI().toString() + "'", e);
                    }
                }

                if (apiVersion >= 1) {
                    for (Calibration record : meterRecords) {
                        JSONObject json = new JSONObject();

                        try {
                            populateV1APIMeterReadingEntry(json, record);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to populate entry, apiVersion: " + apiVersion, e);
                            continue;
                        }

                        String jsonString = json.toString();
                        Log.i(TAG, "MBG JSON: " + jsonString);

                        try {
                            StringEntity se = new StringEntity(jsonString);
                            post.setEntity(se);
                            post.setHeader("Accept", "application/json");
                            post.setHeader("Content-type", "application/json");

                            ResponseHandler responseHandler = new BasicResponseHandler();
                            httpclient.execute(post, responseHandler);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to post data to: '" + post.getURI().toString() + "'", e);
                        }
                    }
                }

                if (apiVersion >= 1 && prefs.getBoolean("cloud_cal_data", false)) {
                    for (Calibration calRecord : calRecords) {

                        JSONObject json = new JSONObject();

                        try {
                            populateV1APICalibrationEntry(json, calRecord);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to populate entry, apiVersion: " + apiVersion, e);
                            continue;
                        }

                        String jsonString = json.toString();
                        Log.i(TAG, "CAL JSON: " + jsonString);

                        try {
                            StringEntity se = new StringEntity(jsonString);
                            post.setEntity(se);
                            post.setHeader("Accept", "application/json");
                            post.setHeader("Content-type", "application/json");

                            ResponseHandler responseHandler = new BasicResponseHandler();
                            httpclient.execute(post, responseHandler);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to post data to: '" + post.getURI().toString() + "'", e);
                        }
                    }
                }

                // TODO: this is a quick port from the original code and needs to be checked before release
                postDeviceStatus(baseURL, apiSecretHeader, httpclient);

            } catch (Exception e) {
                Log.e(TAG, "Unable to post data", e);
            }
        }

        private void populateV1APIBGEntry(JSONObject json, BgReading record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "dexcom");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", record.calculated_value);
            json.put("direction", "FLAT"); //TODO: get these values!
            json.put("type", "sgv");
            if (prefs.getBoolean("cloud_sensor_data", false)) {
                json.put("filtered", record.age_adjusted_raw_value); //TODO: change to actual filtered when I start storing it
                json.put("unfiltered", record.age_adjusted_raw_value);
                json.put("rssi", "100");
            }
        }

        private void populateLegacyAPIEntry(JSONObject json, BgReading record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "dexcom");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", record.calculated_value);
            json.put("direction", "FLAT"); // TODO: Send these values!
        }

        private void populateV1APIMeterReadingEntry(JSONObject json, Calibration record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "dexcom");
            json.put("type", "mbg");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("mbg", record.bg);
        }

        private void populateV1APICalibrationEntry(JSONObject json, Calibration record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());

            json.put("device", "dexcom");
            json.put("type", "cal");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("slope", (int)(record.slope * 1000));
            json.put("intercept", (int) record.intercept);
            json.put("scale", 1000);
        }

        // TODO: this is a quick port from original code and needs to be refactored before release
        private void postDeviceStatus(String baseURL, Header apiSecretHeader, DefaultHttpClient httpclient) throws Exception {
            String devicestatusURL = baseURL + "devicestatus";
            Log.i(TAG, "devicestatusURL: " + devicestatusURL);

            JSONObject json = new JSONObject();
            json.put("uploaderBattery", 100); //TODO: get the actual battery level
            String jsonString = json.toString();

            HttpPost post = new HttpPost(devicestatusURL);

            if (apiSecretHeader != null) {
                post.setHeader(apiSecretHeader);
            }

            StringEntity se = new StringEntity(jsonString);
            post.setEntity(se);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            ResponseHandler responseHandler = new BasicResponseHandler();
            httpclient.execute(post, responseHandler);
        }

        private boolean doMongoUpload(SharedPreferences prefs, BgReading[] glucoseDataSets,
                                      Calibration[] meterRecords, Calibration[] calRecords) {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());

            String dbURI = prefs.getString("cloud_storage_mongodb_uri", null);
            String collectionName = prefs.getString("cloud_storage_mongodb_collection", null);
            String dsCollectionName = prefs.getString("cloud_storage_mongodb_device_status_collection", "devicestatus");

            if (dbURI != null && collectionName != null) {
                try {

                    // connect to db
                    MongoClientURI uri = new MongoClientURI(dbURI.trim());
                    MongoClient client = new MongoClient(uri);

                    // get db
                    DB db = client.getDB(uri.getDatabase());

                    // get collection
                    DBCollection dexcomData = db.getCollection(collectionName.trim());
                    Log.i(TAG, "The number of EGV records being sent to MongoDB is " + glucoseDataSets.length);
                    for (BgReading record : glucoseDataSets) {
                        // make db object
                        BasicDBObject testData = new BasicDBObject();
                        testData.put("device", "dexcom");
                        testData.put("date", record.timestamp);
                        testData.put("dateString", format.format(record.timestamp));
                        testData.put("sgv", record.calculated_value);
                        testData.put("direction", "FLAT"); //TODO: get these values!
                        testData.put("type", "sgv");
                        if (prefs.getBoolean("cloud_sensor_data", false)) {
                            testData.put("filtered", record.age_adjusted_raw_value); //TODO: change to actual filtered when I start storing it
                            testData.put("unfiltered", record.age_adjusted_raw_value);
                            testData.put("rssi", "100");
                        }
                        dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                    }

                    Log.i(TAG, "The number of MBG records being sent to MongoDB is " + meterRecords.length);
                    for (Calibration meterRecord : meterRecords) {
                        // make db object
                        BasicDBObject testData = new BasicDBObject();
                        testData.put("device", "dexcom");
                        testData.put("type", "mbg");
                        testData.put("date", meterRecord.timestamp);
                        testData.put("dateString", format.format(meterRecord.timestamp));
                        testData.put("mbg", meterRecord.bg);
                        dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                    }

                    // TODO: might be best to merge with the the glucose data but will require time
                    // analysis to match record with cal set, for now this will do
                    if (prefs.getBoolean("cloud_cal_data", false)) {
                        for (Calibration calRecord : calRecords) {
                            // make db object
                            BasicDBObject testData = new BasicDBObject();
                            testData.put("device", "dexcom");
                            testData.put("date", calRecord.timestamp);
                            testData.put("dateString", format.format(calRecord.timestamp));
                            testData.put("slope", (int)(calRecord.slope * 1000));
                            testData.put("intercept", (int) calRecord.intercept);
                            testData.put("scale", 1000);
                            testData.put("type", "cal");
                            dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                        }
                    }

                    // TODO: quick port from original code, revisit before release
                    DBCollection dsCollection = db.getCollection(dsCollectionName);
                    BasicDBObject devicestatus = new BasicDBObject();
                    devicestatus.put("uploaderBattery", 100); //TODO: get actual battery level!!
                    devicestatus.put("created_at", new Date());
                    dsCollection.insert(devicestatus, WriteConcern.UNACKNOWLEDGED);

                    client.close();

                    return true;

                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload data to mongo", e);
                }
            }
            return false;
        }
}
