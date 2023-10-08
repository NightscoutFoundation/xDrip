package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Home;

import java.util.List;

/**
 * Created by Emma Black on 12/9/14.
 */

@Table(name = "CalibrationRequest", id = BaseColumns._ID)
public class CalibrationRequest extends Model {
    private static final int max = 250;
    private static final int min = 70;
    private static final String TAG = CalibrationRequest.class.getSimpleName();

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
    static void createOffset(double center, double distance) {
        CalibrationRequest calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = center + distance;
        calibrationRequest.requestIfBelow = max;
        calibrationRequest.save();

        calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = min;
        calibrationRequest.requestIfBelow = center - distance;
        calibrationRequest.save();
    }

    static void clearAll(){
        List<CalibrationRequest> calibrationRequests =  new Select()
                                                            .from(CalibrationRequest.class)
                                                            .execute();
        if (calibrationRequests.size() >=1) {
            for (CalibrationRequest calibrationRequest : calibrationRequests) {
                calibrationRequest.delete();
            }
        }
    }

    public static boolean shouldRequestCalibration(BgReading bgReading) {
        CalibrationRequest calibrationRequest = new Select()
                .from(CalibrationRequest.class)
                .where("requestIfAbove < ?", bgReading.calculated_value)
                .where("requestIfBelow > ?", bgReading.calculated_value)
                .executeSingle();
        return (calibrationRequest != null && isSlopeFlatEnough(bgReading, 1));
    }

    public static boolean isSlopeFlatEnough() {
        BgReading bgReading = BgReading.last(true);
        if (bgReading == null) return false;
        if (JoH.msSince(bgReading.timestamp) > Home.stale_data_millis()) {
            UserError.Log.d(TAG, "Slope cannot be flat enough as data is stale");
            return false;
        }
        // TODO check if stale, check previous slope also, check that reading parameters also
        return isSlopeFlatEnough(bgReading);
    }

    public static boolean isSlopeFlatEnough(BgReading bgReading) {
        return isSlopeFlatEnough(bgReading, 1);
    }

    public static boolean isSlopeFlatEnough(BgReading bgReading, double limit) {
        if (bgReading == null) return false;
        // TODO use BestGlucose
        return Math.abs(bgReading.calculated_value_slope * 60000) < limit;
    }
}
