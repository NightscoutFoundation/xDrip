package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.os.AsyncTask;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.InfluxDB.InfluxDBUploader;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emma Black on 12/19/14.
 */

// TODO unify treatment handling
// TODO refactor with more appropriate name

public class MongoSendTask extends AsyncTask<String, Void, Void> {
    public static Exception exception;
    private static final String TAG = MongoSendTask.class.getSimpleName();

    public MongoSendTask(Context pContext) {
    }

    public Void doInBackground(String... urls) {
        try {
            final List<Long> circuits = new ArrayList<>();
            final List<String> types = new ArrayList<>();

            types.add(BgReading.class.getSimpleName());
            types.add(Calibration.class.getSimpleName());

            if (Home.getPreferencesBooleanDefaultFalse("cloud_storage_mongodb_enable")) {
                circuits.add(UploaderQueue.MONGO_DIRECT);
            }
            if (Home.getPreferencesBooleanDefaultFalse("cloud_storage_api_enable")) {
                circuits.add(UploaderQueue.NIGHTSCOUT_RESTAPI);
            }
            if (Home.getPreferencesBooleanDefaultFalse("cloud_storage_influxdb_enable")) {
                circuits.add(UploaderQueue.INFLUXDB_RESTAPI);
            }


            for (long THIS_QUEUE : circuits) {

                final List<BgReading> bgReadings = new ArrayList<>();
                final List<Calibration> calibrations = new ArrayList<>();
                final List<UploaderQueue> items = new ArrayList<>();

                for (String type : types) {
                    final List<UploaderQueue> bgups = UploaderQueue.getPendingbyType(type, THIS_QUEUE);
                    if (bgups != null) {
                        for (UploaderQueue up : bgups) {
                            switch (up.action) {
                                case "insert":
                                case "update":
                                case "create":
                                    items.add(up);
                                    if (type.equals(BgReading.class.getSimpleName())) {
                                        bgReadings.add(BgReading.byid(up.reference_id));
                                    } else if (type.equals(Calibration.class.getSimpleName())) {
                                        calibrations.add(Calibration.byid(up.reference_id));
                                    }
                                case "delete":
                                    // items.add(up);
                                    if (up.reference_uuid != null) {
                                        Log.d(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " delete not yet implemented: " + up.reference_uuid);
                                        up.completed(THIS_QUEUE); // mark as completed so as not to tie up the queue for now
                                    }
                                    break;
                                default:
                                    Log.e(TAG, "Unsupported operation type for " + type + " " + up.action);
                                    break;
                            }
                        }
                    }
                }

                if ((bgReadings.size() > 0) || (calibrations.size() > 0)
                        || (UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), THIS_QUEUE, 1).size() > 0)) {

                    Log.d(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " Processing: " + bgReadings.size() + " BgReadings and " + calibrations.size() + " Calibrations");
                    boolean uploadStatus = false;

                    if (THIS_QUEUE == UploaderQueue.MONGO_DIRECT) {
                        final NightscoutUploader uploader = new NightscoutUploader(xdrip.getAppContext());
                        uploadStatus = uploader.uploadMongo(bgReadings, calibrations, calibrations);
                    } else if (THIS_QUEUE == UploaderQueue.NIGHTSCOUT_RESTAPI) {
                        final NightscoutUploader uploader = new NightscoutUploader(xdrip.getAppContext());
                        uploadStatus = uploader.uploadRest(bgReadings, calibrations, calibrations);
                    } else if (THIS_QUEUE == UploaderQueue.INFLUXDB_RESTAPI) {
                        final InfluxDBUploader influxDBUploader = new InfluxDBUploader(xdrip.getAppContext());;
                        uploadStatus = influxDBUploader.upload(bgReadings, calibrations, calibrations);
                    }

                    // TODO some kind of fail counter?
                    if (uploadStatus) {
                        for (UploaderQueue up : items) {
                            up.completed(THIS_QUEUE); // approve all types for this queue
                        }
                        Log.d(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " Marking: " + items.size() + " Items as successful");
                    }

                } else {
                    Log.d(TAG, "Nothing to upload for: " + UploaderQueue.getCircuitName(THIS_QUEUE));
                }

            }
        } catch (Exception e) {
            Log.e(TAG, "caught exception", e);
            exception = e;
            return null;
        }
        return null;
    }

}
