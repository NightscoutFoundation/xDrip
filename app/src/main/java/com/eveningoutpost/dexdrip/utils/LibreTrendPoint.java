package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

//This class represents a per minute data from the libre.
public class LibreTrendPoint {
    long sensorTime; // The number of minutes from sensor start.
    long rawSensorValue; // The raw value of the sensor
    int flags; // The flags that was received from the sensor (if any).
    public GlucoseData.DataSource source; // Did we read this data from
                                          // bluetooth or NFC? This affects the
                                          // way that flags are interputed.

    static final String TAG = "LibreTrendPoint";


    public LibreTrendPoint(){}

    // For testing
    public LibreTrendPoint(long sensorTime, long rawSensorValue, int flags, GlucoseData.DataSource source) {
        this.sensorTime = sensorTime;
        this.rawSensorValue = rawSensorValue;
        this.flags = flags;
        this.source = source;
    }

    public long getSensorTime() { return sensorTime;}

    public boolean isError() {
        boolean ret = false;
        if (source == GlucoseData.DataSource.FARM) {
            ret = flags != 0x800;
        } else if (source == GlucoseData.DataSource.BLE) {
            ret = rawSensorValue == 0;
        } else {
            //Log.e(TAG, "LibreTrendPoint.isError called but source not set " + source);
        }
        Log.e(TAG, "LibreTrendPoint.isError returning " + ret + " flags = " + flags + " source = " + source);
        return ret;
    }
}

