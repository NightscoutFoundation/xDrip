package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.models.GlucoseData;
import com.eveningoutpost.dexdrip.models.UserError.Log;

//This class represents a per minute data from the libre.
public class LibreTrendPoint {
    long sensorTime; // The number of minutes from sensor start.
    public long rawSensorValue; // The raw value of the sensor
    public int glucoseLevel;  // The bg values as computed by the oop2.
    int flags; // The flags that were received from the sensor (if any).
    public GlucoseData.DataSource source; // Did we read this data from bluetooth or NFC? This affects the way that flags are interpreted.

    static final String TAG = "LibreTrendPoint";

    public LibreTrendPoint(){}

    // For testing
    public LibreTrendPoint(long sensorTime, long rawSensorValue, int flags, GlucoseData.DataSource source) {
        this.sensorTime = sensorTime;
        this.rawSensorValue = rawSensorValue;
        this.flags = flags;
        this.source = source;
    }

    public String toString(){
        return "{ sensorTime = " + sensorTime + " rawSensorValue = " + rawSensorValue +  " flags = " +   flags +  " source = " +   source +
                " glucoseLevel " + glucoseLevel + "}";
    }

    public long getSensorTime() { return sensorTime;}

    public boolean isError() {
        boolean ret = rawSensorValue == 0;
        if (source == GlucoseData.DataSource.FRAM) {
            // For libre2, it seems that flags ==0 is also a valid value.
            ret |= (flags != 800) && (flags != 0);
        } else if (source == GlucoseData.DataSource.BLE) {
            // Condition already checked (rawSensorValue == 0)
        } else {
            ret = false;
        }
        if(ret) {
            Log.e(TAG, "LibreTrendPoint.isError returning " + ret + " " + toString());
        }
        return ret;
    }
}