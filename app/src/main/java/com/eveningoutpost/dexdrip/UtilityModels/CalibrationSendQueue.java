package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.Calibration;

import java.util.List;

/**
 * Created by stephenblack on 11/7/14.
 */
@Table(name = "CalibrationSendQueue", id = BaseColumns._ID)
public class CalibrationSendQueue extends Model {

    @Column(name = "calibration", index = true)
    public Calibration calibration;

    @Column(name = "success", index = true)
    public boolean success;

    @Column(name = "mongo_success", index = true)
    public boolean mongo_success;

    public static CalibrationSendQueue nextCalibrationJob() {
        CalibrationSendQueue job = new Select()
                .from(CalibrationSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return job;
    }

    public static List<CalibrationSendQueue> queue() {
        return new Select()
                .from(CalibrationSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID asc")
                .execute();
    }
    public static List<CalibrationSendQueue> mongoQueue() {
        return new Select()
                .from(CalibrationSendQueue.class)
                .where("mongo_success = ?", false)
                .orderBy("_ID asc")
                .limit(10)
                .execute();
    }
    public static void addToQueue(Calibration calibration, Context context) {
        CalibrationSendQueue calibrationSendQueue = new CalibrationSendQueue();
        calibrationSendQueue.calibration = calibration;
        calibrationSendQueue.success = false;
        calibrationSendQueue.mongo_success = false;
        calibrationSendQueue.save();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean("cloud_storage_mongodb_enable", false) || prefs.getBoolean("cloud_storage_api_enable", false)) {
            MongoSendTask task = new MongoSendTask(context, calibrationSendQueue);
            task.execute();
        }
    }

    public void markMongoSuccess() {
        mongo_success = true;
        save();
    }
}
