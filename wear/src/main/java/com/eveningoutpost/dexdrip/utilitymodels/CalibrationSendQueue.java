package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.models.Calibration;

import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */
@Table(name = "CalibrationSendQueue", id = BaseColumns._ID)
public class CalibrationSendQueue extends Model {
    private final static String TAG = CalibrationSendQueue.class.getSimpleName();

    @Column(name = "calibration", index = true)
    public Calibration calibration;

    @Column(name = "success", index = true)
    public boolean success;

    @Column(name = "mongo_success", index = true)
    public boolean mongo_success;

    /*
    public static List<CalibrationSendQueue> queue() {
        return new Select()
                .from(CalibrationSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID asc")
                .execute();
    }
    */
    public static List<CalibrationSendQueue> mongoQueue() {
        return new Select()
                .from(CalibrationSendQueue.class)
                .where("mongo_success = ?", false)
                .orderBy("_ID desc")
                .limit(20)
                .execute();
    }

    public static List<CalibrationSendQueue> cleanQueue() {
        return new Delete()
                .from(CalibrationSendQueue.class)
                .where("mongo_success = ?", true)
                .execute();
    }

    public static void addToQueue(Calibration calibration, Context context) {
        CalibrationSendQueue calibrationSendQueue = new CalibrationSendQueue();
        calibrationSendQueue.calibration = calibration;
        calibrationSendQueue.success = false;
        calibrationSendQueue.mongo_success = false;
        calibrationSendQueue.save();
    }

    public void markMongoSuccess() {
        mongo_success = true;
        save();
    }
}
