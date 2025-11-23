package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Date;

/**
 * CareLink Alarm data
 */
public class Alarm {

    public String getMessageAlarmCode() {
        return null;
    }

    public String code;
    //@JsonAdapter(CareLinkJsonAdapter.class)
    public String datetime;
    public Date datetimeAsDate;
    public String type;
    public boolean flash;
    public String kind;
    public long version;
    public Integer instanceId;
    public String messageId;
    public Integer sg;
    public Boolean pumpDeliverySuspendState;
    public String referenceGUID;

}