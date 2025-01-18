package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

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

    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date timestamp = null;
    public Date date = null;

    public Date getDate(){
        if(this.datetimeAsDate != null)
            return datetimeAsDate;
        else
            return timestamp;
    }

    public String toS() {
        String dt;
        if (getDate() == null) {
            dt = "";
        } else {
            dt = getDate().toString();
        }
        return dt + " " + sg;
    }

}