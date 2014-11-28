package com.eveningoutpost.dexdrip.UtilityModels;

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


    public static CalibrationSendQueue nextCalibrationJob() {
        CalibrationSendQueue job = new Select()
                .from(CalibrationSendQueue.class)
                .where("success !=", true)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return job;
    }

    public static List<CalibrationSendQueue> queue() {
        return new Select()
                .from(CalibrationSendQueue.class)
                .where("success !=", true)
                .orderBy("_ID desc")
                .execute();
    }

    public static void addToQueue(Calibration calibration) {
        CalibrationSendQueue calibrationSendQueue = new CalibrationSendQueue();
        calibrationSendQueue.calibration = calibration;
        calibrationSendQueue.success = false;
        calibrationSendQueue.save();
    }
}