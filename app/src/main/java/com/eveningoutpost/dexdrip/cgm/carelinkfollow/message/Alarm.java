package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

/**
 * CareLink Alarm data
 */
public class Alarm {

    public String getMessageAlarmCode() {
        return null;
    }

    public String faultId;
    public Object version;  // changed from int to String
    public String GUID;
    public String dateTime;
    public String type;
    public AdditionalInfo additionalInfo;

    public int code;
    public String datetime;
    public Date datetimeAsDate;

    public boolean flash;
    public String kind;

    public Integer instanceId;
    public String messageId;
    public Integer sg;
    public Boolean pumpDeliverySuspendState;
    public String referenceGUID;

}