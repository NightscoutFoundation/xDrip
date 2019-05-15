package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.google.gson.annotations.Expose;

/**
 * jamorham
 *
 * Model of json data object we receive from Share Servers
 */

public class ShareGlucoseRecord {

    @Expose
    public String DT;

    @Expose
    public String ST;

    @Expose
    public String WT;

    @Expose
    public double Trend;

    @Expose
    public double Value;

    public long timestamp = -1;


    public String slopeDirection() {
        switch ((int) Trend) {
            case 1:
                return "DoubleUp";
            case 2:
                return "SingleUp";
            case 3:
                return "FortyFiveUp";
            case 4:
                return "Flat";
            case 5:
                return "FortyFiveDown";
            case 6:
                return "SingleDown";
            case 7:
                return "DoubleDown";
            default:
                return "";
        }
    }

    public Double slopePerMsFromDirection() {
        final String slope_name = slopeDirection();
        return slope_name == null ? null : BgReading.slopefromName(slope_name);
    }

    public Long getTimestamp() {
        if (timestamp == -1) {
            try {
                timestamp = Long.parseLong(WT.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                timestamp = 0L;
            }
        }
        return timestamp;
    }

    public String toS() {
        return DT + " " + ST + " " + WT + " " + Value + " " + Trend + " " + JoH.dateTimeText(getTimestamp()) + " " + Unitized.unitized_string_static(Value) + " " + slopeDirection();
    }

}
