package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink SensorGlucose message with helper methods for processing
 */
public class SensorGlucose {

    public Integer sg;
    public String datetime;
    public Date datetimeAsDate;
    public boolean timeChange;
    public String kind;
    public int version;
    public String sensorState;
    public int relativeOffset;

    public long timestamp = -1;
    public Date date = null;

    public String toS() {
        String dt;
        if (datetime == null) {
            dt = "";
        } else {
            dt = datetime;
        }
        return dt + " " + sg;
    }

}