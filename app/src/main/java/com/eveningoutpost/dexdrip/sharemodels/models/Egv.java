package com.eveningoutpost.dexdrip.sharemodels.models;

import com.eveningoutpost.dexdrip.sharemodels.ShareUploadableBg;
import com.google.gson.annotations.Expose;

/**
 * Created by Emma Black on 3/19/15.
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


    public Egv(ShareUploadableBg bg) {
        this.Value = bg.getMgdlValue();
        this.DT = toDateString(bg.getEpochTimestamp());
        this.ST = toDateString(bg.getEpochTimestamp());
        this.Trend = bg.getSlopeOrdinal();
    }

    private String toDateString(double timestamp) {
        long shortened = (long) Math.floor((timestamp/1000));
        return "/Date(" + Long.toString(shortened*1000) + ")/";
    }

//    {
//
//        "Trend":4,
//            "ST":"\/Date(1426783106000 - 1426754317000)\/",
//            "DT":"\/Date(1426754317000)\/",
//            "Value":97
//    }
}
