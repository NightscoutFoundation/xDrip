package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

// jamorham

// Nightscout Entry data item

import com.eveningoutpost.dexdrip.Models.DateUtil;
import com.google.gson.annotations.Expose;

import java.util.Date;

public class Entry extends BaseMessage {

    @Expose
    public String _id;
    @Expose
    public long date;
    @Expose
    public String dateString;
    @Expose
    public double delta;
    @Expose
    public String device;
    @Expose
    public String direction;
    @Expose
    public double filtered;
    @Expose
    public int noise;
    @Expose
    public int rssi;
    @Expose
    public int sgv;
    @Expose
    public String sysTime;
    @Expose
    public String type;
    @Expose
    public double unfiltered;

    public long getTimeStamp() {
        if (date > 1000000) {
            return date;
        }
        if (sysTime != null) {
            try {
                final Date date = DateUtil.tolerantFromISODateString(sysTime);
                return date.getTime();
            } catch (Exception e) {
                //
            }
        }
        if (dateString != null) {
            try {
                final Date date = DateUtil.tolerantFromISODateString(dateString);
                return date.getTime();
            } catch (Exception e) {
                //
            }
        }
        return -1;
    }
}

