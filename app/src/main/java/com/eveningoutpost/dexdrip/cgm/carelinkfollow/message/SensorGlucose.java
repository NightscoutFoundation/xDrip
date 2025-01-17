package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink SensorGlucose message with helper methods for processing
 */
public class SensorGlucose {

    public String kind;
    public int version;
    public Integer sg;
    public String sensorState;
    public Object timestamp = -1;  // Changed from long to String

    public String datetime;
    public Date datetimeAsDate;
    public boolean timeChange;
    public int relativeOffset;
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