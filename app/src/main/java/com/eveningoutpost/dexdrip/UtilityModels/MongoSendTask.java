package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.os.AsyncTask;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Services.SyncService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stephenblack on 12/19/14.
 */
public class MongoSendTask extends AsyncTask<String, Void, Void> {
        private Context context;
        public List<BgSendQueue> bgsQueue = new ArrayList<BgSendQueue>();
        public List<CalibrationSendQueue> calibrationsQueue = new ArrayList<CalibrationSendQueue>();

        private Exception exception;
        private static final String TAG = MongoSendTask.class.getSimpleName();

        public MongoSendTask(Context pContext) {
            calibrationsQueue = CalibrationSendQueue.mongoQueue();
            bgsQueue = BgSendQueue.mongoQueue();
            context = pContext;
        }

    public Void doInBackground(String... urls) {
        try {
            List<BgReading> bgReadings = new ArrayList<BgReading>();
            List<Calibration> calibrations = new ArrayList<Calibration>();
            for (CalibrationSendQueue job : calibrationsQueue) {
                calibrations.add(job.calibration);
            }
            for (BgSendQueue job : bgsQueue) {
                bgReadings.add(job.bgReading);
            }
            if ((bgReadings.size() > 0) || (calibrations.size() > 0)
                    || (Home.getPreferencesBooleanDefaultFalse("cloud_storage_mongodb_enable")
                    && (UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), UploaderQueue.MONGO_DIRECT, 1).size() > 0))
                    || (Home.getPreferencesBooleanDefaultFalse("cloud_storage_api_enable")
                    && (UploaderQueue.getPendingbyType(Treatments.class.getSimpleName(), UploaderQueue.NIGHTSCOUT_RESTAPI, 1).size() > 0))) {
                Log.i(TAG, "uploader.upload called " + bgReadings.size());
                NightscoutUploader uploader = new NightscoutUploader(context);
                boolean uploadStatus = uploader.upload(bgReadings, calibrations, calibrations);
                if (uploadStatus) {
                    for (CalibrationSendQueue calibration : calibrationsQueue) {
                        calibration.markMongoSuccess();
                    }
                    for (BgSendQueue bgReading : bgsQueue) {
                        bgReading.markMongoSuccess();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "caught exception", e);
            this.exception = e;
            return null;
        }
        return null;
    }

//        protected void onPostExecute(RSSFeed feed) {
//            // TODO: check this.exception
//            // TODO: do something with the feed
//        }
    }
