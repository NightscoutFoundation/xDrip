package com.eveningoutpost.dexdrip.g5model;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

import lombok.NoArgsConstructor;
import lombok.val;

/**
 * Created by jamorham on 02/07/2018.
 */

@NoArgsConstructor
public abstract class BaseGlucoseRxMessage extends BaseMessage {

    private final static String TAG = G5CollectionService.TAG; // meh

    public TransmitterStatus status;
    public int status_raw;
    public int timestamp;
    public int unfiltered;
    public int filtered;
    public int sequence; // : UInt32
    public boolean glucoseIsDisplayOnly; // : Bool
    public int glucose; // : UInt16
    public int state; //: UInt8
    public int trend; // : Int8 127 = invalid


    CalibrationState calibrationState() {
        return CalibrationState.parse(state);
    }

    CalibrationState adjustedCalibrationState() {
        val realState = CalibrationState.parse(state);
        if (realState == CalibrationState.Stopped) {
            if (timestamp * SECOND_IN_MS < MINUTE_IN_MS * 30) {
                UserError.Log.e(TAG, "Reporting Warming up state when marked stopped at timestamp " + JoH.niceTimeScalar(timestamp * SECOND_IN_MS));
                return CalibrationState.WarmingUp;
            }
        }
        return realState;
    }

    public boolean usable() {
        return calibrationState().usableGlucose();
    }

    boolean insufficient() {
        return calibrationState().insufficientCalibration();
    }

    boolean OkToCalibrate() {
        return calibrationState().readyForCalibration();
    }

    public Double getTrend() {
        return trend != 127 ? ((double) trend) / 10d : Double.NaN;
    }

    public Integer getPredictedGlucose() {
        return null; // stub
    }

    public long getRealTimestamp() {
        return JoH.tsl(); // default behavior is received now
    }


}
