package com.eveningoutpost.dexdrip.ShareModels;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.google.gson.annotations.Expose;

/**
 * Created by stephenblack on 3/19/15.
 */
public class Egv {
    @Expose
    public int Trend;

    @Expose
    public int Value;

    @Expose
    public String ST;

    @Expose
    public String DT;


    public Egv(BgReading bg) {
        this.Value = (int) bg.calculated_value;
        this.DT = toDateString(bg.timestamp);
        this.ST = toDateString(bg.timestamp);
        this.Trend = slopeOrdinal(bg);
    }

    public String toDateString(double timestamp) {
        long shortened = (long) Math.floor((timestamp/1000));
        return "/Date(" + Long.toString(shortened*1000) + ")/";
    }

    public int slopeOrdinal(BgReading bg) {
        double slope_by_minute = bg.calculated_value_slope * 60000;
        int arrow = 0;
        if (slope_by_minute <= (-3.5)) {
            arrow = 7;
        } else if (slope_by_minute <= (-2)) {
            arrow = 6;
        } else if (slope_by_minute <= (-1)) {
            arrow = 5;
        } else if (slope_by_minute <= (1)) {
            arrow = 4;
        } else if (slope_by_minute <= (2)) {
            arrow = 3;
        } else if (slope_by_minute <= (3.5)) {
            arrow = 2;
        } else {
            arrow = 1;
        }
        if(bg.hide_slope) {
            arrow = 9;
        }
        return arrow;
    }
//    {
//
//        "Trend":4,
//            "ST":"\/Date(1426783106000 - 1426754317000)\/",
//            "DT":"\/Date(1426754317000)\/",
//            "Value":97
//    }
}
