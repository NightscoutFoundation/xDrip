package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.AsyncTask;

import com.eveningoutpost.dexdrip.influxdb.InfluxDBUploader;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emma Black on 12/19/14.
 */

// TODO unify treatment handling


public class UploaderTask extends AsyncTask<String, Void, Void> {
    public static Exception exception;
    private static final String TAG = UploaderTask.class.getSimpleName();
    public static final String BACKFILLING_BOOSTER = "backfilling-nightscout";
    private static final boolean retry_timer = false;


    public Void doInBackground(String... urls) {
        try {
            Log.d(TAG, "UploaderTask doInBackground called");
            final List<Long> circuits = new ArrayList<>();
            final List<String> types = new ArrayList<>();

            types.add(BgReading.class.getSimpleName());
            types.add(Calibration.class.getSimpleName());
            types.add(BloodTest.class.getSimpleName());
            types.add(Treatments.class.getSimpleName());
            types.add(TransmitterData.class.getSimpleName());
            types.add(LibreBlock.class.getSimpleName());

            if (Pref.getBooleanDefaultFalse("wear_sync")) {
                circuits.add(UploaderQueue.WATCH_WEARAPI);
            }

            if (Pref.getBooleanDefaultFalse("cloud_storage_mongodb_enable")) {
                circuits.add(UploaderQueue.MONGO_DIRECT);
            }
            if (Pref.getBooleanDefaultFalse("cloud_storage_api_enable")) {
                if ((Pref.getBoolean("cloud_storage_api_use_mobile", true) || (JoH.isLANConnected()))) {
                    circuits.add(UploaderQueue.NIGHTSCOUT_RESTAPI);
                } else {
                    Log.e(TAG, "Skipping Nightscout upload due to mobile data only");
                }
            }
            if (Pref.getBooleanDefaultFalse("cloud_storage_influxdb_enable")) {
                circuits.add(UploaderQueue.INFLUXDB_RESTAPI);
            }


            for (long THIS_QUEUE : circuits) {

                final List<BgReading> bgReadings = new ArrayList<>();
                final List<Calibration> calibrations = new ArrayList<>();
                final List<BloodTest> bloodtests = new ArrayList<>();
                final List<Treatments> treatmentsAdd = new ArrayList<>();
                final List<String> treatmentsDel = new ArrayList<>();
                final List<TransmitterData> transmittersData = new ArrayList<>();
                final List<LibreBlock> libreBlock = new ArrayList<>();
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
                                        final BgReading this_bg = BgReading.byid(up.reference_id);
                                        if (this_bg != null) {
                                            bgReadings.add(this_bg);
                                        } else {
                                            Log.wtf(TAG, "BgReading with ID: " + up.reference_id + " appears to have been deleted");
                                        }
                                    } else if (type.equals(Calibration.class.getSimpleName())) {
                                        final Calibration this_cal = Calibration.byid(up.reference_id);
                                        if (this_cal != null) {
                                            if (this_cal.isValid()) {
                                                calibrations.add(this_cal);
                                            } else {
                                                Log.d(TAG, "Calibration with ID: " + up.reference_id + " is marked invalid");
                                            }
                                        } else {
                                            Log.wtf(TAG, "Calibration with ID: " + up.reference_id + " appears to have been deleted");
                                        }

                                    } else if (type.equals(BloodTest.class.getSimpleName())) {
                                        final BloodTest this_bt = BloodTest.byid(up.reference_id);
                                        if (this_bt != null) {
                                            bloodtests.add(this_bt);
                                        } else {
                                            Log.wtf(TAG, "Bloodtest with ID: " + up.reference_id + " appears to have been deleted");
                                        }
                                    } else if (type.equals(Treatments.class.getSimpleName())) {
                                        final Treatments this_treat = Treatments.byid(up.reference_id);
                                        if (this_treat != null) {
                                            treatmentsAdd.add(this_treat);
                                        } else {
                                            Log.wtf(TAG, "Treatments with ID: " + up.reference_id + " appears to have been deleted");
                                        }
                                    } else if (type.equals(TransmitterData.class.getSimpleName())) {
                                        final TransmitterData this_transmitterData = TransmitterData.byid(up.reference_id);
                                        if (this_transmitterData != null) {
                                            transmittersData.add(this_transmitterData);
                                        } else {
                                            Log.wtf(TAG, "TransmitterData with ID: " + up.reference_id + " appears to have been deleted");
                                        }
                                    } else if (type.equals(LibreBlock.class.getSimpleName())) {
                                        final LibreBlock this_LibreBlock = LibreBlock.byid(up.reference_id);
                                        if (this_LibreBlock != null) {
                                            libreBlock.add(this_LibreBlock);
                                        } else {
                                            Log.wtf(TAG, "LibreBlock with ID: " + up.reference_id + " appears to have been deleted");
                                        }
                                    }
                                    break;
                                case "delete":
                                    if ((THIS_QUEUE == UploaderQueue.WATCH_WEARAPI || THIS_QUEUE == UploaderQueue.NIGHTSCOUT_RESTAPI) && type.equals(Treatments.class.getSimpleName())) {
                                        items.add(up);
                                        Log.wtf(TAG, "Delete Treatments with ID: " + up.reference_uuid);
                                        treatmentsDel.add(up.reference_uuid);
                                    } else if (up.reference_uuid != null) {
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

                if ((bgReadings.size() > 0) || (calibrations.size() > 0) || (bloodtests.size() > 0)
                        || (treatmentsAdd.size() > 0 || treatmentsDel.size() > 0) || (transmittersData.size() > 0) ||
                        (libreBlock.size() > 0)
                        || (UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), THIS_QUEUE, 1).size() > 0)) {

                    Log.d(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " Processing: " + bgReadings.size() + " BgReadings and " + calibrations.size() + " Calibrations " + bloodtests.size() + " bloodtests " + treatmentsAdd.size() + " treatmentsAdd " + treatmentsDel.size() + " treatmentsDel");
                    boolean uploadStatus = false;

                    if (THIS_QUEUE == UploaderQueue.MONGO_DIRECT) {
                        final NightscoutUploader uploader = new NightscoutUploader(xdrip.getAppContext());
                        uploadStatus = uploader.uploadMongo(bgReadings, calibrations, calibrations, transmittersData, libreBlock);
                    } else if (THIS_QUEUE == UploaderQueue.NIGHTSCOUT_RESTAPI) {
                        final NightscoutUploader uploader = new NightscoutUploader(xdrip.getAppContext());
                        uploadStatus = uploader.uploadRest(bgReadings, bloodtests, calibrations);
                    } else if (THIS_QUEUE == UploaderQueue.INFLUXDB_RESTAPI) {
                        final InfluxDBUploader influxDBUploader = new InfluxDBUploader(xdrip.getAppContext());
                        uploadStatus = influxDBUploader.upload(bgReadings, calibrations, calibrations);
                    } else if (THIS_QUEUE == UploaderQueue.WATCH_WEARAPI) {
                        uploadStatus = WatchUpdaterService.sendWearUpload(bgReadings, calibrations, bloodtests, treatmentsAdd, treatmentsDel);
                    }

                    if (retry_timer) {
                        SyncService.startSyncService(Constants.MINUTE_IN_MS * 6); // standard retry timer
                    }

                    // TODO some kind of fail counter?
                    if (uploadStatus) {
                        for (UploaderQueue up : items) {
                            up.completed(THIS_QUEUE); // approve all types for this queue
                        }
                        Log.d(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " Marking: " + items.size() + " Items as successful");

                        if (PersistentStore.getBoolean(BACKFILLING_BOOSTER)) {
                            Log.d(TAG, "Scheduling boosted repeat query");
                            SyncService.startSyncService(2000);
                        }

                    }


                } else {
                    Log.d(TAG, "Nothing to upload for: " + UploaderQueue.getCircuitName(THIS_QUEUE));
                    if (PersistentStore.getBoolean(BACKFILLING_BOOSTER)) {
                        PersistentStore.setBoolean(BACKFILLING_BOOSTER, false);
                        Log.d(TAG, "Switched off backfilling booster");
                    }
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
