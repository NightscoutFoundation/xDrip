package com.eveningoutpost.dexdrip.tidepool;

import com.google.gson.annotations.Expose;

import lombok.AllArgsConstructor;

/**
 * jamorham
 *
 * common element base
 */

public abstract class BaseElement {
    @Expose
    public String deviceTime;
    @Expose
    public String time;
    @Expose
    public int timezoneOffset;
    @Expose
    public String type;
    @Expose
    public Origin origin;


    BaseElement populate(final long timestamp, final String uuid) {
        deviceTime = DateUtil.toFormatNoZone(timestamp);
        time = DateUtil.toFormatAsUTC(timestamp);
        timezoneOffset = DateUtil.getTimeZoneOffsetMinutes(timestamp); // TODO
        origin = new Origin(uuid);
        return this;
    }

    @AllArgsConstructor
    public class Origin {
        @Expose
        String id;
    }
}
