package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
            List<BgReading> glucoseDataSets = new ArrayList<BgReading>();
            glucoseDataSets.add(glucoseDataSet);
            List<Calibration> meterRecords = new ArrayList<Calibration>();
            meterRecords.add(meterRecord);
            List<Calibration> calRecords = new ArrayList<Calibration>();
            calRecords.add(calRecord);
            return upload(glucoseDataSets, meterRecords, calRecords);
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
                    doRESTUploadTo(baseURI, glucoseDataSets, meterRecords, calRecords);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to do REST API Upload");
                    return false;
                }
            }
            return true;
        }

        private void doRESTUploadTo(String baseURI, List<BgReading> glucoseDataSets, List<Calibration> meterRecords, List<Calibration> calRecords) {
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
                    throw new Exception("Unexpected baseURI");
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
                        Log.w(TAG, "Unable to populate entry");
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
                        Log.w(TAG, "Unable to populate entry");
                    }
                }

                if (apiVersion >= 1) {
                    for (Calibration record : meterRecords) {
                        JSONObject json = new JSONObject();

                        try {
                            populateV1APIMeterReadingEntry(json, record);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to populate entry");
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
                            Log.w(TAG, "Unable to post data");
                        }
                    }
                }

                if (apiVersion >= 1) {
                    for (Calibration calRecord : calRecords) {

                        JSONObject json = new JSONObject();

                        try {
                            populateV1APICalibrationEntry(json, calRecord);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to populate entry");
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
                            Log.w(TAG, "Unable to post data");
                        }
                    }
                }

                // TODO: this is a quick port from the original code and needs to be checked before release
                postDeviceStatus(baseURL, apiSecretHeader, httpclient);

            } catch (Exception e) {
                Log.w(TAG, "Unable to post data");
            }
        }

        private void populateV1APIBGEntry(JSONObject json, BgReading record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", (int)record.calculated_value);
            json.put("direction", record.slopeName());
            json.put("type", "sgv");
            json.put("filtered", record.filtered_data * 1000);
            json.put("unfiltered", record.age_adjusted_raw_value * 1000);
            json.put("rssi", 100);
            json.put("noise", Integer.valueOf(record.noiseValue()));
        }

        private void populateLegacyAPIEntry(JSONObject json, BgReading record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("sgv", (int)record.calculated_value);
            json.put("direction", record.slopeName());
        }

        private void populateV1APIMeterReadingEntry(JSONObject json, Calibration record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("type", "mbg");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            json.put("mbg", record.bg);
        }

        private void populateV1APICalibrationEntry(JSONObject json, Calibration record) throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
            format.setTimeZone(TimeZone.getDefault());
            json.put("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"));
            json.put("type", "cal");
            json.put("date", record.timestamp);
            json.put("dateString", format.format(record.timestamp));
            if(record.check_in) {
                json.put("slope", (long) (record.first_slope));
                json.put("intercept", (long) ((record.first_intercept)));
                json.put("scale", record.first_scale);
            } else {
                json.put("slope", (long) (record.slope * 1000));
                json.put("intercept", (long) ((record.intercept * -1000) / (record.slope * 1000)));
                json.put("scale", 1);
            }
        }

        // TODO: this is a quick port from original code and needs to be refactored before release
        private void postDeviceStatus(String baseURL, Header apiSecretHeader, DefaultHttpClient httpclient) throws Exception {
            String devicestatusURL = baseURL + "devicestatus";
            Log.i(TAG, "devicestatusURL: " + devicestatusURL);

            JSONObject json = new JSONObject();
            json.put("uploaderBattery", getBatteryLevel());
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

        private boolean doMongoUpload(SharedPreferences prefs, List<BgReading> glucoseDataSets,
                                      List<Calibration> meterRecords,  List<Calibration> calRecords) {
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
                    Log.i(TAG, "The number of EGV records being sent to MongoDB is " + glucoseDataSets.size());
                    for (BgReading record : glucoseDataSets) {
                        // make db object
                        BasicDBObject testData = new BasicDBObject();
                        testData.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
                        testData.put("date", record.timestamp);
                        testData.put("dateString", format.format(record.timestamp));
                        testData.put("sgv", Math.round(record.calculated_value));
                        testData.put("direction", record.slopeName());
                        testData.put("type", "sgv");
                        testData.put("filtered", record.filtered_data * 1000);
                        testData.put("unfiltered", record.age_adjusted_raw_value * 1000 );
                        testData.put("rssi", 100);
                        testData.put("noise", Integer.valueOf(record.noiseValue()));
                        dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                    }

                    Log.i(TAG, "The number of MBG records being sent to MongoDB is " + meterRecords.size());
                    for (Calibration meterRecord : meterRecords) {
                        // make db object
                        BasicDBObject testData = new BasicDBObject();
                        testData.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
                        testData.put("type", "mbg");
                        testData.put("date", meterRecord.timestamp);
                        testData.put("dateString", format.format(meterRecord.timestamp));
                        testData.put("mbg", meterRecord.bg);
                        dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                    }

                    for (Calibration calRecord : calRecords) {
                        // make db object
                        BasicDBObject testData = new BasicDBObject();
                        testData.put("device", "xDrip-"+prefs.getString("dex_collection_method", "BluetoothWixel"));
                        testData.put("date", calRecord.timestamp);
                        testData.put("dateString", format.format(calRecord.timestamp));
                        if(calRecord.check_in) {
                            testData.put("slope", (long) (calRecord.first_slope));
                            testData.put("intercept", (long) ((calRecord.first_intercept)));
                            testData.put("scale", calRecord.first_scale);
                        } else {
                            testData.put("slope", (long) (calRecord.slope * 1000));
                            testData.put("intercept", (long) ((calRecord.intercept * -1000) / (calRecord.slope * 1000)));
                            testData.put("scale", 1);
                        }
                        testData.put("type", "cal");
                        dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                    }

                    // TODO: quick port from original code, revisit before release
                    DBCollection dsCollection = db.getCollection(dsCollectionName);
                    BasicDBObject devicestatus = new BasicDBObject();
                    devicestatus.put("uploaderBattery", getBatteryLevel());
                    devicestatus.put("created_at", new Date());
                    dsCollection.insert(devicestatus, WriteConcern.UNACKNOWLEDGED);

                    client.close();

                    return true;

                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload data to mongo");
                }
            }
            return false;
        }
    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) {
            return 50;
        }
        return (int)(((float)level / (float)scale) * 100.0f);
    }
}
