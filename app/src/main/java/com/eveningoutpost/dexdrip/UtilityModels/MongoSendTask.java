package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Services.SyncService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stephenblack on 12/19/14.
 */
public class MongoSendTask extends AsyncTask<String, Void, SyncService> {
        private Context context;
        public List<BgSendQueue> bgsQueue = new ArrayList<BgSendQueue>();
        public List<CalibrationSendQueue> calibrationsQueue = new ArrayList<CalibrationSendQueue>();

        private Exception exception;

        public MongoSendTask(Context pContext, BgSendQueue bgSendQueue) {
            bgsQueue.add(bgSendQueue);
            context = pContext;
        }
        public MongoSendTask(Context pContext, CalibrationSendQueue calibrationSendQueue) {
            calibrationsQueue.add(calibrationSendQueue);
            context = pContext;
        }
        public MongoSendTask(Context pContext) {
            calibrationsQueue = CalibrationSendQueue.mongoQueue();
            bgsQueue = BgSendQueue.mongoQueue();
            context = pContext;
        }

        public SyncService doInBackground(String... urls) {
            try {
                List<BgReading> bgReadings = new ArrayList<BgReading>();
                List<Calibration> calibrations = new ArrayList<Calibration>();
                for (CalibrationSendQueue job : calibrationsQueue) {
                    calibrations.add(job.calibration);
                }
                for (BgSendQueue job : bgsQueue) {
                    bgReadings.add(job.bgReading);
                }

                if(bgReadings.size() + calibrations.size() > 0) {
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
                this.exception = e;
                return null;
            }
            return new SyncService();
        }

//        protected void onPostExecute(RSSFeed feed) {
//            // TODO: check this.exception
//            // TODO: do something with the feed
//        }
    }
