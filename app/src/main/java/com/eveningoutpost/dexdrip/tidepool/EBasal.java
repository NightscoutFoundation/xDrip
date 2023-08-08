package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.annotations.Expose;

// jamorham

public class EBasal extends BaseElement {

    long timestamp; // not exposed

    @Expose
    String deliveryType = "automated";
    @Expose
    long duration;
    @Expose
    double rate = -1;
    @Expose
    String scheduleName = "AAPS";
    @Expose
    long clockDriftOffset = 0;
    @Expose
    long conversionOffset = 0;

    {
        type = "basal";
    }

    EBasal(double rate, long timeStart, long duration, String uuid) {
        this.timestamp = timeStart;
        this.rate = rate;
        this.duration = duration;
        populate(timeStart, uuid);
    }

    boolean isValid() {
        return (rate > -1 && duration > 0);
    }

    String toS() {
        return rate + " Start: " + JoH.dateTimeText(timestamp) + " for: " + JoH.niceTimeScalar(duration);
    }
}
