package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.eveningoutpost.dexdrip.models.DateUtil;
import com.google.gson.annotations.JsonAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * CareLink SensorGlucose message with helper methods for processing
 */
public class SensorGlucose {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            dt = DateUtil.toISOString(getDate());
        }
        return dt + " - " + sg;
    }

}