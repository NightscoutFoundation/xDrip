package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Date;

public class Notification {

    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date dateTime;
    public String type;
    public String faultId;
    public int instanceId;
    public String messageId;
    public String pumpDeliverySuspendState;
    public String pnpId;
    public int relativeOffset;

    public String getMessageId(){
        if(messageId != null)
            return  messageId;
        else if(faultId != null)
            return faultId;
        else
            return null;
    }

}
