
package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import java.text.DecimalFormat;

// class from LibreAlarm

public class GlucoseData implements Comparable<GlucoseData> {
    public enum DataSource {NOT_SET, FRAM, BLE}

    ;

    public long realDate;                 // The time of this reading in ms
    public int sensorTime;                // The counter in minutes from start of sensor.
    public int glucoseLevel = -1;         // The bg value that was calculated by the oop algorithm.
    public int glucoseLevelSmoothed = -1; // The smoothed bg value that was calculated by the oop algorithm.
    public int glucoseLevelRaw = -1;
    public int glucoseLevelRawSmoothed;
    public int flags;
    public int temp;
    public DataSource source;

    public GlucoseData() {
    }

    // jamorham added constructor
    public GlucoseData(int glucoseLevelRaw, long timestamp) {
        this.glucoseLevelRaw = glucoseLevelRaw;
        this.realDate = timestamp;
    }

    public String toString() {
        return "{ sensorTime = " + sensorTime + " glucoseLevel = " + glucoseLevel + " glucoseLevelRaw = " + glucoseLevelRaw +
                " glucoseLevelRawSmoothed = " + glucoseLevelRawSmoothed + " flags = " + flags +
                " source = " + source + "  glucoseLevel " + glucoseLevel + " glucoseLevelSmoothed " + glucoseLevelSmoothed + "}";
    }

    public String glucose(boolean mmol) {
        return glucose(glucoseLevel, mmol);
    }

    public static String glucose(int mgdl, boolean mmol) {
        return mmol ? new DecimalFormat("##.0").format(mgdl / Constants.MMOLL_TO_MGDL) : String.valueOf(mgdl);
    }

    @Override
    public int compareTo(GlucoseData another) {
        return (int) (realDate - another.realDate);
    }
}
