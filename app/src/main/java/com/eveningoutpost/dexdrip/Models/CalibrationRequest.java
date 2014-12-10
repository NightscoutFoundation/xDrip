package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.List;

/**
 * Created by stephenblack on 12/9/14.
 */

@Table(name = "CalibrationRequest", id = BaseColumns._ID)
public class CalibrationRequest extends Model {
    private static final int max = 250;
    private static final int min = 70;

    @Column(name = "requestIfAbove")
    public double requestIfAbove;

   @Column(name = "requestIfBelow")
    public double requestIfBelow;

    public static void createRange(double low, double high) {
        CalibrationRequest calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = low;
        calibrationRequest.requestIfBelow = high;
        calibrationRequest.save();
    }
    public static void createOffset(double center, double distance) {
        CalibrationRequest calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = center + distance;
        calibrationRequest.requestIfBelow = max;
        calibrationRequest.save();

        calibrationRequest.requestIfAbove = min;
        calibrationRequest.requestIfBelow = center - distance;
        calibrationRequest.save();
    }

    public static void clearAll(){
        List<CalibrationRequest> calibrationRequests =  new Select()
                                                            .from(CalibrationRequest.class)
                                                            .execute();
        if (calibrationRequests.size() >=1) {
            for (CalibrationRequest calibrationRequest : calibrationRequests) {
                calibrationRequest.delete();
            }
        }
    }

    public static boolean shouldRequestCalibration(BgReading bgReading){
        CalibrationRequest calibrationRequest =  new Select()
                .from(CalibrationRequest.class)
                .where("requestIfAbove < ?", bgReading.calculated_value)
                .where("requestIfBelow > ?", bgReading.calculated_value)
                .executeSingle();
        if (calibrationRequest != null && Math.abs(bgReading.calculated_value_slope * 60000) < 1.8) {
            return true;
        } else {
            return false;
        }
    }
}
