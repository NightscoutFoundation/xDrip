package com.eveningoutpost.dexdrip.tidepool;

import com.google.gson.annotations.Expose;

/**
 * jamorham
 *
 * common element base
 */

public abstract class BaseElement {
    @Expose
    public String deviceTime;
    @Expose
    public String deviceId;
    @Expose
    public String time;
    @Expose
    public int timezoneOffset;
    @Expose
    public String type;


    BaseElement populate(final long timestamp) {
        deviceTime = DateUtil.toFormatNoZone(timestamp);
        time = DateUtil.toFormatAsUTC(timestamp);
        timezoneOffset = DateUtil.getTimeZoneOffsetMinutes(timestamp); // TODO
        deviceId = "42"; // TODO
        return this;
    }

}
