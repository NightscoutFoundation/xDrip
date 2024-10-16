package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.AsyncTask;
import android.os.Build;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.Arrays;
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
            Log.e(TAG, "UploaderTask doInBackground called");
            final List<Long> circuits = new ArrayList<>();
            final List<String> types = new ArrayList<>();

            types.add(BgReading.class.getSimpleName());
            circuits.add(UploaderQueue.NIGHTSCOUT_RESTAPI);

            for (long THIS_QUEUE : circuits) {

                final List<BgReading> bgReadings = new ArrayList<>();
                final List<UploaderQueue> items = new ArrayList<>();

                for (String type : types) {
                    final List<UploaderQueue> bgups = UploaderQueue.getPendingbyType(type, THIS_QUEUE);
                    if (bgups != null) {
                        for (UploaderQueue up : bgups) {
                            if (up == null) {
                                Log.e(TAG, "Queue entry is empty - skip it");
                                continue;
                            }
                            Log.d(TAG, "Process queue: " +up.toString() + "with action: " + up.action);
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
                                    }
                                    break;
                                case "delete":
                                    if (up.reference_uuid != null) {
                                        Log.e(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " delete not yet implemented: " + up.reference_uuid);
                                        up.completed(THIS_QUEUE); // mark as completed so as not to tie up the queue for now
                                    }
                                    break;
                                default:
                                    Log.e(TAG, "Unsupported operation type for " + type + " " + up.action);
                                    break;
                            }
                        }
                    }
                    else {
                        Log.d(TAG, "No pending queue entries for type " + type);
                    }
                }

                Log.d(TAG, "Amount of collected bgreadings: " + bgReadings.size());
                if (bgReadings.size() > 0) {

                    Log.e(TAG, UploaderQueue.getCircuitName(THIS_QUEUE) + " Processing: " + bgReadings.size() + " BgReadings");
                    boolean uploadStatus = false;

                    if (THIS_QUEUE == UploaderQueue.NIGHTSCOUT_RESTAPI) {
                        Log.d(TAG, "Start nightscout uploader!");
                        final NightscoutUploader uploader = new NightscoutUploader(xdrip.getAppContext());
                        uploadStatus = uploader.uploadRest(bgReadings, new ArrayList<>(), new ArrayList<>());
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
            Log.e(TAG, "Stack: "+ Arrays.toString(e.getStackTrace()));
            exception = e;
            return null;
        }
        return null;
    }

}
